package grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.Utils;
import helpers.Utils_HTTP;
import obj.Assignment;
import obj.Rubric;
import obj.Section;
import obj.Submission;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GradeByRubrics {

    static String ASSIGNMENT_URL;
    static boolean GROUPED;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, GradeByRubrics::run);
    }

    static void run(Scanner in) throws Exception {
        Utils.printPrompt("assignment url (do not end with /)");
        ASSIGNMENT_URL = Utils.getApiUrl(in.nextLine());
        Utils.printPrompt("""
                one option (1 or 2):
                1. Download template
                2. Upload result""");

        int option = in.nextInt();
        in.nextLine(); // make sure cursor move to next line.

        switch (option) {
            case 1 -> downloadTemplate(in);
            case 2 -> uploadResult(in);
            default -> System.out.println("Wrong option, program terminated. ");
        }
    }

    static void downloadTemplate(Scanner in) throws Exception {
        Utils.printPrompt("your section number (enter a for all)");
        String sec = in.nextLine();
        Utils.printProgress("generating template");
        String[] headers = getHeaders();
        Submission[] submissions = getSubmissionsJSON(sec);
        List<String[]> contents = getContent(headers, submissions);
        Utils.printDoneProcess("Templated generated");
        Utils.printPrompt("filename to save (without .csv)");
        String filename = in.nextLine();
        Utils.writeCSV(filename, contents);
        Desktop.getDesktop().open(new File(filename + ".csv"));
        Utils.printDoneProcess("Templated opened. After grading, run this again to upload.");
    }

    static String[] getHeaders() throws JsonProcessingException {
        Assignment assignment = Utils.getObjFromURL(ASSIGNMENT_URL, Assignment.class);
        GROUPED = assignment.isGrouped();
        ArrayList<Rubric> rubrics = assignment.getRubrics();

        String[] headers = new String[rubrics.size() * 2 + 2];
        headers[0] = "StudentName";
        headers[1] = "UserID";
        for (int i = 0; i < rubrics.size(); i++) {
            Rubric rubric = rubrics.get(i);
            String id = rubric.getId();
            String name = rubric.getDescription();
            double points = rubric.getPoints();
            String r = String.format("%s-%s", id, name);
            headers[2 * i + 2] = r;
            headers[2 * i + 3] = String.valueOf(points);
        }
        return headers;
    }

    static Submission[] getSubmissionsJSON(String sec) throws JsonProcessingException {
        String params = GROUPED ?
                "?grouped=true&include=group" : "?include=user";
        String url = String.format("%s/submissions%s%s",
                ASSIGNMENT_URL, params, "&per_page=100");
        if (sec.matches("\\d+")) {
            int section = getSectionID(sec);
            url = url.replaceAll("courses/\\d+", "sections/" + section);
        }
        return Utils.getObjFromURL(url, Submission[].class);
    }

    static List<String[]> getContent(String[] headers, Submission[] submissions) {
        List<String[]> result = new ArrayList<>();
        result.add(headers);
        for (Submission sub : submissions) {
            if (sub.getStatus().equals("unsubmitted"))
                continue;
            String name = sub.getGroup() == null ?
                    sub.getUserName() : sub.getGroupName();
            int id = sub.getUserID();
            result.add(new String[]{name, String.valueOf(id)});
        }
        return result;
    }

    static int getSectionID(String num) throws JsonProcessingException {
        String courseUrl = ASSIGNMENT_URL.replaceAll("assignments.*", "sections");
        Section[] sections = Utils.getObjFromURL(courseUrl, Section[].class);

        for (Section section : sections) {
            if (section.getNum().equals(num))
                return section.getId();
        }
        return -1;
    }

    static void uploadResult(Scanner in) throws Exception {
        Utils.printPrompt("Filename to read (without .csv)");
        String filename = in.nextLine();
        Utils.readCSV(filename, GradeByRubrics::upload);
        Utils.printDoneProcess("Results uploaded, double-check on Canvas");
    }

    static void upload(List<String[]> content) {
        String[] header = content.remove(0);
        ObjectMapper mapper = new ObjectMapper();
        for (String[] row : content) {
            String studentID = row[1];
            String url = ASSIGNMENT_URL + "/submissions/" + studentID;
            ObjectNode scores = mapper.createObjectNode();
            for (int i = 2; i < row.length - 1; i += 2) {
                String pts = row[i + 1];
                if (pts.isEmpty()) continue;

                String rubricID = header[i].replaceAll("-.*", "");
                ObjectNode score = scores.putObject(rubricID)
                        .put("points", Double.parseDouble(pts));
                if (!row[i].isEmpty())
                    score.put("comment", row[i]);
            }
            if (scores.isEmpty()) continue;
            ObjectNode result = mapper.createObjectNode();
            result.putPOJO("rubric_assessment", scores);
            Utils_HTTP.putData(url, result.toString());
        }
    }
}