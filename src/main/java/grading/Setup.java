package grading;

import helpers.Utils;
import helpers.Utils_HTTP;
import helpers.Utils_Setup;
import org.json.JSONObject;

import java.util.Scanner;

import static constants.FolderNames.GRADING_FOLDER;
import static constants.FolderNames.JSON_FOLDER;
import static constants.Parameters.API_URL;
import static constants.Parameters.CLASS;
import static helpers.Utils_Setup.generateHTMLs;
import static helpers.Utils_Setup.setQuestions;

public class Setup {

    static String reportName;

    public static void main(String[] args) {

        try {
            Utils.makeFolder(GRADING_FOLDER);
            Utils.makeFolder(JSON_FOLDER);

            Utils.askForParameters(true);

            System.out.println("...Fetching questions and submissions...");
            String sub = API_URL + "/submissions?page=1&per_page=100";
            String subR = Utils_HTTP.getData(sub);
            Utils_Setup.setStudents(CLASS, subR);

            String q = API_URL + "/questions?page=1&per_page=100";
            String qR = Utils_HTTP.getData(q);
            setQuestions(qR);

            System.out.println("\u2713 Questions and Submissions fetched.");
            System.out.println("...Fetching submission report...");
            downloadReport();
            Utils.readCSV(reportName, Utils_Setup::readSubmissions);

            generateHTMLs();

            System.out.print("""
                    \u2713 HTMLs and JSONs generated.
                    Upload scoring for MC adjustment & unanswered (Y/N)?
                    >>\s""");
            Scanner scanner = new Scanner(System.in);
            String answer = scanner.nextLine();
            if (answer.startsWith("Y")) {
                Utils.goThroughFiles(Utils::uploadJSON, JSON_FOLDER);
                System.out.println("\u2713 MC & unanswered adjustment done");
            }

            System.out.println("\u2713 Setting up done. After grading, run Upload.java");
        } catch (Exception e) {
            System.out.println("Terminated: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void downloadReport() {
        Scanner in = new Scanner(System.in);
        try {
            String url = API_URL + "/reports?quiz_report[report_type]=student_analysis";
            String data = Utils_HTTP.httpRequest("POST", url, "");
            JSONObject object = new JSONObject(data);
            int id = object.getInt("id");
            url = API_URL + "/reports/" + id;
            do {
                data = Utils_HTTP.getData(url);
                object = new JSONObject(data);
            } while (!object.has("file"));

            JSONObject file = object.getJSONObject("file");
            String fileURL = file.getString("url");

            System.out.print("Submission report fetched. Name your download (without .csv): \n>> ");
            reportName = in.nextLine();
            Utils_HTTP.getFile(fileURL, reportName);
            System.out.println("\u2713 " + reportName + ".csv downloaded.");
        } catch (Exception e) {
            System.out.println("Fail to fetch the file." +
                    "\nPlease manually download the report on Canvas (Quiz Statistics -> Student Analysis)");
            System.out.print("Then enter filename of downloaded submission report (without .csv): \n>> ");
            reportName = in.nextLine();
        }
    }
}
