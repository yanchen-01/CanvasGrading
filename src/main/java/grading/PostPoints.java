package grading;

import helpers.Utils;
import helpers.Utils_HTML;
import helpers.Utils_HTTP;
import obj.Assignment;
import obj.MyGitHub;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

import static constants.JsonKeywords.*;
import static constants.Parameters.*;

public class PostPoints {
    static final String TIME_PLACEHOLDER = "[[UPDATE TIME]]";
    static HashMap<Integer, Double> STUDENTS;
    static COURSE CLASS;
    static int EC_ID;
    static String API_URL, UPDATE_TIME, NEW_EC_DES;
    static HashMap<String, Assignment> CALCULATED;
    static String UPDATED;

    public static void main(String[] args) {
        STUDENTS = new HashMap<>();
        CALCULATED = new HashMap<>();
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, PostPoints::extraCredit);
    }

    static void extraCredit(Scanner in) throws IOException {
        getParams(in);
        goOverAssignments();
        post(in);
        if (CLASS.calculator)
            updateCalculator(in);
    }

    static void getParams(Scanner in) {
        Utils.printPrompt("Course num (CS154, CS166, or CS175)");
        String courseNum = in.nextLine().toUpperCase();
        courseNum = courseNum.contains("CS") ? courseNum : "CS" + courseNum;
        CLASS = COURSE.valueOf(courseNum);
        API_URL = API + "/courses/" + CLASS.courseID + "/assignments";
    }

    static void goOverAssignments() {
        JSONArray assignments = Utils_HTTP.getJSONArray(API_URL + "?per_page=100");

        for (int i = 0; i < assignments.length(); i++) {
            JSONObject current = assignments.getJSONObject(i);
            String name = current.getString("name");
            int id = current.getInt(ID);
            // For extra credits, record the id and continue to next assignment
            if (name.equals("Extra Credits")) {
                EC_ID = id;
                continue;
            }

            // Skip others things that need to be skipped
            if (!(name.matches(MIDTERM) || name.matches(ASSIGNMENTS))
                    || !current.getBoolean(HAS_GRADED) || current.getInt(NEED_GRADE) != 0)
                continue;

            double total = current.getDouble(POINTS);
            String url = current.getString(URL);
            Assignment assignment = new Assignment(name, total, url);
            calculatePoints(assignment);
            CALCULATED.put(assignment.getShortName(), assignment);
        }
        Utils.printDoneProcess("Calculation done!");
    }

    static void calculatePoints(Assignment assignment) {
        double total = assignment.getTotal();
        String name = assignment.getName();
        Utils.printProgress("calculating points for " + name);

        String apiUrl = assignment.getApiUrl() + "/submissions?per_page=200";
        JSONArray submissions = Utils_HTTP.getJSONArray(apiUrl);

        for (int i = 0; i < submissions.length(); i++) {
            JSONObject current = submissions.getJSONObject(i);
            int student_id = current.getInt(USER_ID);
            String status = current.getString(STATUS);

            if (student_id == CLASS.testStudent
                    || !status.equals(GRADED)) continue;

            double score = current.getDouble("score");
            double points;
            if (name.matches(MIDTERM))
                points = score / total >= 0.5 ? 3 : 0;
            else if (total > 5)
                points = score / total * CLASS.each;
            else points = score;
            STUDENTS.computeIfPresent(student_id, (k, v) -> v + points);
            STUDENTS.putIfAbsent(student_id, points);
        }
    }

    static void post(Scanner in) {
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM. d', at' HH:mm");
        UPDATE_TIME = dateFormat.format(new Date());
        // May doesn't need . after...
        UPDATE_TIME = UPDATE_TIME.replace("May.", "May");
        updateDescription();
        updateHomepage();
        Utils.printDoneProcess("Posting done. Double-check on Canvas");
    }

    private static void postPoints() {
        Utils.printProgress("posting to Extra Credits");
        String assignment = API_URL + "/" + EC_ID + "/submissions/";

        STUDENTS.forEach((student, points) -> {
            String url = assignment + student;

            JSONObject submission = new JSONObject();
            JSONObject score = new JSONObject();
            score.put("posted_grade", String.format("%.2f", points));
            submission.put("submission", score);

            Utils_HTTP.putData(url, submission.toString());
        });
    }

    private static void setNewPosted() {
        String url = API_URL + "/" + EC_ID;
        JSONObject data = Utils_HTTP.getJSON(url);
        String body = data.getString("description");
        Document doc = Jsoup.parse(body);

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
                newPosted.append(" & ").append(assignment.getAbbr());

                template.attr("title", assignment.getName());
                template.attr("href", assignment.getUrl());
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
        UPDATED = newPosted.toString().replaceFirst(" & ", "");
        Element upTime = doc.select("p:contains(Updated) > strong").first();
        Objects.requireNonNull(upTime).html(TIME_PLACEHOLDER);
        NEW_EC_DES = doc.body().html();
        Utils.printDoneProcess("Newly added: " + UPDATED);
    }

    static void updateDescription() {
        String url = API_URL + "/" + EC_ID;
        String upTime = String.format("Updated on %s (%s)", UPDATE_TIME, UPDATED);
        NEW_EC_DES = NEW_EC_DES.replace(TIME_PLACEHOLDER, upTime);
        JSONObject updated = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("description", NEW_EC_DES);
        updated.put("assignment", data);

        Utils_HTTP.putData(url, updated.toString());
        Utils.printDoneProcess("Extra credits assignment description updated");
    }

    static void updateHomepage() {
        String url = API_URL.replace("assignments", "front_page");
        JSONObject front = Utils_HTTP.getJSON(url);
        String body = front.getString("body");
        Document doc = Jsoup.parse(body);

        Element e = doc.select("li:has(a:contains(Extra Credits))").first();
        if (e != null) {
            Element extra = e.select("a:contains(Extra Credits)").first();
            Elements anns = doc.select("h4:contains(Latest Updates) + ul > li");
            String update = String.format("<li>%s: %s Updated (%s)",
                    UPDATE_TIME, Objects.requireNonNull(extra).outerHtml(), UPDATED);
            Objects.requireNonNull(anns.first()).before(update);
            e.remove();
        }

        JSONObject updated = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("body", doc.body().html());
        updated.put("wiki_page", data);
        Utils_HTTP.putData(url, updated.toString());
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