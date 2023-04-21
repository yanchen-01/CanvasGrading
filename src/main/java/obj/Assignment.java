package obj;

import helpers.Utils;

public class Assignment {
    private final String name, url;
    private final String shortName, abbr, apiUrl;
    private final double total;

    public Assignment(String name, double total, String url) {
        this.name = name;
        shortName = name.replaceAll(" -.+", "");
        abbr = generateAbbr(shortName);
        this.total = total;
        this.url = url;
        apiUrl = Utils.getApiUrl(url);
    }

    private String generateAbbr(String shortName) {
        String[] info = shortName.split(" ");
        StringBuilder result = new StringBuilder();
        for(String s: info) {
            result.append(s.matches("\\d+") ?
                    s : s.charAt(0));
        }
        return result.toString();
    }

    public String getAbbr() {
        return abbr;
    }

    public String getShortName() {
        return shortName;
    }

    public String getApiUrl() {
        return apiUrl;
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
