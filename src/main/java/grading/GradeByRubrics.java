package grading;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static constants.JsonKeywords.ID;

public class GradeByRubrics {

    static String ASSIGNMENT_URL;
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, GradeByRubrics::run);
    }

    static void run(Scanner in) throws Exception {
        Utils.printPrompt("assignment url (do not end with /)");
        ASSIGNMENT_URL = in.nextLine().replace("courses", "api/v1/courses");
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
        Utils.printPrompt("your section number (enter a for all): ");
        String sec = in.nextLine();
        String[] headers = getHeaders();
        JSONArray students = getStudentsJSON(sec);
        List<String[]> contents = getContent(headers, students);
        Utils.printDoneProcess("Templated generated");
        Utils.printPrompt("filename to save (without .csv)");
        String filename = in.nextLine();
        Utils.writeCSV(filename, contents);
        Desktop.getDesktop().open(new File(filename + ".csv"));
        Utils.printDoneProcess("Templated opened. After grading, run this again to upload.");
    }

    static String[] getHeaders() {
        String data = Utils_HTTP.getData(ASSIGNMENT_URL);
        JSONObject o = new JSONObject(data);
        JSONArray rubrics = o.getJSONArray("rubric");
        String[] headers = new String[rubrics.length() * 2 + 2];
        headers[0] = "StudentName";
        headers[1] = "UserID";
        for (int i = 0; i < rubrics.length(); i++) {
            JSONObject rubric = rubrics.getJSONObject(i);
            String id = rubric.getString(ID);
            String name = rubric.getString("description");
            double points = rubric.getDouble("points");
            String r = String.format("%s-%s", id, name);
            headers[2 * i + 2] = r;
            headers[2 * i + 3] = String.valueOf(points);
        }
        return headers;
    }

    static JSONArray getStudentsJSON(String sec) {
        String courseUrl = ASSIGNMENT_URL.replaceAll("assignments.*", "");
        if (sec.matches("\\d+")) {
            int section = Integer.parseInt(sec);
            section = getSectionID(courseUrl, section);
            String url = String.format("%ssections/%d?include=students",
                    courseUrl, section);
            String data = Utils_HTTP.getData(url);
            JSONObject o = new JSONObject(data);
            return o.getJSONArray("students");
        } else {
            String data = Utils_HTTP.getData(courseUrl + "students?per_page=200");
            return new JSONArray(data);
        }
    }

    static List<String[]> getContent(String[] headers, JSONArray students) {
        List<String[]> result = new ArrayList<>();
        result.add(headers);
        for (int i = 0; i < students.length(); i++) {
            JSONObject student = students.getJSONObject(i);
            String name = student.getString("name");
            int id = student.getInt(ID);
            result.add(new String[]{name, String.valueOf(id)});
        }
        return result;
    }

    static int getSectionID(String courseUrl, int section) {
        courseUrl = courseUrl + "sections";
        String data = Utils_HTTP.getData(courseUrl);
        JSONArray sections = new JSONArray(data);
        for (int i = 0; i < sections.length(); i++) {
            JSONObject sec = sections.getJSONObject(i);
            String name = sec.getString("name");
            name = name.replaceAll(".*Section ", "");
            if (section == Integer.parseInt(name))
                return sec.getInt(ID);
        }
        return -1;
    }

    static void uploadResult(Scanner in) throws Exception {
        Utils.printPrompt("Filename to read (without .csv)");
        String filename = in.nextLine();
        Utils.readCSV(filename, GradeByRubrics::upload);
    }

    static void upload(List<String[]> content) {
        String[] header = content.remove(0);

        for (String[] row : content) {
            String studentID = row[1];
            String url = ASSIGNMENT_URL + "/submissions/" + studentID;
            JSONObject rubrics = new JSONObject();
            for (int i = 2; i < row.length - 1; i += 2) {
                String pts = row[i + 1];
                if (pts.isEmpty()) continue;
                double point = Double.parseDouble(pts);

                JSONObject rubric = new JSONObject();
                rubric.put("points", point);
                if (!row[i].isEmpty())
                    rubric.put("comments", row[i]);

                String rubricID = header[i].replaceAll("-.*", "");
                rubrics.put(rubricID, rubric);
            }

            JSONObject result = new JSONObject();
            result.put("rubric_assessment", rubrics);
            Utils_HTTP.putData(url, result.toString());
        }
    }
}