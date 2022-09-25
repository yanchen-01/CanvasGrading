package grading;

import constants.FolderNames;
import helpers.Utils;
import obj.Question;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import static jff.Constants_JFF.*;

public class GradeJFFs {
    static HashMap<String, JffQuestion> JFF_Qs;
    static HashSet<String> notDFA;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        JFF_Qs = new HashMap<>();
        notDFA = (HashSet<String>) Utils.getObjectFromFile(
                SET_NOT_DFA);
        HashSet<Question> jffQs = (HashSet<Question>) Utils.getObjectFromFile(
                JFF_QUESTIONS);
        try (Scanner scanner = new Scanner(System.in)) {
            assert jffQs != null;
            jffQs.forEach((question) -> setScores(question, scanner));
            Utils.printPrompt("folder name of batch test results");
            String folder = scanner.nextLine();
            Utils.goThroughFiles(GradeJFFs::grade, folder);
            System.out.printf("Grading results are written to %s folder, " +
                    "put them together with other grading result files and run Upload.java", FolderNames.JFF_RESULTS);
        } catch (Exception e) {
            System.out.println("Terminated: " + e.getMessage());
        }
    }

    static void setScores(Question question, Scanner scanner) {
        String id = String.valueOf(question.getId());
        rewriteResult(id);

        String type = question.getJffType();
        Utils.printPrompt("pt for each test case for question " + question.getContent());
        double accept = scanner.nextDouble();
        double output = 0.0;
        if (type.equals("turing")) {
            System.out.print("If it's a transducer, ");
            Utils.printPrompt("pt for each correct output");
            output = scanner.nextDouble();
        }
        JffQuestion q = new JffQuestion(type, accept, output);
        JFF_Qs.put(id, q);
        scanner.nextLine(); // make sure it changes the line...
    }

    static void rewriteResult(String id) {
        String filename = FolderNames.JFF_RESULTS + "/" + id;
        File file = new File(filename + "p.txt");
        if (!file.exists()) return;
        try (Scanner scanner = new Scanner(file)) {
            String score = scanner.nextLine();
            StringBuilder content = new StringBuilder(score + "\n");
            while (scanner.hasNextLine()) {
                String current = scanner.nextLine();
                if (current.matches("\\d+-\\d+_\\d+")) {
                    String next = scanner.nextLine();
                    if (!NOT_DFA.startsWith(next)) {
                        next = String.format("-%s %s\n", score.replace(".0", ""), next);
                        content.append(current).append("\n").append(next);
                    }
                }
            }
            Utils.writeToFile(filename, content.toString());
        } catch (FileNotFoundException e) {
            System.out.println("!Warning: " + filename + " not found");
        }
        Utils.deleteFile(file);
    }

    static void grade(File file) {
        //Filename format: resultsAttempt-subID_qID.jff.txt
        String studentInfo = file.getName().replace("results", "");
        studentInfo = studentInfo.replace(".jff.txt", "");
        StringBuilder comment = new StringBuilder(studentInfo + "\n");

        String qID = studentInfo.split("_")[1];
        JffQuestion question = JFF_Qs.get(qID);
        if (question.type.equals("dfa") && notDFA.contains(studentInfo))
            comment.append("-1 ").append(
                    NOT_DFA).append("\n");

        String gradingResult = gradeFile(file, question);
        gradingResult = gradingResult.isEmpty()? "\n": gradingResult;
        comment.append(gradingResult);

        Utils.writeToFile(FolderNames.JFF_RESULTS + "/" + qID, comment.toString());
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
                numOfActualA += current.contains(ACCEPT)? 1 : 0;
                numOfActualR += current.contains(REJECT)? 1 : 0;
                if (current.endsWith(") ")){
                    numOfWrongResult++;
                }
                // not end with ) but has ( -> output wrong
                else if (current.contains("(")){
                    numOfWrongOutput++;
                }
            }

            double total = (numOfActualA + numOfActualR) * question.each;
            if (numOfActualA == numOfTests)
                return String.format("-%.0f your machine accepts everything!\n", total);
            else if (numOfActualR == numOfTests)
                return String.format("-%.0f your machine rejects everything!\n", total);
            String error = "";
            if (numOfWrongResult > 0)
                error += String.format("-%.1f for failing %d test cases because of wrong accept/reject. \n",
                        numOfWrongResult * question.each, numOfWrongResult);
            if (numOfWrongOutput > 0)
                error += String.format("-%.1f for failing %d test cases because of wrong output. \n",
                        numOfWrongOutput * question.eachOutput, numOfWrongOutput);
            return error;

        } catch (FileNotFoundException e) {
            return "file not found";
        }
    }

    static class JffQuestion {
        String type;
        double eachAccept;
        double eachOutput;
        double each;

        public JffQuestion(String type, double eachAccept, double eachOutput) {
            this.type = type;
            this.eachAccept = eachAccept;
            this.eachOutput = eachOutput;
            each = eachAccept + eachOutput;
        }
    }

}
