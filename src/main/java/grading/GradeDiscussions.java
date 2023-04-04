package grading;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.JsonKeywords.*;

public class GradeDiscussions {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, GradeDiscussions::grade);
    }

    static void grade(Scanner in) {
        Utils.printPrompt("discussion url or course url for all discussions");
        String url = in.nextLine();
        url = Utils.getApiUrl(url);
        if (url.contains("discussion_topics")) {
            gradeOne(url);
        } else {
            gradeAll(url + "/assignments");
        }
        Utils.printDoneProcess("Graded, double-check on Canvas");
    }

    static void gradeOne(String url) {
        Utils.printProgress("Getting discussion info");
        JSONObject discussion = Utils_HTTP.getJSON(url);
        JSONObject assignment = discussion.getJSONObject("assignment");
        gradeDiscussion(assignment);
    }

    static void gradeAll(String url) {
        Utils.printProgress("Getting all discussions");
        JSONArray assignments = Utils_HTTP.getJSONArray(url + "?per_page=100");
        for (int i = 0; i < assignments.length(); i++) {
            JSONObject assignment = assignments.getJSONObject(i);
            JSONArray types = assignment.getJSONArray("submission_types");
            if (types.getString(0).equals("discussion_topic"))
                gradeDiscussion(assignment);
        }
    }

    static void gradeDiscussion(JSONObject assignment) {
        double score = assignment.getDouble(POINTS);
        if (score > 2) return;
        String name = assignment.getString(NAME);
        Utils.printProgress("Grading " + name);

        String url = assignment.getString("html_url");
        url = Utils.getApiUrl(url) + "/submissions";

        String aData = Utils_HTTP.getData(url + "?per_page=200");
        JSONArray submissions = new JSONArray(aData);
        for (int i = 0; i < submissions.length(); i++) {
            JSONObject submission = submissions.getJSONObject(i);
            if (submission.getString("workflow_state").equals("unsubmitted")
                    || submission.get("grade") instanceof String)
                continue;
            int uID = submission.getInt(USER_ID);
            String sURL = url + "/" + uID;

            String data = String.format("""
                    {
                        "submission": {
                            "posted_grade":%.1f
                        }
                    }
                    """, score);
            Utils_HTTP.putData(sURL, data);
        }
    }
}
