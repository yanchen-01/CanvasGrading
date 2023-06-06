package grading;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.Utils;
import helpers.Utils_HTTP;
import obj.Assignment;
import obj.Submission;

import java.util.Scanner;

public class GradeDiscussions {

    public static void main(String[] args) {
        Utils.runFunctionality(GradeDiscussions::grade);
    }

    static void grade(Scanner in) throws JsonProcessingException {
        Utils.printPrompt("discussion url or course url for all discussions");
        String url = in.nextLine();
        url = Utils.getApiUrl(url);
        if (url.contains("discussion_topics")) {
            gradeOne(url);
        } else {
            gradeAll(url + "/assignments");
        }
        Utils.printDoneProcess("Graded, double-check on Canvas");
    }

    static void gradeOne(String url) throws JsonProcessingException {
        Utils.printProgress("Getting discussion info");
        Discussion discussion = Utils.getObjFromURL(url, Discussion.class);
        gradeDiscussion(discussion.assignment);
    }

    static void gradeAll(String url) throws JsonProcessingException {
        Utils.printProgress("Getting all discussions");
        Assignment[] assignments = Utils.getObjFromURL(url + "?per_page=100", Assignment[].class);
        for (Assignment assignment : assignments) {
            if (assignment.getTypes()[0].equals("discussion_topic"))
                gradeDiscussion(assignment);
        }
    }

    static void gradeDiscussion(Assignment assignment) throws JsonProcessingException {
        double score = assignment.getTotal();
        if (score >= 2) return;
        String name = assignment.getName();
        Utils.printProgress("Grading " + name);

        String url = assignment.getHtmlUrl();
        url = Utils.getApiUrl(url) + "/submissions?per_page=200";

        Submission[] submissions = Utils.getObjFromURL(url, Submission[].class);
        for (Submission submission : submissions) {
            if (submission.getStatus().equals("unsubmitted")
                    || submission.getStatus().equals("graded"))
                continue;
            String sURL = url + "/" + submission.getUserID();
            String data = String.format("""
                    {
                        "submission": {
                            "posted_grade":%.1f
                        }
                    }
                    """, score);
            Utils_HTTP.putData(sURL, data);
        }
    }

    static class Discussion {
        @JsonProperty("assignment")
        Assignment assignment;
    }
}
