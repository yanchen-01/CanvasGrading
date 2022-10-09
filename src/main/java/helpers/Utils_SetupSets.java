package helpers;

import obj.Student;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;

import static constants.JsonKeywords.*;
import static constants.Parameters.API_URL;

/**
 * Setting up question sets (for midterms and finals where one question has different versions).
 */
public class Utils_SetupSets {
    /**
     * Generate and get the json string for question sets.
     * Result string is consistent with the string returned from normal assignments.
     * <ul>
     *     <li>Short answers under the same set has the same name </li>
     *     <li>Auto grading questions will still be as separate questions. </li>
     * </ul>
     *
     * @return the json string for question sets.
     */
    public static String getQuestionSets() {
        int num = getNumOfQuestions();
        JSONArray result = new JSONArray();
        HashSet<Integer> setQs = new HashSet<>();
        outer:
        for (Student student : Utils_Setup.STUDENTS.values()) {
            String url = String.format
                    ("https://sjsu.instructure.com/api/v1/quiz_submissions/%s/questions",
                            student.getSubID());
            JSONObject json = new JSONObject(Utils_HTTP.getData(url));
            JSONArray questions = json.getJSONArray("quiz_submission_questions");
            for (int i = 0; i < questions.length(); i++) {
                JSONObject q = questions.getJSONObject(i);
                int id = q.getInt(ID);
                if (!setQs.add(id)) continue;
                String type = q.getString(TYPE);
                if (type.equals(ESSAY) || type.equals(UPLOAD))
                    updateQuestion(q);
                result.put(q);
                if (result.length() == num)
                    break outer;
            }
        }
        return result.toString();
    }

    private static int getNumOfQuestions() {
        String temp = Utils_HTTP.getData(API_URL + "/statistics");
        JSONObject statistics = new JSONObject(temp);
        JSONArray array = statistics.getJSONArray("quiz_statistics");
        JSONArray questions = array.getJSONObject(0)
                .getJSONArray("question_statistics");
        return questions.length();
    }

    private static void updateQuestion(JSONObject q) {
        int groupID = q.getInt("quiz_group_id");

        String url = API_URL + "/groups/" + groupID;
        JSONObject group = new JSONObject(Utils_HTTP.getData(url));
        String name = group.getString("name");
        double score = group.getDouble("question_points");
        int pickCount = group.getInt("pick_count");
        if (pickCount != 1) {
            String content = q.getString(CONTENT);
            String[] strong = content.split("\u003c/*strong\u003e");
            name = name + "-" + (strong.length < 2 ? q.getInt("id")
                    : strong[1].replaceAll("\\p{Punct}", ""));
        }
        q.put(NAME, name);
        q.put(POINTS, score);
    }
}
