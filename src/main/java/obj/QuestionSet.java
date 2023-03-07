package obj;

import java.io.Serializable;
import java.util.HashSet;

public class QuestionSet extends HashSet<Question> implements Serializable {
    private final String name;
    private double score;

    public QuestionSet(String name, double score) {
        super(20);
        this.name = name;
        this.score = score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return name + ": " + super.toString();
    }
}
