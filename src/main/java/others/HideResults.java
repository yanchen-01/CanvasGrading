package others;

import constants.Parameters;
import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.JsonKeywords.ID;
import static constants.Parameters.API;

public class HideResults {

    static final String HIDE = """
            {
            "quiz":
                {
                "hide_results": "always",
                "notify_of_update": false
                }
            }""";
    static final String SHOW = """
            {
            "quiz":
                {
                "allowed_attempts": 2;
                "hide_results": "until_after_last_attempt",
                "notify_of_update": false
                }
            }""";
    static final String ATTEMPTS = """
            {
            "quiz":
                {
                "allowed_attempts": 1;
                "notify_of_update": false
                }
            }""";

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, HideResults::hide);
    }

    static void hide(Scanner in) {
        Utils.printPrompt("Course num (CS154, CS166, or CS175)");
        String courseNum = in.nextLine().toUpperCase();
        courseNum = courseNum.contains("CS") ? courseNum : "CS" + courseNum;
        Parameters.COURSE theClass = Parameters.COURSE.valueOf(courseNum);
        String url = API + "/courses/" + theClass.courseID + "/quizzes";
        Utils.printPrompt("(H)ide or (S)how?");
        String action = in.nextLine();

        String quizData = Utils_HTTP.getData(url);
        JSONArray quizzes = new JSONArray(quizData);
        for (int i = 0; i < quizzes.length(); i++) {
            JSONObject quiz = quizzes.getJSONObject(i);
            int id = quiz.getInt(ID);
            String qURL = url + "/" + id;
            if (action.equalsIgnoreCase("H"))
                Utils_HTTP.putData(qURL, HIDE);
            else if (action.equalsIgnoreCase("S")) {
                Utils_HTTP.putData(qURL, SHOW);
                Utils_HTTP.putData(qURL, ATTEMPTS);
            } else System.out.println("Wrong option.");
        }
    }
}
