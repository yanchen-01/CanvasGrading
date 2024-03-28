package obj;

import org.json.JSONObject;

/**
 * Score for a question.
 * TODO: may need to further simplify or reorganize - extend JSON, etc.
 */
public class QuestionScore {
    private final String qID;
    private double pts;
    private String comment;

    /**
     * Construct a score without comment
     *
     * @param qID question ID
     * @param pts initial points of the question, normally the full points possible
     */
    public QuestionScore(String qID, double pts) {
        this.qID = qID;
        this.pts = pts;
        this.comment = "";
    }

    public String getQID() {
        return qID;
    }

    /**
     * Generate a json object for the score.
     *
     * @return json object contains score and comment
     */
    public JSONObject generateJSON() {
        JSONObject result = new JSONObject();
        result.put("score", pts);
        result.put("comment", comment);
        return result;
    }

    /**
     * Add a line of comment (with pts calculation)
     *
     * @param comment the comment to be added
     */
    public void addComment(String comment) {
        comment = comment.replace("\"", "\\u0022");
        if (comment.equalsIgnoreCase("w"))
            pts = 0.5;
        else if (comment.equalsIgnoreCase("u"))
            pts = 0.0;
        else if (!comment.startsWith("-") && !comment.startsWith("+"))
            this.comment += comment + "\n";
        else {
            int splitIndex = comment.indexOf(" ");
            double deduction = splitIndex < 0 ?
                    Double.parseDouble(comment) :
                    Double.parseDouble(comment.substring(0, splitIndex));
            pts += deduction;
            this.comment += comment + "\n";
        }
    }

    /**
     * Getter for question id.
     *
     * @return the question id.
     */
    public String getqID() {
        return qID;
    }

    /**
     * Getter for points.
     *
     * @return the points.
     */
    public double getPts() {
        return pts;
    }

    /**
     * Getter for comment.
     *
     * @return the comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Setter for comment (no calculation, just set the comment)
     *
     * @param comment comment for the question
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
}
