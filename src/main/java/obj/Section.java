package obj;

import com.fasterxml.jackson.annotation.JsonProperty;

import static constants.JsonKeywords.COURSE_ID;
import static constants.Parameters.API;

public class Section {
    private int id;
    private String name;
    @JsonProperty(COURSE_ID)
    private int courseID;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCourseID() {
        return courseID;
    }

    public String getNum() {
        return name.replaceAll(".*Section (0)?", "");
    }

    public String getApiUrl() {
        return String.format("%s/sections/%d", API, id);
    }
}
