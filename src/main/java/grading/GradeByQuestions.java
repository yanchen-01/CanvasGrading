package grading;

import helpers.Utils;
import helpers.Utils_QuizSetup;
import helpers.Utils_QuizUpload;
import obj.Quiz;

import java.awt.*;
import java.io.File;
import java.util.Scanner;

import static constants.FolderNames.*;
import static constants.Parameters.REPORT_NAME;

public class GradeByQuestions {
    static Quiz quiz;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, GradeByQuestions::run);
    }

    static void run(Scanner in) throws Exception {
        Utils.printPrompt("quiz url (do not end with /)");
        String url = in.nextLine();

        Utils.printProgress("getting quiz info");
        quiz = new Quiz(url);
        Utils.printDoneProcess("Quiz info fetched");

        int option = Utils.getOption(in, """
                one option (1 or 2):
                1. Setup grading files;
                2. Upload grading results""");

        switch (option) {
            case 1 -> setup(in);
            case 2 -> upload(in);
            default -> System.out.println("Wrong option, program terminated. ");
        }
    }

    static void setup(Scanner in) throws Exception {
        Utils.makeFolder(GRADING_FOLDER);
        Utils.makeFolder(JSON_FOLDER);
        quiz.fetchSubmissionsAndQuestions();

        Utils_QuizSetup.setQuiz(quiz);
        if (!quiz.getUploadQuestions().isEmpty())
            Utils_QuizSetup.organizeSubmissions(in);

        Utils_QuizSetup.downloadReport(in);
        Utils.readCSV(REPORT_NAME, Utils_QuizSetup::readSubmissions);
        Utils_QuizSetup.generateFiles();

        //Utils_QuizUpload.uploadResults("MC & unanswered adjustment", quiz);

        Utils.printDoneProcess("Setting up done. After grading, run again and choose 2. Upload");
        Desktop.getDesktop().open(new File(INDEX + ".html"));
    }

    static void upload(Scanner in) throws Exception {
        Utils.makeFolder(JSON_FOLDER);
        int option = Utils.getOption(in, """
                one option (1, 2, 3 or 4):
                1. Upload grading result (no extra adjustment);
                2. Extra adjustment only (grading result already uploaded);
                3. Both of above;
                4. Upload generated JSON (already did any of above)""");
        Utils.checkOption(option, 4);
        if (option == 4) {
            Utils_QuizUpload.confirmUpload(in, "pre-generated JSONs", quiz);
            return;
        }
        if (option != 2) {
            Utils_QuizUpload.readGradingResults(in);
            Utils_QuizUpload.confirmUpload(in, "grading", quiz);
        }
        if (option != 1) {
            Utils_QuizUpload.fudgePoints(in, quiz);
            Utils_QuizUpload.confirmUpload(in, "extra adjustment", quiz);
        }
    }
}