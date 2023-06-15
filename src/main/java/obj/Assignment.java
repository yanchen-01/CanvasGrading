package obj;

import com.fasterxml.jackson.annotation.JsonProperty;
import helpers.Utils;

import java.util.ArrayList;

import static constants.JsonKeywords.*;

public class Assignment {
    private int id;
    private String name;
    private boolean grouped;
    @JsonProperty(URL)
    private String htmlUrl;
    @JsonProperty(POINTS)
    private double total;
    @JsonProperty(HAS_GRADED)
    private boolean hasGraded;
    @JsonProperty(NEED_GRADE)
    private int ungraded;
    @JsonProperty(RUBRIC)
    private ArrayList<Rubric> rubrics;
    @JsonProperty("submission_types")
    private String[] types;

    public boolean isGrouped() {
        return grouped;
    }

    @JsonProperty("group_category_id")
    public void setGrouped(Integer groupID) {
        this.grouped = groupID != null;
    }

    public boolean isHasGraded() {
        return hasGraded;
    }

    public int getUngraded() {
        return ungraded;
    }

    public int getId() {
        return id;
    }

    public String getAbbr(String shortName) {
        String[] info = shortName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String s : info) {
            result.append(s.matches("\\d+") ?
                    s : s.charAt(0));
        }
        return result.toString();
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getApiUrl() {
        return Utils.getApiUrl(htmlUrl);
    }

    public double getTotal() {
        return total;
    }

    public String getName() {
        return name;
    }

    public String[] getTypes() {
        return types;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return name.replaceAll(" -.+", "");
    }

    public ArrayList<Rubric> getRubrics() {
        return rubrics;
    }

}