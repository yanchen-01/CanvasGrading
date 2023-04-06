package grading;

import helpers.Utils;
import jff.JffQuestion;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import static jff.Constants_JFF.*;

public class GradeJFFs {
    static HashMap<Integer, JffQuestion> JFF_Qs;
    static HashMap<String, String> ERRORS;
    static HashSet<String> SET_OF_NOT_DFA;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Utils.runFunctionality(in, GradeJFFs::run);
    }

    static void run(Scanner in) throws FileNotFoundException {
        setup();
        preCheck();
        Utils.printPrompt("folder name of batch test results");
        String folder = in.nextLine();
        Utils.goThroughFiles(GradeJFFs::grade, folder);
        System.out.printf("Grading results are written to %s folder, " +
                "put them together with other grading result files and run Upload.java\n", JFF_GRADING_RESULTS);
    }

    @SuppressWarnings("unchecked")
    static void setup() {
        Utils.makeFolder(JFF_GRADING_RESULTS);
        JFF_Qs = (HashMap<Integer, JffQuestion>) Utils.getObjectFromFile(JFF_QUESTIONS);
        assert JFF_Qs != null;
        JFF_Qs.forEach((qID, question) -> {
            String filename = JFF_GRADING_RESULTS + "/" + qID;
            String content = question.getTotal() + "\n";
            Utils.writeToFile(filename, content);
        });
    }

    @SuppressWarnings("unchecked")
    static void preCheck() {
        SET_OF_NOT_DFA = new HashSet<>();
        ERRORS = (HashMap<String, String>) Utils.getObjectFromFile(
                JFF_ERRORS);
        assert ERRORS != null;
        ERRORS.forEach(GradeJFFs::preGrading);
    }

    static void preGrading(String studentInfo, String error) {
        if (error.equals(NOT_DFA))
            SET_OF_NOT_DFA.add(studentInfo);
        else {
            int qID = extractQID(studentInfo);
            double total = JFF_Qs.get(qID).getTotal();
            String comment = String.format("-%.1f %s", total, error);
            String filename = JFF_GRADING_RESULTS + "/" + qID;
            String content = String.format("""
                    %s
                    %s""", studentInfo, comment);
            Utils.writeToFile(filename, content);
        }
    }

    static int extractQID(String studentInfo) {
        // format: Attempt-subID_qID
        String qID = studentInfo.split("_")[1];
        qID = Utils.removeNonDigits(qID);
        return Integer.parseInt(qID);
    }

    static void grade(File file) {
        if (!file.isFile()) return;
        //Filename format: resultsAttempt-subID_qID.jff.txt
        String filename = file.getName();
        if (!filename.matches("results\\d+-\\d+_\\d+.jff.txt")) return;
        String studentInfo = filename.replace("results", "");
        studentInfo = studentInfo.replace(".jff.txt", "");
        StringBuilder comment = new StringBuilder(studentInfo + "\n");

        int qID = extractQID(studentInfo);
        JffQuestion question = JFF_Qs.get(qID);
        if (question.getJffType().equals("dfa")
                && SET_OF_NOT_DFA.contains(studentInfo))
            comment.append("-1 ").append(NOT_DFA); // NOT_DFA has line breaker already

        String gradingResult = gradeFile(file, question);
        gradingResult = gradingResult.isEmpty() ? "\n" : gradingResult;
        comment.append(gradingResult);

        Utils.writeToFile(JFF_GRADING_RESULTS + "/" + qID, comment.toString());
    }

    static String gradeFile(File file, JffQuestion question) {
        int numOfTests = 0;
        int numOfActualA = 0;
        int numOfActualR = 0;
        int numOfWrongOutput = 0;
        int numOfWrongResult = 0;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String current = scanner.nextLine();
                numOfTests++;
                numOfActualA += current.contains(ACCEPT) ? 1 : 0;
                numOfActualR += current.contains(REJECT) ? 1 : 0;
                if (current.endsWith(") ")) {
                    numOfWrongResult++;
                }
                // not end with ) but has ( -> output wrong
                else if (current.contains("(")) {
                    numOfWrongOutput++;
                }
            }

            double total = (numOfActualA + numOfActualR) * question.getEach();
            if (numOfActualA == numOfTests && question.getOutput() == 0.0)
                return String.format("-%.0f your machine accepts everything!\n", total);
            else if (numOfActualR == numOfTests)
                return String.format("-%.0f your machine rejects everything!\n", total);
            String error = "";
            if (numOfWrongResult > 0)
                error += String.format("-%.1f for failing %d test cases because of wrong accept/reject. \n",
                        numOfWrongResult * question.getEach(), numOfWrongResult);
            if (numOfWrongOutput > 0)
                error += String.format("-%.1f for failing %d test cases because of wrong output. \n",
                        numOfWrongOutput * question.getOutput(), numOfWrongOutput);
            return error;

        } catch (FileNotFoundException e) {
            return "file not found";
        }
    }

}
