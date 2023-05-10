package obj;

import com.fasterxml.jackson.annotation.JsonProperty;

import static constants.JsonKeywords.STATUS;
import static constants.JsonKeywords.USER_ID;

public class Submission {
    private int id;
    @JsonProperty(USER_ID)
    private int userID;
    @JsonProperty(STATUS)
    private String status;
    private double score;
    private Group group;
    private User user;

    public User getUser() {
        return user;
    }

    public String getUserName() {
        return user.name;
    }

    public String getGroupName() {
        return group.name;
    }

    public int getId() {
        return id;
    }

    public Group getGroup() {
        return group;
    }

    public String getStatus() {
        return status;
    }

    public double getScore() {
        return score;
    }

    public int getUserID() {
        return userID;
    }

    public static class Group {
        String name;

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class User {
        String name;

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
