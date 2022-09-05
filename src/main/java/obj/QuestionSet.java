package obj;

import java.util.HashSet;

public class QuestionSet extends HashSet<Question> {
    private final String name;
    private final double score;

    public QuestionSet(String name, double score) {
        super(20);
        this.name = name;
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
