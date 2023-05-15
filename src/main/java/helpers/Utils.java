package helpers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import obj.Quiz;
import org.json.JSONObject;

import java.io.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static constants.Parameters.*;

/**
 * General util methods
 * TODO: re-arrange the other of methods - currently hard to find things...
 */
public class Utils {

    public static <T> T getObjFromURL(String url, Class<T> obj) throws JsonProcessingException {
        String data = Utils_HTTP.getData(url);
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper.readValue(data, obj);
    }


    public static <T> String getJSONString(T obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper.writeValueAsString(obj);
    }

    public static String getApiUrl(String original) {
        if (original.contains("api/v1")) return original;
        if (original.contains("speed_grader")) {
            original = original.replace("gradebook/speed_grader?assignment_id=",
                    "assignments/");
            original = original.replaceAll("&student_id.*", "");
        }
        return original.replace("courses", "api/v1/courses");
    }

    public static void checkOption(int option, int max) throws Exception {
        if (option <= 0 || option > max)
            throw new Exception("Wrong option, program terminated");
    }

    public static void printProgress(String step) {
        System.out.println("... " + step + " ...");
    }

    public static void printDoneProcess(String step) {
        System.out.println("\u2713 " + step + ".");
    }

    public static int getOption(Scanner in, String prompt) {
        Utils.printPrompt(prompt);
        int option = 0;
        if (in.hasNextInt()) {
            option = in.nextInt();
        }
        in.nextLine(); // make sure cursor move to next line.
        return option;
    }

    public static void runFunctionality(Scanner in, Functionality functionality) {
        try (in) {
            AUTH = askForParam(in, API_TOKEN);
            functionality.run(in);
        } catch (Exception e) {
            printFatalError(e);
        }
    }

    public static int lateStatus(String due, String actual, double adjustDays) {
        Date original = parseISODate(due, 0.0);
        Date newDead = parseISODate(due, adjustDays);
        Date a = parseISODate(actual, 0);
        if (a.after(newDead)) return 2;
        else if (a.after(original)) return 1;
        else return 0;
    }

    public static Date parseISODate(String date, double adjustDays) {
        Instant instant = Instant.parse(date)
                .plusSeconds((long) adjustDays * 60 * 60 * 24);
        return Date.from(instant);
    }

    public static String removeNonDigits(String input) {
        return input.replaceAll("\\D+", "");
    }

    /**
     * Print a warning message - anything wrong with individual file/folder,
     * but not serious enough to terminate the whole program.
     *
     * @param message detailed message about error (maybe a format string)
     * @param e       the exception received
     * @param args    arguments for a format string if needed
     */
    public static void printWarning(String message, Exception e, Object... args) {
        if (args != null && args.length > 0)
            message = String.format(message, args);
        System.out.println("!Warning: " + message);
        if (e != null)
            System.out.println("  causation: " + e.getMessage());
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
            printWarning("fail to write %s to %s", e, content, filename);
        }
    }

    /**
     * Ask for the private parameter.
     *
     * @param scanner scanner to take user input
     * @param param   the parameter name (Parameters.TOKEN, Parameters.USERNAME or Parameters.PASSWORD )
     */
    public static String askForParam(Scanner scanner, String param) {
        try {
            Class<?> privateParams = Class.forName("constants.PrivateParams");
            return (String) privateParams.getDeclaredField(param).get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            printPrompt(param);
            return scanner.nextLine();
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
        if (folder.exists()) return;
        if (!folder.mkdir())
            printWarning("fail to create '%s' folder.", null, folderName);
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
     * @param content  the content to write
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
        String url = QUIZ_URL + "/submissions/" + id;
        Utils_HTTP.putData(url, file.getAbsolutePath());
        deleteFile(file);
    }

    /**
     * Upload a json file for score and comments.
     *
     * @param file the json file
     */
    public static void uploadJSON(File file, Quiz quiz) {
        if (!file.isFile()) return;
        String filename = file.getName();
        String id = removeNonDigits(filename);
        String url = filename.startsWith("a") ?
                quiz.getAssignmentUrl() : quiz.getUrl();
        url = url + "/submissions/" + id;
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
            printWarning("fail to delete %s.", null, file.getAbsolutePath());
        }
    }

}