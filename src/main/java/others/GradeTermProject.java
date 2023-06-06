package others;

import com.opencsv.CSVWriter;
import helpers.Utils;
import jff.Utils_JFF;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class GradeTermProject {

    static HashMap<String, Score> RESULTS = new HashMap<>();
    static ArrayList<String[]> CONTENT = new ArrayList<>();
    static String SUBMISSION_FOLDER;
    static String RESULT = "temp-result.csv";
    static String RESULTS_MAP = "temp-results";

    public static void main(String[] args) {
        Utils.runFunctionality(GradeTermProject::grade);
    }

    static void grade(Scanner in) throws Exception {
        int option = Utils.getOption(in, """
                one option (1 or 2):
                1. Setup;
                2. Write Results""");
        Utils.checkOption(option, 2);
        switch (option) {
            case 1 -> setUp(in);
            case 2 -> writeResult(in);
        }
    }

    static void setUp(Scanner in) throws FileNotFoundException {
        Utils.printPrompt("Folder name for submissions");
        SUBMISSION_FOLDER = in.nextLine();
        Utils.goThroughFiles(GradeTermProject::check, SUBMISSION_FOLDER);
        Utils.saveObject(RESULTS_MAP, RESULTS);
    }

    @SuppressWarnings("unchecked")
    static void writeResult(Scanner in) throws IOException {
        RESULTS = (HashMap<String, Score>) Utils.getObjectFromFile(RESULTS_MAP);
        Utils.printPrompt("Test result folder");
        String folder = in.nextLine();
        Utils.goThroughFiles(GradeTermProject::score, folder);
        try (CSVWriter writer = new CSVWriter(new FileWriter(RESULT))) {
            RESULTS.forEach((group, score) -> {
                String[] row = {group, score.score, score.comment};
                CONTENT.add(row);
            });
            writer.writeAll(CONTENT);
            Desktop.getDesktop().open(new File(RESULT));
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
                else if (!current.contains("A")
                    && !current.contains("R"))
                    System.out.println(file.getName() + "seems empty");
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
            } else newName = newName + groupID + ".jff";

            if (!file.renameTo(new File(newName)))
                System.out.printf("!Warning: fail to rename '%s' to '%s'\n", oldName, newName);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static class Score implements Serializable {
        String score;
        String comment;

        public Score(String score, String comment) {
            this.score = score;
            this.comment = comment;
        }

        @Override
        public String toString() {
            return "Score{" +
                    "score='" + score + '\'' +
                    ", comment='" + comment + '\'' +
                    '}';
        }
    }
}
