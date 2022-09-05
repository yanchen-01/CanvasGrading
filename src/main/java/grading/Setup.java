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
            String sub = API_URL + "/submissions?page=1&per_page=100";
            String subR = Utils_HTTP.getData(sub);
            Utils_Setup.setStudents(CLASS, subR);

            String q = API_URL + "/questions?page=1&per_page=100";
            String qR = Utils_HTTP.getData(q);
            setQuestions(qR);

            downloadReport();
            Utils.readCSV(reportName, Utils_Setup::readSubmissions);

            generateHTMLs();

            System.out.print("All setup. Upload scoring for MC adjustment & unanswered (Y/N)? ");
            Scanner scanner = new Scanner(System.in);
            String answer = scanner.nextLine();
            if (answer.startsWith("Y"))
                Utils.goThroughFiles(Utils::uploadJSON, JSON_FOLDER);

            System.out.println("Setting up done. After grading, run Upload.java");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void downloadReport() {
        Scanner in = new Scanner(System.in);
        try {
            System.out.println("Fetching submission report downloadable link ...");
            String url = API_URL + "/reports?quiz_report[report_type]=student_analysis&include[]=file";
            String data = Utils_HTTP.httpRequest("POST", url, "");
            JSONObject object = new JSONObject(data);
            JSONObject file = object.getJSONObject("file");
            String fileURL = file.getString("url");

            System.out.println("Fetching done. Name your download (without .csv): ");
            reportName = in.nextLine().concat(".csv");
            Utils_HTTP.getFile(fileURL, reportName);
        } catch (Exception e) {
            System.out.println("Fail to get downloadable link." +
                    "\nPlease manually download the report on Canvas (Quiz Statistics -> Student Analysis)");
            System.out.println("Then enter filename of downloaded submission report (without .csv): ");
            reportName = in.nextLine().concat(".csv");
        }
    }
}
