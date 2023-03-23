package obj;

import helpers.Utils;

public class FileInfo {
    private final int studentID, questionID;
    private String ext, studentInfo, folder, jffType, error;

    public FileInfo(String filename) {
        this(filename, "");
    }

    public FileInfo(String filename, String jffType) {
        String[] info = filename.split("_question_");
        if (info.length >= 2) {
            String sID = Utils.removeNonDigits(info[0]);
            studentID  = Integer.parseInt(sID);
            String qID = info[1].replaceAll("_.+", "");
            questionID = Integer.parseInt(qID);
            studentInfo = "_" + questionID;
        } else {
            studentID = questionID = -1;
            studentInfo = filename;
        }

        int splitIndex = filename.lastIndexOf('.');
        ext = splitIndex < 0? ".txt"
                : filename.substring(splitIndex);

        folder = "";
        this.jffType = jffType;
        error = "";
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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
        String f = folder.isEmpty()? ""
                : folder + "/";
        String filename = studentID < 0?
                studentInfo : studentInfo + ext;
        return f + filename;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String root) {
        this.folder = root + "/" + questionID;
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
