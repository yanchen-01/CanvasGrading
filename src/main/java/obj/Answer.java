package obj;

/**
 * Answer from a student for a question.
 * May be converted to a record (JAVA 14 +)
 */
public class Answer {
    private final Student student;
    private final String answer;

    public Answer(Student student, String answer) {
        this.student = student;
        this.answer = answer;
    }

    public Student getStudent() {
        return student;
    }

    public String getAnswer() {
        return answer;
    }

}