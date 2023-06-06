package others;

import com.fasterxml.jackson.annotation.JsonProperty;
import helpers.Utils;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GetEmails {
    static final String FILENAME = "temp-emails";

    public static void main(String[] args) {
        Utils.runFunctionality(GetEmails::getEmails);
    }

    static void getEmails(Scanner in) throws Exception {
        Utils.printPrompt("Course URL (do not end with /");
        String courseURL = in.nextLine();
        courseURL = Utils.getApiUrl(courseURL) + "/users?per_page=200";

        Utils.printProgress("generating " + FILENAME + ".csv");
        User[] users = Utils.getObjFromURL(courseURL, User[].class);
        List<String[]> contents = new ArrayList<>();
        String[] headers = {"First Name", "Last Name", "Email"};
        contents.add(headers);
        for (User user : users) {
            contents.add(user.getEntry());
        }
        Utils.writeCSV(FILENAME, contents);
        Utils.printDoneProcess(FILENAME + ".csv generated and opened");

        Desktop.getDesktop().open(new File(FILENAME + ".csv"));
    }

    static class User {
        @JsonProperty("sortable_name")
        String name;
        @JsonProperty("email")
        String email;

        String[] getEntry() {
            String firstname = name.split(",")[1].trim();
            String lastname = name.split(",")[0].trim();
            return new String[]{firstname, lastname, email};
        }
    }
}
