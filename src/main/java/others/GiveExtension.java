package others;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.JsonKeywords.*;
import static constants.Parameters.API;

/**
 * Give extension (another attempt) to students that got scores < certain threshold
 */
public class GiveExtension {

    static double penalty;
    static int TEST_STUDENT = 4562766;

    public static void main(String[] args) {
        Utils.runFunctionality(GiveExtension::give);
    }

    static void give(Scanner in) {
        Utils.printPrompt("Course ID");
        String courseID = in.nextLine();
        Utils.printPrompt("Assignments (in regex)");
        String regex = in.nextLine();
        Utils.printPrompt("Late penalty (in double)");
        penalty = in.nextDouble();
        String url = API + "/courses/" + courseID + "/quizzes";
        String quizData = Utils_HTTP.getData(url + "?page=1&per_page=100");

        JSONArray quizzes = new JSONArray(quizData);
        for (int i = 0; i < quizzes.length(); i++) {
            JSONObject quiz = quizzes.getJSONObject(i);
            String title = quiz.getString("title");
            if (!title.matches(regex)) continue;

            int id = quiz.getInt(ID);
            double total = quiz.getDouble(POINTS);
            String qURL = url + "/" + id + "/submissions?page=1&per_page=100";
            System.out.println(title);
            handleQuiz(qURL, total);
        }
    }

    static void handleQuiz(String url, double total) {
        String data = Utils_HTTP.getData(url);
        JSONObject json = new JSONObject(data);
        JSONArray submissions = json.getJSONArray("quiz_submissions");
        JSONArray extensions = new JSONArray();
        for (int i = 0; i < submissions.length(); i++) {
            JSONObject current = submissions.getJSONObject(i);
            double score;
            try {
                score = current.getDouble("kept_score");
            } catch (JSONException e) {
                continue;
            }
            int student_id = current.getInt(USER_ID);
            if (score >= total * (1 - penalty) || student_id == TEST_STUDENT)
                continue;

            JSONObject extension = new JSONObject();
            extension.put(USER_ID, student_id);
            extension.put("extra_attempts", 1);
            extensions.put(extension);
            System.out.println(student_id + " " + score);
        }
        JSONObject result = new JSONObject();
        result.put("quiz_extensions", extensions);

        url = url.replace("submissions?page=1&per_page=100", "extensions");
        System.out.println(url);
        System.out.println(result);
        Utils_HTTP.httpRequest("POST", url, result.toString());
    }
}
