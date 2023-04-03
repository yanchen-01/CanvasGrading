package obj;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Stack;

import static constants.JsonKeywords.LATE_STATUS;
import static constants.JsonKeywords.TEXT_COMMENT;

public class QuizScore {

    private final static String LEAVE_COMMENT = "comment";
    private final static String CANCEL_LATE = "submission";
    private final Stack<QuestionScore> scores;
    private final JSONObject adjustment;
    private double fudgePoints;
    private int studentID;

    public QuizScore() {
        adjustment = new JSONObject();
        scores = new Stack<>();
        fudgePoints = 0.0;
        studentID = 0;
    }

    public JSONObject getAdjustment() {
        return adjustment;
    }

    public void cancelLate() {
        generateAdjustment(CANCEL_LATE, "");
    }

    public void leaveComment(String comment) {
        if (comment.isEmpty()) return;
        comment = "As announced in class, " + comment;
        generateAdjustment(LEAVE_COMMENT, comment);
    }

    private void generateAdjustment(String key, String comment) {
        JSONObject element = new JSONObject();
        if (key.equals(CANCEL_LATE)) {
            element.put(LATE_STATUS, "none");
        } else if (key.equals(LEAVE_COMMENT) && !comment.isEmpty()) {
            element.put(TEXT_COMMENT, comment);
        }
        adjustment.put(key, element);
    }

    public Stack<QuestionScore> getScores() {
        return scores;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public double getFudgePoints() {
        return fudgePoints;
    }

    public void setFudgePoints(double fudgePoints) {
        this.fudgePoints = fudgePoints;
    }

    public void addScore(QuestionScore score) {
        scores.push(score);
    }

    public JSONObject generateJSON(int attempt) {
        JSONObject submission = new JSONObject();
        // either need to fudge points (already graded)
        // or need to grade
        // otherwise, no need to generate.
        if (fudgePoints != 0.0)
            submission.put("fudge_points", fudgePoints);
        else if (!scores.isEmpty()) {
            JSONObject questions = new JSONObject();
            while (!scores.isEmpty()) {
                QuestionScore score = scores.pop();
                questions.put(score.getQID(), score.generateJSON());
            }
            submission.put("questions", questions);
        } else return null;

        submission.put("attempt", attempt);
        JSONArray array = new JSONArray();
        array.put(submission);

        JSONObject result = new JSONObject();
        result.put("quiz_submissions", array);
        return result;
    }

}
