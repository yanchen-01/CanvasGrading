package grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.Utils;
import helpers.Utils_HTML;
import helpers.Utils_HTTP;
import obj.Assignment;
import obj.MyGitHub;
import obj.Submission;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

import static constants.JsonKeywords.GRADED;
import static constants.Parameters.*;

public class PostPoints {
    static final String TIME_PLACEHOLDER = "[[UPDATE TIME]]";
    static HashMap<Integer, Double> STUDENTS;
    static COURSE CLASS;
    static int EC_ID;
    static String API_URL, UPDATE_TIME, NEW_EC_DES;
    static HashMap<String, Assignment> CALCULATED;
    static String UPDATED;
    static boolean DISCUSSION;

    public static void main(String[] args) {
        STUDENTS = new HashMap<>();
        CALCULATED = new HashMap<>();
        Utils.runFunctionality(PostPoints::extraCredit);
    }

    static void extraCredit(Scanner in) throws IOException {
        getParams(in);
        Utils.printPrompt("Including discussions? (Y/N)");
        DISCUSSION = in.nextLine().equals("Y");
        goOverAssignments();
        post(in);
    }

    static void getParams(Scanner in) {
        Utils.printPrompt("Course num (CS154 or CS46A)");
        String courseNum = in.nextLine().toUpperCase();
        courseNum = courseNum.contains("CS") ? courseNum : "CS" + courseNum;
        CLASS = COURSE.valueOf(courseNum);
        API_URL = API + "/courses/" + CLASS.courseID + "/assignments";
    }

    static void goOverAssignments() throws JsonProcessingException {
        Assignment[] assignments = Utils.getObjFromURL(API_URL + "?per_page=100", Assignment[].class);

        for (Assignment assignment : assignments) {
            String name = assignment.getName();
            int id = assignment.getId();
            // For extra credits, record the id and continue to next assignment
            if (name.equals("Extra Credits")) {
                EC_ID = id;
                continue;
            }
            // Skip others things that need to be skipped
            if (name.contains("Final") || name.contains("Term")
                    || !assignment.isHasGraded() || assignment.getUngraded() != 0)
                continue;
            if (!DISCUSSION && !(name.matches(MIDTERM) || name.matches(ASSIGNMENTS)))
                continue;
            if (name.contains("Group") || name.contains("Mini")
                    || name.contains("Review"))
                continue;

            calculatePoints(assignment);
            CALCULATED.put(assignment.getShortName(), assignment);
        }

        Utils.printDoneProcess("Calculation done for all assignments!");
    }

    static void calculatePoints(Assignment assignment) throws JsonProcessingException {
        double total = assignment.getTotal();
        String name = assignment.getName();
        Utils.printProgress("calculating points for " + name);

        String apiUrl = assignment.getApiUrl() + "/submissions?per_page=200";
        Submission[] submissions = Utils.getObjFromURL(apiUrl, Submission[].class);

        for (Submission submission : submissions) {
            int studentID = submission.getUserID();
            String status = submission.getStatus();

            if (studentID == CLASS.testStudent
                    || !status.equals(GRADED)) continue;
            double score = submission.getScore();
            double points;
            if (name.matches(MIDTERM))
                points = score / total >= 0.5 ? 3 : 0;
            else if (total > 10)
                points = score / total * CLASS.each;
            else points = score;
            STUDENTS.computeIfPresent(studentID, (k, v) -> v + points);
            STUDENTS.putIfAbsent(studentID, points);
        }
    }

    static void post(Scanner in) throws IOException {
        if (CALCULATED.isEmpty()) {
            System.out.println("None assignment is being calculated. Please contact Yan for the bug.");
            return;
        }
        setNewPosted();
        Utils.printPrompt("whether post to Extra Credits? (Y/N)");
        String confirm = in.nextLine();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Posting cancelled. " +
                    "Contact Yan if it's because wrong assignments were calculated. ");
            return;
        }

        postPoints();
        UPDATE_TIME = Utils.formatDate(new Date());
        updateDescription();
        updateHomepage();
        if (CLASS.calculator)
            updateCalculator(in);
        Utils.printDoneProcess("Posting done. Double-check on Canvas");
    }

    private static void postPoints() {
        Utils.printProgress("posting to Extra Credits");
        String assignment = API_URL + "/" + EC_ID + "/submissions/";
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse

        STUDENTS.forEach((student, points) -> {
            String url = assignment + student;
            ObjectNode result = mapper.createObjectNode();
            result.putObject("submission")
                    .put("posted_grade", String.format("%.2f", points));
            Utils_HTTP.putData(url, result.toString());
        });
    }

    private static void setNewPosted() throws JsonProcessingException {
        String url = API_URL + "/" + EC_ID;
        Document doc = Utils.getHtmlDoc(url, false);

        String spanColor = "";
        StringBuilder newPosted = new StringBuilder();
        Element template = new Element("a");
        template.attr("data-api-returntype", "Assignment");

        Elements spans = doc.select("td span");
        for (int i = 0; i < spans.size(); i += 2) {
            Element current = spans.get(i);
            String content = current.ownText();
            String color = current.attr("style");
            Assignment assignment = CALCULATED.get(content);
            if (!color.equals("color: #95a5a6;")) {
                spanColor = color;
            } else if (assignment != null) {
                newPosted.append(" & ").append(assignment.getAbbr(assignment.getShortName()));

                template.attr("title", assignment.getName());
                template.attr("href", assignment.getHtmlUrl());
                template.attr("data-api-endpoint", assignment.getApiUrl());
                current.wrap(template.outerHtml());

                if (spanColor.isEmpty()) {
                    current.unwrap();
                    spans.get(i + 1).unwrap();
                } else {
                    current.attr("style", spanColor);
                    spans.get(i + 1).attr("style", spanColor);
                }
            }
        }
        UPDATED = DISCUSSION ? "All"
                : newPosted.toString().replaceFirst(" & ", "");
        Element upTime = doc.select("p:contains(Updated) > strong").first();
        if (upTime != null)
            upTime.html(TIME_PLACEHOLDER);
        NEW_EC_DES = doc.body().html();
        Utils.printDoneProcess("Newly added: " + UPDATED);
    }

    static void updateDescription() {
        String url = API_URL + "/" + EC_ID;
        String upTime = String.format("Updated on %s (%s)", UPDATE_TIME, UPDATED);
        NEW_EC_DES = NEW_EC_DES.contains(TIME_PLACEHOLDER) ?
                NEW_EC_DES.replace(TIME_PLACEHOLDER, upTime)
                : String.format("<p><strong>%s</p></strong>\n%s", upTime, NEW_EC_DES);
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        ObjectNode result = mapper.createObjectNode();
        result.putObject("assignment")
                .put("description", NEW_EC_DES);
//        Utils_HTML.writeToHTMLFile("des", NEW_EC_DES);
        Utils_HTTP.putData(url, result.toString());
        Utils.printDoneProcess("Extra credits assignment description updated");
    }

    static void updateHomepage() throws JsonProcessingException {
        String url = API_URL.replace("assignments", "front_page");
        Document doc = Utils.getHtmlDoc(url, true);

        String update;
        Element e = doc.select("li:has(a:contains(Extra Credits))").first();
        if (e != null) {
            Element extra = e.select("a:contains(Extra Credits)").first();
            update = String.format("<li>%s: %s Updated (%s)",
                    UPDATE_TIME, Objects.requireNonNull(extra).outerHtml(), UPDATED);
            e.remove();
        } else {
            String extraUrl = Utils.getOriginalUrl(API_URL + "/" + EC_ID);
            update = String.format("<li>%s: <a title=\"Extra Credits\" href=\"%s\">Extra Credits</a> Updated (%s)</li>",
                    UPDATE_TIME, extraUrl, UPDATED);
        }

        Elements anns = doc.select("h4:contains(Latest Updates) + ul > li");
        Objects.requireNonNull(anns.first()).before(update);

        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        ObjectNode result = mapper.createObjectNode();
        result.putObject("wiki_page")
                .put("body", doc.body().html());
        Utils_HTTP.putData(url, result.toString());
//        Utils_HTML.writeToHTMLFile("home", doc.html());
        Utils.printDoneProcess("Home page updated");
    }

    static void updateCalculator(Scanner in) throws IOException {
        Utils.printProgress("Updating 154 Calculator");
        String username = Utils.askForParam(in, USERNAME);
        String password = Utils.askForParam(in, PASSWORD);
        MyGitHub git = new MyGitHub(username, password);
        git.setGit(CAL_FOLDER);
        if (git.getGit() == null)
            git.cloneRepo(CAL_URL, CAL_FOLDER);
        File file = new File(CAL_FOLDER + "/index.html");
        Document doc = Jsoup.parse(file);
        Element time = doc.getElementById("time");
        Objects.requireNonNull(time).html(UPDATE_TIME.replaceAll(", at.*", ""));

        Elements trs = doc.select("tr.unpo");
        for (Element tr : trs) {
            String id = tr.id();
            Assignment a = CALCULATED.get(id);
            if (a != null) {
                String total = String.format("%.1f", a.getTotal());
                total = total.replace(".0", "");
                tr.removeClass("unpo");
                Element t = tr.getElementsByClass("total").first();
                Objects.requireNonNull(t).html(total);
            }
        }
        String filename = file.getAbsolutePath();
        Utils_HTML.writeToHTMLFile(filename, doc.html());

        git.commitAndPush(UPDATED);
    }

}