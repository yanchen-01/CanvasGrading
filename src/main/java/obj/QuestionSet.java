package obj;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;

import static constants.JsonKeywords.*;

public class QuestionSet {
    HashSet<Question> questions;
    @JsonProperty(NAME)
    private String name;
    @JsonProperty(GROUP_POINTS)
    private double score;
    @JsonProperty(PICK_COUNT)
    private int pickCount;

    public QuestionSet(@JsonProperty(NAME) String name, @JsonProperty(GROUP_POINTS) double score) {
        questions = new HashSet<>();
        this.name = name;
        this.score = score;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public HashSet<Question> getQuestions() {
        return questions;
    }

    public int getPickCount() {
        return pickCount;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return name + ": " + questions.toString();
    }
}
