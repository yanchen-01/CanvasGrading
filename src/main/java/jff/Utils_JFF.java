package jff;

import helpers.Utils;
import obj.FileInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;

import static helpers.Utils_QuizSetup.ERRORS;
import static jff.Constants_JFF.*;

/**
 * Utility class for organizing jff files.
 */
public class Utils_JFF {

    public static HashSet<String> notDFA;
    protected static boolean checkDFA;
    protected static boolean isDFA;

    public static boolean handleJFF(File file, FileInfo fileInfo) {
        String studentInfo = fileInfo.getStudentInfo();
        String jffType = fileInfo.getJffType();
        if (!fileInfo.getExt().equalsIgnoreCase(".jff")) {
            ERRORS.put(studentInfo, WRONG_EXT);
            return false;
        }
        checkDFA = jffType.equals("dfa");
        jffType = jffType.equals("dfa") ? "fa" : jffType;
        String preCheckResult = preCheckMachine(file, jffType);
        if (!preCheckResult.isEmpty()) {
            ERRORS.put(studentInfo, preCheckResult);
            return false;
        }
        fileInfo.setExt(".png");
        Utils_Draw.drawJff(file, fileInfo.getFullName());
        if (checkDFA && !isDFA)
            ERRORS.put(studentInfo, NOT_DFA);
        return true;
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
            Utils.printWarning("fail to parse " + inputFile.getName(), e);
            return null;
        }
    }

    public static String preCheckMachine(File file, String machineType) {
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
        if (actual.equals(TURING_WITH_BLOCKS) && type.equals("turing"))
            type = TURING_WITH_BLOCKS;
        return !actual.equals(type) || tape.getLength() != 0;
    }

    private static boolean missingState(Document doc, String type) {
        return doc.getElementsByTagName(type).getLength() == 0;
    }

}