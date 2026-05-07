package others;

import helpers.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class CS166PA {
    static HashMap<String, String> SUBMISSIONS = new HashMap<>();
    static final String NEW_FOLDER = "organized";
    static final String JAVA_FOLDER = NEW_FOLDER + "/java";
    static final String PY_FOLDER = NEW_FOLDER + "/py";
    public static void main(String[] args) {
        Utils.makeFolder(NEW_FOLDER);
        Utils.makeFolder(JAVA_FOLDER);
        Utils.makeFolder(PY_FOLDER);
        Utils.runFunctionality(CS166PA::organizeSubs);
    }

    static void organizeSubs(Scanner in) throws Exception {
        String filename = Utils.askForParam(in, "filename (without .csv)");
        Utils.readCSV(filename, CS166PA::generateMap);
        String folder = Utils.askForParam(in, "folder name for submissions");

        Utils.goThroughFiles(CS166PA::goThroughFolder, folder);
    }

    static void generateMap(List<String[]> content) {
        for (int i = 1; i < content.size(); i++) {
            // name | subID
            String[] row = content.get(i);
            SUBMISSIONS.put(row[1], row[0]);
        }
    }

    static void goThroughFolder(File folder) throws FileNotFoundException {
        String folderName = folder.getName();
        if (folderName.contains(".DS_Store")) return;
        String studentName = SUBMISSIONS.get(folderName.replace("submission_", ""));
        Utils.goThroughFiles(file -> goThroughFile(studentName, file), folder.getAbsolutePath());
    }

    static void goThroughFile(String name, File file) {
        String filename = file.getName();
        if (filename.contains(".DS_Store")) return;

        String newName = filename.replaceAll("text_file_[0-9]+-", "");
        newName = name + "_" + newName;
        if (filename.endsWith(".java")) {
            newName = JAVA_FOLDER + "/" + newName;
        } else if (filename.endsWith(".py")){
            newName = PY_FOLDER + "/" + newName;
        } else {
            newName = NEW_FOLDER + "/" + newName;
        }

        if(!file.renameTo(new File(newName))) {
            System.out.printf("Fail to rename from %s to %s\n", filename, newName);
        }
    }
}
