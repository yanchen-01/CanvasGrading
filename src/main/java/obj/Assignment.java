package obj;

public class Assignment {
    private final String name;
    private final double total;
    private final String url;

    public Assignment(String name, double total, String url) {
        this.name = name;
        this.total = total;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public double getTotal() {
        return total;
    }

    public String getUrl() {
        return url;
    }
}
