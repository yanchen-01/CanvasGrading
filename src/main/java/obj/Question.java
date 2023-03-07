package obj;

import helpers.Utils_HTML;

import java.io.Serializable;
import java.util.Objects;
import java.util.Stack;

/**
 * A question with a stack of answers
 */
public class Question implements Serializable {
    private final int id;
    private final String type;
    private transient final Stack<Answer> answers;
    private String content;
    private String jffType;

    /**
     * Construct a question (all fields defined, current not in use).
     *
     * @param id      question ID
     * @param content question detail
     * @param type    the type of the question
     */
    public Question(int id, String content, String type) {
        this.id = id;
        this.content = content;
        this.type = type;
        answers = new Stack<>();
        jffType = "";
    }

    /**
     * Construct a question with ID and type.
     *
     * @param id   question ID
     * @param type the type of the question
     */
    public Question(int id, String type) {
        this(id, "", type);
    }

    /**
     * Getter for content.
     * <p>For JFF, probably use {@link String getInfo} instead
     *
     * @return content of question.
     */
    public String getContent() {
        return content;
    }

    /**
     * Setter for content (will parse to html paragraph).
     * TODO: may need to update to convert more unicode back to symbol.
     *
     * @param content content in raw data
     */
    public void setContent(String content) {
        if (content.contains(".jff"))
            setJffType(Utils_HTML.getBoldText(content, "dfa"));

        content = content.replace("\\u003c", "<");
        content = content.replace("\\u003e", ">");
        content = content.replace("\\\"", "\"");
        this.content = content;
    }

    /**
     * Getter for id.
     *
     * @return id of the question
     */
    public int getId() {
        return id;
    }

    /**
     * Getter for type.
     *
     * @return type of the question
     */
    public String getType() {
        return type;
    }


    /**
     * Getter for the stack of answers.
     *
     * @return stack of students' answers to this question
     */
    public Stack<Answer> getAnswers() {
        return answers;
    }

    /**
     * Add a student answer to the answer stack
     *
     * @param answer an Answer object
     */
    public void addStudentAnswer(Answer answer) {
        answers.push(answer);
    }

    /**
     * Two Questions are equal if they have the same id.
     *
     * @param o other object
     * @return true if id is the same
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Question answers = (Question) o;
        return Objects.equals(id, answers.id);
    }

    /**
     * Hash code of question is based on ID
     *
     * @return the hashcode of the id
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    /**
     * (For jff question ONLY) Type of the jff.
     *
     * @return type of the machine
     */
    public String getJffType() {
        return jffType;
    }

    /**
     * (For jff question ONLY) Set type of the jff.
     */
    public void setJffType(String jffType) {
        if (jffType.contains("TM"))
            this.jffType = "turing";
        else
            this.jffType = jffType.toLowerCase();
    }
}
