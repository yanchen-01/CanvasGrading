package obj;

public class Rubric {
    private String id;
    private String description;
    private double points;

    public double getPoints() {
        return points;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description+id;
    }
}
