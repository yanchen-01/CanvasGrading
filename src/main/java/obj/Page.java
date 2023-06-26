package obj;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Page {
    @JsonProperty("body")
    private String body;
    @JsonProperty("description")
    private String description;

    public String getBody() {
        return body;
    }

    public String getDescription() {
        return description;
    }
}
