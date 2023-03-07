package obj;

import helpers.Utils;

import static constants.FolderNames.SUBMISSIONS_FOLDER;
import static jff.Constants_JFF.JFF_FILES;

public class FileInfo {
    private final int studentID, questionID;
    private String ext, studentInfo, folder, jffType;

    public FileInfo(String filename) {
        String[] info = filename.split("_question_");
        String sID = Utils.removeNonDigits(info[0]);
        studentID = Integer.parseInt(sID);
        String qID = info[1].replaceAll("_.+", "");
        questionID = Integer.parseInt(qID);
        ext = filename.substring(filename.lastIndexOf('.'));
        folder = SUBMISSIONS_FOLDER + "/" + qID;
        studentInfo = "_" + questionID;
        jffType = "";
    }

    public String getJffType() {
        return jffType;
    }

    public void setJffType(String jffType) {
        this.jffType = jffType;
    }

    public void setStudentInfo(int attempt, int subID) {
        String prefix = attempt + "-" + subID;
        studentInfo = prefix + studentInfo;
    }

    public String getStudentInfo() {
        return studentInfo;
    }

    public String getFullName() {
        return folder + "/" + studentInfo + ext;
    }

    public String getFolder() {
        return folder;
    }

    public void toJffFolder() {
        folder = JFF_FILES + "/" + questionID;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public int getStudentID() {
        return studentID;
    }

    public int getQuestionID() {
        return questionID;
    }
}
