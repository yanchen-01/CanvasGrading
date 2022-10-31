package obj;

import java.io.Serializable;

/**
 * A student object.
 *     <ul>
 *         <li>submissionID and attempt are from submissions API;</li>
 *         <li>name is from report.csv.</li>
 *     </ul>
 *     <p>Serializable so that can be stored as file (no longer needed).
 */
public class Student implements Serializable {
    private final int submissionID;
    private final int attempt;
    private String name;

    /**
     * Construct student with submissionID.
     *
     * @param submissionID submissionID of the student
     *                     (various depends on the assignment)
     * @param attempt number of attempt
     */
    public Student(int submissionID, int attempt) {
        this.submissionID = submissionID;
        this.attempt = attempt;
        this.name = "";
    }

    /**
     * Getter for the number of attempts
     * @return number of attempts
     */
    public int getAttempt() {
        return attempt;
    }

    /**
     * Getter for the submission id.
     *
     * @return the submission id
     */
    public int getSubID() {
        return submissionID;
    }

    /**
     * Setter for the name.
     *
     * @param name name of the student
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * A string representation of a student object.
     *
     * @return name_submissionID (name format: firstname lastname)
     */
    @Override
    public String toString() {
        return String.format("%s_%s", name, submissionID);
    }
}