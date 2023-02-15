package helpers;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.List;
import java.util.Scanner;

import static constants.Parameters.*;

/**
 * General util methods
 */
public class Utils {

    public static String removeNonDigits(String input) {
        return input.replaceAll("\\D+", "");
    }

    /**
     * Print a warning message - anything wrong with individual file/folder,
     * but not serious enough to terminate the whole program.
     *
     * @param message detailed message about error (maybe a format string)
     * @param args    arguments for a format string if needed
     */
    public static void printWarning(String message, Object... args) {
        if (args != null && args.length > 0)
            message = String.format(message, args);
        System.out.println("!Warning: " + message);
    }

    /**
     * Print the error that terminates the whole program.
     *
     * @param e the exception that crashed the program
     */
    public static void printFatalError(Exception e) {
        System.out.println("Terminated: " + e.getMessage());
        e.printStackTrace();
    }

    /**
     * Write content to the file (append if exist).
     *
     * @param filename name of the file (without extension)
     * @param content  content to write (WITH line breaker at the end)
     */
    public static void writeToFile(String filename, String content) {
        filename = filename.concat(".txt");
        try (FileWriter myWriter = new FileWriter(filename, true)) {
            myWriter.write(content);
        } catch (IOException e) {
            printWarning("fail to write %s to %s", content, filename);
        }
    }

    /**
     * Ask for global parameters (Auth token, URL, etc.) needed.
     *
     * @param scanner scanner to take user input
     */
    public static void askForParameters(Scanner scanner) {
        askForAuth(scanner);
        printPrompt("assignment URL (start with https, do NOT end with /)");
        ASSIGNMENT_URL = scanner.nextLine();
        if (ASSIGNMENT_URL.contains("speed")) {
            ASSIGNMENT_URL = ASSIGNMENT_URL.replace
                    ("gradebook/speed_grader?assignment_id=", "assignments/");
            ASSIGNMENT_URL = ASSIGNMENT_URL.replaceAll("&student_id.*", "");
        }
        API_URL = ASSIGNMENT_URL.replace("courses", "api/v1/courses");

//        printPrompt("folder name if contains upload questions (for JFF question, " +
//                "please name the folder as 'jff') or N if not");
//        SUBMISSION_FOLDER = scanner.nextLine();
    }

    /**
     * Ask for the Auth token.
     *
     * @param scanner scanner to take user input
     */
    public static void askForAuth(Scanner scanner) {
        try {
            Class<?> privateParams = Class.forName("constants.PrivateParams");
            AUTH = (String) privateParams.getDeclaredField("AUTH").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            printPrompt("TOKEN");
            AUTH = scanner.nextLine();
        }
    }

    /**
     * Prints a prompt ask user to enter the parameter needed.
     *
     * @param param what to enter
     */
    public static void printPrompt(String param) {
        System.out.printf("Enter %s: \n>> ", param);
    }

    /**
     * Make a new folder.
     *
     * @param folderName the name of the folder
     */
    public static void makeFolder(String folderName) {
        File folder = new File(folderName);
        if (!folder.mkdir())
            printWarning("fail to create '%s' folder.", folderName);
    }

    /**
     * Read a CSV file.
     *
     * @param filename the name of the file (without extension)
     * @param parser   a function to organize the content
     * @throws Exception if anything wrong
     */
    public static void readCSV(String filename, ParseCSV parser) throws Exception {
        try (FileReader input = new FileReader(filename + ".csv");
             CSVReader reader = new CSVReader(input)) {
            List<String[]> contents = reader.readAll();
            parser.parse(contents);
        } catch (Exception e) {
            throw new Exception(Utils.exceptionMsg("reading " + filename, e));
        }
    }

    /**
     * Write a CSV file
     *
     * @param filename the name of the file (without extension)
     * @param content the content to write
     * @throws Exception if anything wrong
     */
    public static void writeCSV(String filename, List<String[]> content) throws Exception {
        try (FileWriter out = new FileWriter(filename + ".csv");
             CSVWriter writer = new CSVWriter(out)) {
            writer.writeAll(content);
        } catch (Exception e) {
            throw new Exception(exceptionMsg("writing file " + filename, e));
        }
    }

    /**
     * An exception message indicating which step wrong with the exception.
     *
     * @param step the step that causes the exception
     * @param e    the exception caught
     * @return an exception message indicating which step wrong with the exception.
     */
    public static String exceptionMsg(String step, Exception e) {
        return String.format("Something wrong when %s...debug yourself...\n%s",
                step, e.getMessage());
    }

    /**
     * Save object to file.
     *
     * @param filename name of the file to be saved
     * @param object   object needs to be saved
     * @param <T>      A Serializable parameter type
     */
    public static <T extends Serializable> void saveObject(String filename, T object) {
        try (FileOutputStream fos = new FileOutputStream(filename);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            // write object to file
            oos.writeObject(object);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load an object from a file.
     *
     * @param filename name of the file
     * @return the resulting object
     */
    public static Object getObjectFromFile(String filename) {
        try (FileInputStream fileIn = new FileInputStream(filename);
             ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
            return objectIn.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Write a json file that stores score and comment for 1 submission.
     *
     * @param question A json object that contains scores and comments
     * @param filename the output filename (without extension)
     * @param attempt  the number of attempts
     */
    public static void writeScoreAndCommentJSON(JSONObject question, String filename, int attempt) {
        JSONObject submission = new JSONObject();
        submission.put("attempt", attempt);
        submission.put("questions", question);

        JSONArray array = new JSONArray();
        array.put(submission);

        JSONObject result = new JSONObject();
        result.put("quiz_submissions", array);
        writeJSON(result, filename);
    }

    /**
     * Write a json object to a file
     *
     * @param object   the json object
     * @param filename the name of the file (without extension)
     */
    public static void writeJSON(JSONObject object, String filename) {
        try (PrintWriter writer = new PrintWriter(filename + ".json")) {
            object.write(writer);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Go through the files in a folder using the given operation.
     *
     * @param op       the operation on each file
     * @param pathname the pathname of the folder
     * @throws FileNotFoundException if the folder doesn't exist
     */
    public static void goThroughFiles(FileOp op, String pathname) throws FileNotFoundException {
        File folder = new File(pathname);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            throw new FileNotFoundException("'" + pathname + "' folder doesn't exist!");
        for (File file : listOfFiles)
            op.operate(file);
    }

    /**
     * Upload a json file for score and comments.
     *
     * @param file the json file
     */
    public static void uploadJSON(File file) {
        if (!file.isFile()) return;
        String id = file.getName().replace(".json", "");
        String url = API_URL + "/submissions/" + id;
        Utils_HTTP.putData(url, file.getAbsolutePath());
        deleteFile(file);
    }

    /**
     * Delete a file (print a message of fail to delete).
     *
     * @param file file to be deleted
     */
    public static void deleteFile(File file) {
        if (!file.delete()) {
            printWarning("fail to delete %s.", file.getAbsolutePath());
        }
    }

}