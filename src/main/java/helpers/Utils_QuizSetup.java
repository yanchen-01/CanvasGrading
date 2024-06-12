package helpers;

import jff.JffQuestion;
import jff.Utils_JFF;
import obj.*;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static constants.FolderNames.*;
import static constants.JsonKeywords.*;
import static constants.Parameters.REPORT_NAME;
import static jff.Constants_JFF.*;

public class Utils_QuizSetup {
    static HashMap<String, String> FILES, ERRORS;
    static List<String[]> CONTENTS;
    static String[] HEADERS;
    static StringBuilder BY_STUDENTS_CONTENT;
    static Quiz quiz;

    public static void setQuiz(Quiz quiz) {
        Utils_QuizSetup.quiz = quiz;
    }

    public static void organizeSubmissions(Scanner in) throws FileNotFoundException {
        Utils.makeFolder(SUBMISSIONS_FOLDER);
        for (int id : quiz.getUploadQuestions())
            Utils.makeFolder(SUBMISSIONS_FOLDER + "/" + id);
        FILES = new HashMap<>();
        ERRORS = new HashMap<>();
        Utils.printProgress("*Upload question detected*");
        Utils.printPrompt("folder name for submissions");
        String folder = in.nextLine();
        Utils.printProgress("Organizing submitted files");
        Utils.renameFiles(folder, Utils_QuizSetup::rename);
        Utils.printDoneProcess("Submission files organized");
    }

    public static void downloadReport(Scanner in) {
        try {
            Utils.printPrompt("whether the submission report is downloaded (Y/N)");
            if (in.nextLine().equals("Y")) {
                Utils.printPrompt("filename of the downloaded report (without .csv)");
                REPORT_NAME = in.nextLine();
                return;
            }
            Utils.printProgress("fetching submission report");

            String url = quiz.getUrl() + "/reports?quiz_report[report_type]=student_analysis";
            String data = Utils_HTTP.httpRequest("POST", url, "");
            JSONObject object = new JSONObject(data);
            int id = object.getInt("id");
            url = quiz.getUrl() + "/reports/" + id;
            do {
                data = Utils_HTTP.getData(url);
                object = new JSONObject(data);
            } while (!object.has("file"));

            JSONObject file = object.getJSONObject("file");
            String fileURL = file.getString("url");

            Utils_HTTP.getFile(fileURL, REPORT_NAME.concat(".csv"));

            Utils.printDoneProcess(REPORT_NAME + ".csv downloaded");
        } catch (Exception e) {
            Utils.printWarning("Fail to fetch the file. " +
                    "Please manually download the report (Quiz Statistics -> Student Analysis).", e);
            Utils.printPrompt("filename of the downloaded submission report (without .csv)");
            REPORT_NAME = in.nextLine();
        }
    }

    public static void readSubmissions(List<String[]> contents) {
        CONTENTS = contents;
        HEADERS = CONTENTS.remove(0);
        BY_STUDENTS_CONTENT = new StringBuilder();
        for (String[] row : CONTENTS) {
            readCurrent(row);
        }
        Utils.printDoneProcess("submissions all read");
    }

    public static void generateFiles() {
        generateHTMLs();
        saveJFFObjects();
        Utils.printDoneProcess("All files needed generated");
    }

    private static String rename (File file) {
        if (!file.isFile()) return null;
        String oldName = file.getName();
        // name format: nameSID_question_qID_otherInfo
        if (oldName.contains("student_test")
                || !oldName.matches("\\D+\\d+_question_\\d+_.*"))
            return null;
        try {
            FileInfo fileInfo = new FileInfo(oldName);
            fileInfo.setFolder(SUBMISSIONS_FOLDER);
            QuizSubmission submission = quiz.getSubmissions().get(fileInfo.getStudentID());
            int subID = submission.getSubmissionID();
            int attempt = submission.getAttempt();
            fileInfo.setStudentInfo(attempt, subID);

            Question q = quiz.getQuestions().get(fileInfo.getQuestionID());

            if (q instanceof JffQuestion j) {
                Utils.makeFolder(JFF_FILES);
                fileInfo.setJffType(j.getJffType());
                Utils_JFF.handleJFF(file, fileInfo);
            }

            String studentInfo = fileInfo.getStudentInfo();
            FILES.put(studentInfo, fileInfo.getFullName());

            String error = fileInfo.getError();
            if (fileInfo.isJFF() && (error.isEmpty() || error.equals(NOT_DFA))) {
                fileInfo.setFolder(JFF_FILES);
                Utils.makeFolder(fileInfo.getFolder());
                fileInfo.setExt(".jff");
            }
            if (!fileInfo.getError().isEmpty())
                ERRORS.put(studentInfo, error);
            return fileInfo.getFullName();

        } catch (Exception e) {
            Utils.printWarning("sth wrong for file '%s'", e, oldName);
            return null;
        }
    }

    private static void readCurrent(String[] current) {
        // name - first column; id - second column.
        String studentName = current[0];
        try {
            int sID = Integer.parseInt(current[1]);
            QuizSubmission submission = quiz.getSubmissions().get(sID);
            submission.setStudentName(studentName);
            String url = quiz.getUrl().replace("/api/v1", "")
                    + "/submissions/" + submission.getSubmissionID();
            BY_STUDENTS_CONTENT.append(String.format("<p>%s</p>",
                    Utils_HTML.getHTMLHref(url, submission, true)));

            QuizScore quizScore = new QuizScore();
            for (int i = 2; i < HEADERS.length - 1; i++) {
                String currentQ = HEADERS[i];
                // If not the latest attempt, go to next row
                if (currentQ.equals("attempt")
                        && !current[i].equals(String.valueOf(submission.getAttempt())))
                    break;
                // If not a question column, continue to the next column
                if (!currentQ.contains(":")) continue;
                // pt is empty means current student didn't get this version of the question
                String pt = current[i + 1];
                if (pt.isEmpty()) continue;
                QuestionScore score = handleCurrentAnswer(submission, current[i], pt, i);
                if (score != null)
                    quizScore.addScore(score);
                i++;
            }

            JSONObject result = quizScore.generateJSON(submission.getAttempt());
            if (result != null)
                Utils.writeJSON(result, JSON_FOLDER + "/" + submission.getSubmissionID());


        } catch (Exception e) {
            Utils.printWarning("sth wrong when reading %s's answers", e, studentName);
        }
    }

    private static QuestionScore handleCurrentAnswer(QuizSubmission submission, String answer, String pts, int i) {
        String questionText = HEADERS[i];
        String total = HEADERS[i + 1];
        try {
            double pt = Double.parseDouble(pts);
            double t = Double.parseDouble(total);
            String qID_string = questionText.substring(0, questionText.indexOf(":"));
            int qID = Integer.parseInt(qID_string);

            Question question = quiz.getQuestions().get(qID);
            if (question == null) return null;
            String type = question.getType();
            if (total.equals("0")
                    || (answer.isBlank() && pt == 0)
                    || (type.equals(MC) && pt != t && pt != 0.0))
                return new QuestionScore(qID_string, 0.0);
            else if (pt == 0) // skip the graded
                handleShortAnswer(question, submission, i, answer);
            return null;

        } catch (Exception e) {
            Utils.printWarning("sth wrong when reading %s' answer for %s",
                    e, submission.getStudentName(), questionText);
            return null;
        }
    }

    private static void handleShortAnswer(Question question, QuizSubmission submission, int i, String answer) {
        String type = question.getType();
        if (!type.equals(ESSAY) && !type.equals(UPLOAD))
            return;

        int qID = question.getId();
        QuestionSet set = quiz.getShortAnswers().get(qID);
        JffQuestion jff = quiz.getJffQuestions().get(qID);
        double total = Double.parseDouble(HEADERS[i + 1]);

        if (set != null && set.getScore() < 0)
            set.setScore(total);

        if (jff != null) jff.setTotal(total);

        if (type.equals(UPLOAD))
            answer = getUploadAnswer(submission, qID);

        question.addStudentAnswer(new Answer(submission, answer));
    }

    private static String getUploadAnswer(QuizSubmission submission, int qID) {
        String result = "";
        String s = submission.getPrefix() + "_" + qID;
        String error = ERRORS.get(s);
        if (error != null) {
            JSONObject attributes = new JSONObject();
            attributes.put("style", "color:red");
            result = Utils_HTML.getHTMLElement("span", attributes, error);
        }

        String filename = "../" + FILES.get(s);
        if (!filename.endsWith(".jff")) {
            String embedFile = Utils_HTML.getHTMLEmbed(filename, 350);
            result = result.isEmpty() ? embedFile
                    : result + "<br>" + embedFile;
        }

        return result;
    }

    private static void generateHTMLs() {
        StringBuilder indexContent = new StringBuilder("<body>");
        quiz.getShortAnswers().forEach((id, qs) -> {
            String summary = qs.getName();
            Utils_HTML.writeQuestionHTML(summary, qs);
            String url = String.format("%s/%s.html", GRADING_FOLDER, summary);
            String body = Utils_HTML.getHTMLHref(url, summary, true);
            indexContent.append(Utils_HTML.getHTMLParagraph(body));
        });
        String byStudents = Utils_HTML.getHTMLHref(BY_STUDENT + ".html");
        indexContent.append(Utils_HTML.getHTMLParagraph(byStudents));
        indexContent.append("</body>\n");
        // Write submissions_by_students.html & index
        Utils_HTML.writeToHTMLFile(BY_STUDENT, BY_STUDENTS_CONTENT.toString());
        Utils_HTML.writeToHTMLFile(INDEX, indexContent.toString());
    }

    private static void saveJFFObjects() {
        if (ERRORS != null && !ERRORS.isEmpty())
            Utils.saveObject(JFF_ERRORS, ERRORS);
        HashMap<Integer, JffQuestion> jffQs = quiz.getJffQuestions();
        if (!jffQs.isEmpty())
            Utils.saveObject(JFF_QUESTIONS, jffQs);
    }

}
