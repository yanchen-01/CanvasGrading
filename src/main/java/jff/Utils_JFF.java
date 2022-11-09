package jff;

import helpers.Utils;
import helpers.Utils_Setup;
import obj.Question;
import obj.Student;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;

import static constants.FolderNames.JFF_FOLDER;
import static constants.FolderNames.JFF_RESULTS;
import static helpers.Utils_Setup.QUESTIONS;
import static jff.Constants_JFF.*;

/**
 * Utility class for organizing jff files.
 */
public class Utils_JFF {

    public static HashSet<String> notDFA;
    protected static boolean checkDFA;
    protected static boolean isDFA;
    protected static String machineType;

    /**
     * Organize .jff submissions and draw to png.
     * For each correct file extension and correct type (except for DFA checking):
     * <ul>
     *     <li>Draw to attempt-subID_qID.png under "jffs" folder</li>
     *     <li>Also checks if it's DFA, if not, write error in jffResults folder</li>
     *     <li>Rename to attempt-subID_qID.jff</li>
     *     <li>Move to "jffs/qID" folder</li>
     * </ul>
     * if file extension or type of machine is wrong,
     * the file stays in the original folder and error is written in jffResults folder.
     *
     * @param file .jff file
     */
    public static void organize(File file) {
        try {
            String oldName = file.getName();
            if (!oldName.matches("\\D+\\d+_question_\\d+_\\d+_.*.")) return;
            // Get info - name format: nameSID_question_qID_otherInfo
            String[] info = oldName.split("_question_");
            String sID = info[0].replaceAll("\\D+", "");
            String qID = info[1].replaceAll("_.+", "");
            int studentID = Integer.parseInt(sID);
            Student student = Utils_Setup.STUDENTS.get(studentID);
            int subID = student.getSubID();
            int attempt = student.getAttempt();
            String studentInfo = String.format("%d-%d_%s", attempt, subID, qID);
            String resultFile = JFF_RESULTS + "/" + qID + "p";

            if (!oldName.endsWith(".jff")) {
                String error = studentInfo + "\n" + WRONG_EXT;
                Utils.writeToFile(resultFile, error);
                return;
            }

            // if extension is correct, pre-check the machine
            Question question = QUESTIONS.get(Integer.parseInt(qID));
            machineType = question.getJffType();
            // For DFAs, check if it's DFA when drawing
            // since not DFA is not a fatal error
            if (machineType.equals("dfa")) {
                checkDFA = true;
                machineType = "fa";
            }

            String preCheckResult = preCheckMachine(file);
            if (!preCheckResult.isEmpty()) {
                preCheckResult = studentInfo + "\n" + preCheckResult;
                Utils.writeToFile(resultFile, preCheckResult);
                return;
            }

            // if no fatal error, draw and move to corresponding folder
            Utils_Draw.drawJff(file, JFF_FOLDER + "/" + studentInfo);
            if (!isDFA) {
                Utils.writeToFile(resultFile, studentInfo + "\n" + NOT_DFA);
                notDFA.add(studentInfo);
            }

            String newName = String.format("%s/%s/%s.jff", JFF_FOLDER, qID, studentInfo);

            if (!file.renameTo(new File(newName)))
                System.out.printf("!Warning: fail to rename '%s' to '%s'\n", oldName, newName);


        } catch (Exception e) {
            System.out.printf("!Warning: file '%s' is not a legit submission file\n", file.getName());
        }
    }

    /**
     * Get the content of the child element given the tag.
     *
     * @param parent parent element
     * @param tag    tag name of the child
     * @return the content in string or null if the tag doesn't exist
     */
    public static String getContent(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0)
            return null;
        else
            return list.item(0).getTextContent();
    }

    /**
     * Get parse the .jff to a document file.
     *
     * @param inputFile the file to be parsed.
     * @return a document object that represent a .jff file.
     * <br> returns null when the file is not found.
     */
    public static Document getDoc(File inputFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            System.out.println("!Warning: fail to parse" + inputFile.getName());
            return null;
        }
    }

    private static String preCheckMachine(File file) {
        Document document = getDoc(file);
        assert document != null;
        if (missingState(document, "initial"))
            return NO_INITIAL;
        else if (missingState(document, "final"))
            return NO_FINAL;
        else if (wrongType(document, machineType))
            return WRONG_TYPE;

        return "";
    }

    /**
     * Check whether the document is the given type (not for checking DFA) or not.
     *
     * @param doc  the document to be checked.
     *             <br>Call {@link #getDoc(File) getDoc} to get the document object.
     * @param type the correct type of the machine in String.
     *             <ul>
     *             <li>For NFA/DFA, it should be "fa"; </li>
     *             <li>For PDA, it should be "pda"; </li>
     *             <li>For TM, it should be "turing". </li>
     *             </ul>
     * @return true if the document is NOT the given type.
     */
    private static boolean wrongType(Document doc, String type) {
        NodeList tape = doc.getElementsByTagName("tapes");
        String actual = getContent(doc.getDocumentElement(), "type");
        if (actual == null) return true;
        if (actual.equals(TURING_WITH_BLOCKS) && machineType.equals("turing"))
            machineType = TURING_WITH_BLOCKS;
        return !actual.equals(type) || tape.getLength() != 0;
    }

    private static boolean missingState(Document doc, String type) {
        return doc.getElementsByTagName(type).getLength() == 0;
    }

}