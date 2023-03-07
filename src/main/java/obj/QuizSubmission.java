package obj;

public class QuizSubmission {
    private String subDate, studentName;
    private double percentage;
    private final int submissionID, attempt;

    public QuizSubmission(int submissionID, int attempt) {
        this.subDate = "";
        this.studentName = "";
        this.percentage = -1.0;
        this.submissionID = submissionID;
        this.attempt = attempt;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getAttempt() {
        return attempt;
    }

    public String getSubDate() {
        return subDate;
    }

    public double getPercentage() {
        return percentage;
    }

    public int getSubmissionID() {
        return submissionID;
    }

    public void setSubDate(String subDate) {
        this.subDate = subDate;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getPrefix() {
        return attempt + "-" + submissionID;
    }

    /**
     * A string representation of a quiz submission object.
     *
     * @return name_submissionID (name format: firstname lastname)
     */
    @Override
    public String toString() {
        return String.format("%s_%s", studentName, submissionID);
    }
}
