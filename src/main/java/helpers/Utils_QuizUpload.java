package helpers;

import obj.QuestionScore;
import obj.Quiz;
import obj.QuizScore;
import obj.QuizSubmission;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

import static constants.FolderNames.JSON_FOLDER;
import static grading.GradeByQuestions.quiz;


public class Utils_QuizUpload {
    static HashMap<String, QuizScore> ALL_GRADES;
    static String DEADLINE;
    static double FUDGE_POINTS, ADJUST_DAYS;

    public static void upload(Scanner in, int option) throws Exception {
        if (option == 4) {
            confirmUpload(in, "pre-generated JSONs");
            return;
        }
        if (option != 2) {
            readGradingResults(in);
            confirmUpload(in, "grading results");
        }
        if (option != 1) {
            fudgePoints(in);
            confirmUpload(in, "extra adjustment");
        }
    }

    private static void readGradingResults(Scanner in) throws Exception {
        ALL_GRADES = new HashMap<>();
        Utils.printPrompt("folder name for grading results");
        String folder = in.nextLine();
        Utils.goThroughFiles(Utils_QuizUpload::readFile, folder);
        saveJSONs();
        Utils.printDoneProcess("Finished reading grading result in " + folder);
    }

    private static void readFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            if (!file.isFile()) return;
            double full = Double.parseDouble(scanner.nextLine());
            QuestionScore questionScore = null;
            while (scanner.hasNextLine()) {
                String current = scanner.nextLine();
                if (current.matches("\\d+-\\d+_\\d+")) {
                    // format: attempt-submissionID_questionID
                    String[] info = current.split("_");
                    ALL_GRADES.computeIfAbsent(info[0], k -> new QuizScore());
                    QuizScore g = ALL_GRADES.get(info[0]);
                    questionScore = new QuestionScore(info[1], full);
                    g.addScore(questionScore);
                } else if (questionScore != null && !current.isBlank()) {
                    questionScore.addComment(current);
                }
            }
        } catch (Exception exception) {
            Utils.printWarning("something wrong when reading " + file
                    + ". Manually check if needed.", exception);
        }
    }

    private static void fudgePoints(Scanner in) {
        quiz.fetchSubmissions();

        if (ALL_GRADES == null)
            ALL_GRADES = new HashMap<>();

        Utils.printPrompt("fudge points (enter number only)");
        FUDGE_POINTS = in.nextDouble();
        Utils.printPrompt("extra allowed days if any (number only, 0 for none)");
        ADJUST_DAYS = in.nextDouble();
        in.nextLine(); // make sure cursor move to next line.

        DEADLINE = quiz.getDeadline();
        quiz.getSubmissions().forEach(Utils_QuizUpload::adjust);
        saveJSONs();
        Utils.printDoneProcess("Finished setting up extra adjustment");
    }

    private static void adjust(int studentID, QuizSubmission submission) {
        try {
            String subDate = submission.getSubDate();
            if (subDate.isEmpty() || submission.getPercentage() < 0.0) {
                System.out.println("Has ungraded submission, " +
                        "please grade all submissions before extra adjustment");
                System.exit(0);
            }
            String scoreID = submission.getPrefix();
            QuizScore score = ALL_GRADES.get(scoreID);
            if (score == null) score = new QuizScore();

            int status = Utils.lateStatus(DEADLINE, subDate, ADJUST_DAYS);
            // 0-early; 1-normal; 2-late
            if (status == 2) return;
            if (status == 0 && submission.getPercentage() > 0.2) {
                score.setFudgePoints(FUDGE_POINTS);
                score.generateAdjustment(QuizScore.COMMENT_EARLY);
            } else if (status == 1) {
                score.generateAdjustment(QuizScore.CANCEL_LATE);
            }
            score.setStudentID(studentID);
            ALL_GRADES.putIfAbsent(scoreID, score);
        } catch (Exception e) {
            Utils.printWarning(submission.toString(), e);
        }
    }

    private static void saveJSONs() {
        ALL_GRADES.forEach((scoreID, quizScore) -> {
            // scoreID format: attempt-submissionID
            String[] attempt_sID = scoreID.split("-");
            int attempt = Integer.parseInt(attempt_sID[0]);
            String sID = attempt_sID[1];
            JSONObject result = quizScore.generateJSON(attempt);
            if (result != null) {
                Utils.writeJSON(result, JSON_FOLDER + "/" + sID);
            }

            int studentID = quizScore.getStudentID();
            if (studentID != 0) {
                JSONObject adjustment = quizScore.getAdjustment();
                Utils.writeJSON(adjustment, JSON_FOLDER + "/a" + studentID);
            }
        });
    }

    public static void confirmUpload(Scanner in, String upload, Quiz quiz) throws Exception {
        Utils.printPrompt("whether ready to upload (Y/N)");
        if (!in.nextLine().equals("Y")) {
            String note = !upload.contains("grading") ? ""
                    : "Note that you need to upload grading before setting extra adjustment";
            System.out.println("OK, rerun to upload when you are ready. " + note);
            System.exit(0);
        } else {
            uploadResults(upload, quiz);
            System.out.println("Please double-check on Canvas.");
        }
    }

    public static void uploadResults(String result, Quiz quiz) throws Exception {
        Utils.printProgress("uploading " + result + " results");
        Utils.goThroughFiles(file -> Utils.uploadJSON(file, quiz), JSON_FOLDER);
        Utils.printDoneProcess(result + " results uploaded");
    }


}
