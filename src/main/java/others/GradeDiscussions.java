package others;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.Parameters.API;

// TODO: make code looks nicer.
public class GradeDiscussions {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Utils.askForAuth(scanner);
        Utils.printPrompt("Course ID");
        String courseID = scanner.nextLine();
        String url = API + "/courses/" + courseID + "/assignments";
        String a = Utils_HTTP.getData(url + "?per_page=100");
        JSONArray assignments = new JSONArray(a);
        for (int i = 0; i < assignments.length(); i++) {
            JSONObject assignment = assignments.getJSONObject(i);
            JSONArray types = assignment.getJSONArray("submission_types");
            double score = assignment.getDouble("points_possible");
            if (types.getString(0).equals("discussion_topic")
                    && score < 2.0) {
                String aURL = url + "/" + assignment.getInt("id") + "/submissions";
                String aData = Utils_HTTP.getData(aURL + "?per_page=200");
                JSONArray submissions = new JSONArray(aData);
                for (int j = 0; j < submissions.length(); j++) {
                    JSONObject submission = submissions.getJSONObject(j);
                    if (submission.getString("workflow_state").equals("unsubmitted"))
                        continue;
                    int uID = submission.getInt("user_id");
                    String sURL = aURL + "/" + uID;
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
    }
}
