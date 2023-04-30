package others;

import helpers.Utils;
import helpers.Utils_HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.JsonKeywords.ID;
import static constants.Parameters.API;

/**
 * Change the available date for selected assignments.
 * Currently, for CS175 only. May update later.
 */
public class ChangeDate {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, ChangeDate::change);
    }

    static void change(Scanner in) {
        Utils.printPrompt("Course ID");
        String courseID = in.nextLine();
        String url = API + "/courses/" + courseID + "/assignments";

        String assignmentData = Utils_HTTP.getData(url + "?page=1&per_page=100");
        JSONArray assignments = new JSONArray(assignmentData);
        for (int i = 0; i < assignments.length(); i++) {
            JSONObject assignment = assignments.getJSONObject(i);
            String name = assignment.getString("name");

            if (!name.matches("(Mini|Exercise [1-6]).*-.*")) continue;

            int id = assignment.getInt(ID);
            String qURL = url + "/" + id;
            Utils_HTTP.putData(qURL, DATE);
        }
    }

    static final String DATE = """
                    {
                    "assignment":
                        {
                        "lock_at": "2022-12-09T23:59:00-08:00"
                        }
                    }""";

}
