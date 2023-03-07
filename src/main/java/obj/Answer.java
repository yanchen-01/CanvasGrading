package obj;

import java.io.Serializable;

/**
 * Answer from a student for a question.
 */
public class Answer implements Serializable {
    private final QuizSubmission submission;
    private final String answer;

    public Answer(QuizSubmission submission, String answer) {
        this.submission = submission;
        this.answer = answer;
    }

    public int getSubmissionID() {
        return submission.getSubmissionID();
    }

    public int getAttempt() {
        return submission.getAttempt();
    }
    public QuizSubmission getQuizSubmission() {
        return submission;
    }

    public String getAnswer() {
        return answer;
    }

}