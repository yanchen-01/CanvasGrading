package others;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.Parameters.API;

public class GradeDiscussions {
    static String URL;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Utils.askForAuth(scanner);
        Utils.printPrompt("Course ID");
        String courseID = scanner.nextLine();
        URL = API + "/courses/" + courseID + "/assignments";
        String url = Utils_HTTP.getData(URL + "?per_page=100");
        goThroughDiscussions(url);
    }

    static void goThroughDiscussions(String url) {
        JSONArray assignments = new JSONArray(url);
        for (int i = 0; i < assignments.length(); i++) {
            JSONObject assignment = assignments.getJSONObject(i);
            JSONArray types = assignment.getJSONArray("submission_types");
            double score = assignment.getDouble("points_possible");
            if (types.getString(0).equals("discussion_topic")
                    && score < 2.0)
                gradeDiscussion(assignment, score);
        }
    }

    static void gradeDiscussion(JSONObject discussion, double score) {
        String url = URL + "/" + discussion.getInt("id") + "/submissions";
        String aData = Utils_HTTP.getData(url + "?per_page=200");
        JSONArray submissions = new JSONArray(aData);
        for (int i = 0; i < submissions.length(); i++) {
            JSONObject submission = submissions.getJSONObject(i);
            if (submission.getString("workflow_state").equals("unsubmitted")
                    || submission.get("grade") instanceof String)
                continue;

            int uID = submission.getInt("user_id");
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
