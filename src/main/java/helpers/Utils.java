package helpers;

import com.opencsv.CSVReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.List;
import java.util.Scanner;

import static constants.Parameters.*;

public class Utils {

    /**
     * Ask for global parameters (Auth token, URL, etc.) needed.
     *
     * @param askForClass true if need to know the class number
     */
    public static void askForParameters(boolean askForClass) {
        Scanner scanner = new Scanner(System.in);
        try {
            Class<?> privateParams = Class.forName("constants.PrivateParams");
            AUTH = (String) privateParams.getDeclaredField("AUTH").get(null);
            CLASS = (String) privateParams.getDeclaredField("CLASS").get(null);
            CLASS = CLASS.contains("CS")? CLASS : "CS" + CLASS;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            if (AUTH.isEmpty()) {
                printPrompt("TOKEN");
                AUTH = scanner.nextLine();
            }
            if (askForClass && CLASS.isEmpty()) {
                printPrompt("Enter class name (154, etc.)");
                CLASS = "CS" + scanner.nextLine();
            }
        }
        printPrompt("Enter assignment URL (start with https, do NOT end with /)");
        API_URL = scanner.nextLine();
        API_URL = API_URL.replace("courses", "api/v1/courses");
    }

    /**
     * Prints a prompt ask user to enter the parameter needed.
     *
     * @param param what to enter
     */
    public static void printPrompt (String param){
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
            System.out.printf("!Warning: fail to create '%s' folder. " +
                    "But don't freak out since it may already exists\n", folderName);
    }

    /**
     * Read a CSV file.
     *
     * @param filename the name of the file (WITH extension)
     * @param parser   a function to organize the content
     * @throws Exception if anything wrong
     */
    public static void readCSV(String filename, ParseCSV parser) throws Exception {
        try (FileReader input = new FileReader(filename);
             CSVReader reader = new CSVReader(input)) {
            List<String[]> contents = reader.readAll();
            parser.parse(contents);
        } catch (Exception e) {
            throw new Exception(Utils.exceptionMsg("reading " + filename, e));
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
     */
    public static void writeScoreAndCommentJSON(JSONObject question, String filename) {
        JSONObject submission = new JSONObject();
        submission.put("attempt", 1);
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
     * @throws Exception if the folder doesn't exist
     */
    public static void goThroughFiles(FileOp op, String pathname) throws Exception {
        File folder = new File(pathname);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            throw new Exception("'" + pathname + "' folder doesn't exist!");
        for (File file : listOfFiles) {
            if (file.isFile()) {
                op.operate(file);
            }
        }
    }

    /**
     * Upload a json file for score and comments.
     *
     * @param file the json file
     */
    public static void uploadJSON(File file) {
        String id = file.getName().replace(".json", "");
        String url = API_URL + "/submissions/" + id;
        Utils_HTTP.putData(url, file.getAbsolutePath());
        assert file.delete();
    }

}