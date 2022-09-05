package grading;

import helpers.Utils;
import obj.Score;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;

import static constants.FolderNames.JSON_FOLDER;

public class Upload {

    static HashMap<String, Stack<Score>> allGrades = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter folder name for grading results, or enter skip if ready to upload: ");
        String folder = scanner.nextLine();
        if (!folder.equals("skip")) {
            Utils.goThroughFiles(Upload::readFile, folder);
            allGrades.forEach(Upload::saveJSONs);
            System.out.print("Finished reading results. Ready to upload (Y/N)?: ");
            if (!scanner.nextLine().equals("Y")){
                System.out.println("OK. Run this again when you are ready. ");
                return;
            }
        }

        Utils.askForParameters(false);
        Utils.goThroughFiles(Utils::uploadJSON, JSON_FOLDER);
        System.out.println("Uploading done. Double check Canvas to see if success.");
    }

    static void saveJSONs(String sID, Stack<Score> scores) {
        JSONObject questionJSON = new JSONObject();
        while (!scores.isEmpty()) {
            Score score = scores.pop();
            questionJSON.put(score.getQID(), score.generateJSON());
        }
        Utils.writeScoreAndCommentJSON(questionJSON, JSON_FOLDER + "/" + sID);
    }

    static void readFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            double full = Double.parseDouble(scanner.nextLine());
            Score grade = null;
            while (scanner.hasNextLine()) {
                String current = scanner.nextLine();
                if (current.matches("[0-9]+_[0-9]+")) {
                    String[] qID_sID = current.split("_");
                    allGrades.computeIfAbsent(qID_sID[1], k -> new Stack<>());
                    Stack<Score> g = allGrades.get(qID_sID[1]);
                    grade = new Score(qID_sID[0], full);
                    g.push(grade);
                } else if (grade != null && !current.isBlank()) {
                    grade.addComment(current);
                }
            }
        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}