package grading;

import helpers.Utils;
import helpers.Utils_HTTP;
import obj.Assignment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Scanner;

import static constants.JsonKeywords.*;
import static constants.Parameters.*;

public class PostPoints {
    static HashMap<Integer, Double> students;
    static COURSE course;
    static int extraCreditAssignment;
    static String apiURL;
    static int calculated;

    public static void main(String[] args) {
        students = new HashMap<>();
        try (Scanner in = new Scanner(System.in)) {
            getParams(in);
            goOverAssignments();
            postPoints(in);
        } catch (Exception e) {
            Utils.printFatalError(e);
        }
    }

    static void getParams(Scanner in) {
        Utils.askForAuth(in);
        Utils.printPrompt("Course num (CS154, CS166, or CS175)");
        String courseNum = in.nextLine().toUpperCase();
        courseNum = courseNum.contains("CS") ? courseNum : "CS" + courseNum;
        course = COURSE.valueOf(courseNum);
        apiURL = API + "/courses/" + course.courseID + "/assignments";
    }

    static void goOverAssignments() {
        calculated = 0;
        String quizData = Utils_HTTP.getData(apiURL + "?page=1&per_page=100");
        JSONArray assignments = new JSONArray(quizData);

        for (int i = 0; i < assignments.length(); i++) {
            JSONObject current = assignments.getJSONObject(i);
            String name = current.getString("name");
            int id = current.getInt(ID);
            // For extra credits, record the id and continue to next assignment
            if (name.equals("Extra Credits")) {
                extraCreditAssignment = id;
                continue;
            }

            // Skip others things that need to be skipped
            if (!(name.matches(MIDTERM) || name.matches(ASSIGNMENTS))
                    || !current.getBoolean(HAS_SUBS) || current.getInt(NEED_GRADE) != 0)
                continue;

            double total = current.getDouble(POINTS);
            String url = apiURL + "/" + id + "/submissions?page=1&per_page=150";
            Assignment assignment = new Assignment(name, total, url);
            calculatePoints(assignment);
            calculated++;
        }
    }

    static void calculatePoints(Assignment assignment) {
        double total = assignment.getTotal();
        String name = assignment.getName();
        Utils.printProgress("calculating points for" + name);

        String data = Utils_HTTP.getData(assignment.getUrl());
        JSONArray submissions = new JSONArray(data);

        for (int i = 0; i < submissions.length(); i++) {
            JSONObject current = submissions.getJSONObject(i);
            int student_id = current.getInt(USER_ID);
            String status = current.getString(STATUS);

            if (student_id == course.testStudent
                    || !status.equals(GRADED)) continue;

            double score = current.getDouble("score");
            double points;
            if (name.matches(MIDTERM))
                points = score / total >= 0.5 ? 3 : 0;
            else if (total > 5)
                points = score / total * course.each;
            else points = score;
            students.computeIfPresent(student_id, (k, v) -> v + points);
            students.putIfAbsent(student_id, points);
        }
    }

    static void postPoints(Scanner in) {
        if (calculated == 0) {
            System.out.println("None assignment is being calculated. Please contact Yan for the bug.");
            return;
        }
        Utils.printDoneProcess("Calculation done!");
        Utils.printPrompt("whether post to Extra Credits? (Y/N)");
        String confirm = in.nextLine();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Posting cancelled. " +
                    "Contact Yan if it's because wrong assignments were calculated. ");
            return;
        }

        Utils.printProgress("posting to Extra Credits");
        String assignment = apiURL + "/" + extraCreditAssignment + "/submissions/";

        students.forEach((student, points) -> {
            String url = assignment + student;

            JSONObject submission = new JSONObject();
            JSONObject score = new JSONObject();
            score.put("posted_grade", String.format("%.2f", points));
            submission.put("submission", score);

            Utils_HTTP.putData(url, submission.toString());
        });
        Utils.printDoneProcess("Points all posted. Double-check on Canvas. ");
    }
}