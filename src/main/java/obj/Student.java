package obj;

import java.io.Serializable;

/**
 * A student object.
 * <p>Serializable so that can store a hashmap of sid and student.
 */
public class Student implements Serializable {
    private final String name;
    private int submissionID;

    /**
     * Construct student with name.
     * <p>submissionID will be set later based on different assignment
     *
     * @param name name of the student
     */
    public Student(String name) {
        this.name = name;
        submissionID = 0;
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
     * Setter for the submission id.
     *
     * @param submissionID submission id (get from submissions API)
     */
    public void setSubID(int submissionID) {
        this.submissionID = submissionID;
    }

    /**
     * A string representation of a student object.
     *
     * @return name_submissionID (name format: lastname, firstname)
     */
    @Override
    public String toString() {
        return String.format("%s_%s", name, submissionID);
    }
}