package others;

import com.opencsv.CSVWriter;
import helpers.Utils;
import jff.Utils_JFF;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

import static constants.FolderNames.INDEX;

public class GradeTermProject {

    static HashMap<String, Score> RESULTS = new HashMap<>();
    static ArrayList<String[]> CONTENT = new ArrayList<>();
    static String SUBMISSION_FOLDER;

    public static void main(String[] args) {

        try {
            Scanner scanner = new Scanner(System.in);
            Utils.printPrompt("Folder name for submissions");
            SUBMISSION_FOLDER = scanner.nextLine();
            Utils.goThroughFiles(GradeTermProject::check, SUBMISSION_FOLDER);
            Utils.printPrompt("Test result folder");
            String folder = scanner.nextLine();
            Utils.goThroughFiles(GradeTermProject::score, folder);
            Utils.printPrompt("Result filename (without .csv)");
            String filename = scanner.nextLine();
            writeResult(filename);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    static void writeResult(String filename) {
        filename += ".csv";
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            RESULTS.forEach((group, score) -> {
                String[] row = {group, score.score, score.comment};
                CONTENT.add(row);
            });
            writer.writeAll(CONTENT);
            Desktop.getDesktop().open(new File(filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void score(File file) {
        if (!file.isFile()) return;
        try (Scanner scanner = new Scanner(file)) {
            int wrong = 0;
            while (scanner.hasNextLine()) {
                String current = scanner.nextLine();
                if (current.contains("(")) wrong++;
            }

            String id = Utils.removeNonDigits(file.getName());
            String score = String.format("%.1f", 70.0 - wrong * 3.5);
            String comment = wrong == 0 ? "" : String.format("Failed %d test cases", wrong);

            RESULTS.put(id, new Score(score, comment));
        } catch (Exception e) {
            System.out.println(file.getAbsolutePath());
        }
    }

    static void check(File folder) {
        try {
            if (!folder.isDirectory()) return;
            String error = "";
            if (Objects.requireNonNull(folder.listFiles()).length != 1)
                error = "Wrong submission format";

            String groupID = Utils.removeNonDigits(folder.getName());

            File file = Objects.requireNonNull(folder.listFiles())[0];
            String oldName = file.getName();
            if (error.isEmpty() && !oldName.endsWith(".jff"))
                error = "Wrong file format";
            else {
                String preCheckMachine = Utils_JFF.preCheckMachine(file, "turing");
                error = preCheckMachine.isEmpty() ? "" : preCheckMachine;
            }

            String newName = SUBMISSION_FOLDER + "/";
            if (!error.isEmpty()) {
                newName = newName + groupID + oldName;
                Score score = new Score("O", error);
                RESULTS.put(groupID, score);
                System.out.println(error);
            } else newName = newName + groupID + ".jff";

            if (!file.renameTo(new File(newName)))
                System.out.printf("!Warning: fail to rename '%s' to '%s'\n", oldName, newName);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static class Score {
        String score;
        String comment;

        public Score(String score, String comment) {
            this.score = score;
            this.comment = comment;
        }
    }
}
