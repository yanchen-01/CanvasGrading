package grading;

import helpers.Utils;
import helpers.Utils_HTTP;
import helpers.Utils_Setup;
import helpers.Utils_SetupSets;
import jff.Constants_JFF;
import jff.Utils_JFF;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Scanner;

import static constants.FolderNames.*;
import static constants.Parameters.*;
import static helpers.Utils_Setup.generateHTMLs;
import static helpers.Utils_Setup.setQuestions;

public class Setup {

    static String reportName;

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {
            Utils.makeFolder(GRADING_FOLDER);
            Utils.makeFolder(JSON_FOLDER);

            boolean hasJFF = false;
            Utils.askForParameters(scanner, true);
            if (!JFF_SUBMISSION_FOLDER.isEmpty() && !JFF_SUBMISSION_FOLDER.equals("N")) {
                Utils.makeFolder(JFF_FOLDER);
                Utils.makeFolder(JFF_RESULTS);
                hasJFF = true;
            }

            System.out.println("...Fetching questions and submissions...");
            String sub = API_URL + "/submissions?page=1&per_page=100";
            String subR = Utils_HTTP.getData(sub);
            Utils_Setup.setStudents(CLASS, subR);

            String q = API_URL + "/questions?page=1&per_page=100";
            String qR = Utils_HTTP.getData(q);
            if (qR.equals("[]"))
                qR = Utils_SetupSets.getQuestionSets();
            setQuestions(qR);

            if (hasJFF) {
                Utils_JFF.notDFA = new HashSet<>();
                Utils.goThroughFiles(Utils_JFF::organize, JFF_SUBMISSION_FOLDER);
                Utils.saveObject(Constants_JFF.SET_NOT_DFA, Utils_JFF.notDFA);
            }

            System.out.println("\u2713 Questions and Submissions fetched.");
            System.out.println("...Fetching submission report...");
            downloadReport(scanner);
            Utils.readCSV(reportName, Utils_Setup::readSubmissions);

            generateHTMLs();

            System.out.print("""
                    \u2713 HTMLs and JSONs generated.
                    Upload scoring for MC adjustment & unanswered (Y/N)?
                    >>\s""");
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

    static void downloadReport(Scanner scanner) {
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
            reportName = scanner.nextLine();
            Utils_HTTP.getFile(fileURL, reportName.concat(".csv"));
            System.out.println("\u2713 " + reportName + ".csv downloaded.");
        } catch (Exception e) {
            System.out.println("Fail to fetch the file." +
                    "\nPlease manually download the report on Canvas (Quiz Statistics -> Student Analysis)");
            System.out.print("Then enter filename of downloaded submission report (without .csv): \n>> ");
            reportName = scanner.nextLine();
        }
    }
}
