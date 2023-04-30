package others;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static constants.JsonKeywords.USER_ID;

public class UploadRubrics {
    static HashMap<String, String> RUBRICS;
    static HashMap<String, Integer> GROUPS;
    static String API_URL;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, UploadRubrics::upload);
    }

    static void upload(Scanner in) throws Exception {
        // Link format: courseID/gradebook/speed_grader?assignment_id=aID&student_id=sID
        Utils.printPrompt("Test Student's speed grader link");
        API_URL = in.nextLine();
        String[] tokens = API_URL.split("=");
        String assignmentID = Utils.removeNonDigits(tokens[1]);
        String testStudent = tokens[2];
        API_URL = API_URL.replace("/courses", "/api/v1/courses");
        API_URL = API_URL.replaceAll("gradebook.*",
                "assignments/" + assignmentID + "/submissions/");

        setRubrics(testStudent);
        setGroups(testStudent);

        //System.out.println(GROUPS);
        Utils.printPrompt("Filename to read (without .csv)");
        String filename = in.nextLine();
        Utils.readCSV(filename, UploadRubrics::upload);
    }

    static void setRubrics(String testStudent) {
        RUBRICS = new HashMap<>(6);
        String url = API_URL + testStudent + "?include[]=rubric_assessment";
        String rubricsStr = Utils_HTTP.getData(url);
        JSONObject json = new JSONObject(rubricsStr);
        JSONObject rubrics = json.getJSONObject("rubric_assessment");
        for (String rubricID : rubrics.keySet()) {
            JSONObject rubric = rubrics.getJSONObject(rubricID);
            String rubricTitle = rubric.getString("comments");
            RUBRICS.put(rubricTitle, rubricID);
        }
    }

    static void setGroups(String testStudent) {
        GROUPS = new HashMap<>(20);
        String url = API_URL + "?grouped=true&include=group&per_page=100";
        String subStr = Utils_HTTP.getData(url);
        JSONArray array = new JSONArray(subStr);
        for (int i = 0; i < array.length(); i++) {
            JSONObject group = array.getJSONObject(i);
            String submit = group.getString("workflow_state");
            if (submit.equals("unsubmitted")) continue;
            int id = group.getInt(USER_ID);
            if (id == Integer.parseInt(testStudent)) continue;

            JSONObject g = group.getJSONObject("group");
            String name = g.getString("name");
            name = "G" + Utils.removeNonDigits(name);
            GROUPS.put(name, id);
        }
    }

    static void upload(List<String[]> content) {
        String[] header = content.remove(0);

        for (String[] row : content) {
            int groupID = GROUPS.get(row[0]);
            String url = API_URL + groupID;
            StringBuilder rubrics = new StringBuilder();
            for (int i = 1; i < row.length - 1; i += 2) {
                double point = Double.parseDouble(row[i]);
                String rubricID = RUBRICS.get(header[i]);
                rubrics.append(String.format("""
                        "%s": {
                            "comments": "%s",
                            "points": %.1f
                        }""", rubricID, row[i + 1], point));
                if (i != row.length - 2)
                    rubrics.append(",\n");
            }
            String data = String.format("""
                    {
                        "rubric_assessment": {
                            %s
                        }
                    }
                    """, rubrics);

            //System.out.println(url + "\n" + data + "\n***********");
            Utils_HTTP.putData(url, data);
        }
    }
}
