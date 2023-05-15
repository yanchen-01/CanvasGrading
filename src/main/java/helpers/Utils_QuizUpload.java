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


public class Utils_QuizUpload {
    static HashMap<String, QuizScore> ALL_GRADES;
    static String DEADLINE, COMMENT_GENERAL, COMMENT_EARLY;
    static double FUDGE_GENERAL, FUDGE_EARLY, ADJUST_DAYS;

    public static void readGradingResults(Scanner in) throws Exception {
        ALL_GRADES = new HashMap<>();
        Utils.printPrompt("folder name for grading results");
        String folder = in.nextLine();
        Utils.goThroughFiles(Utils_QuizUpload::readFile, folder);
        saveJSONs();
        Utils.printDoneProcess("Finished reading grading result in " + folder);
    }

    private static void readFile(File file) {
        String current = "";
        try (Scanner scanner = new Scanner(file)) {
            if (!file.isFile()) return;
            double full = Double.parseDouble(scanner.nextLine());
            QuestionScore questionScore = null;
            while (scanner.hasNextLine()) {
                current = scanner.nextLine();
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
                    + ". Manually check if needed. " + current, exception);
        }
    }

    public static void fudgePoints(Scanner in, Quiz quiz) throws Exception {
        ADJUST_DAYS = -1.0;
        FUDGE_GENERAL = FUDGE_EARLY = 0.0;
        COMMENT_EARLY = COMMENT_GENERAL = "";
        if (ALL_GRADES == null)
            ALL_GRADES = new HashMap<>();
        int option = Utils.getOption(in, """
                one option (1, 2, or 3):
                1. General extra credit;
                2. Extra credit for submitting early;
                3. Both of above""");

        Utils.checkOption(option, 3);
        if (option != 2) {
            Utils.printPrompt("general fudge points (enter number only)");
            FUDGE_GENERAL = in.nextDouble();
            in.nextLine(); // make sure cursor move to next line.
            Utils.printPrompt("what for (don't include 'for ' at the beginning)");
            String temp = in.nextLine();
            COMMENT_GENERAL = String.format("+%.2f points for %s", FUDGE_GENERAL, temp);
        }

        if (option != 1) {
            Utils.printPrompt("early-submission fudge points (enter number only)");
            FUDGE_EARLY = in.nextDouble();
            Utils.printPrompt("extra allowed days if any (number only, 0 for none)");
            ADJUST_DAYS = in.nextDouble();
            COMMENT_EARLY = String.format("+%.2f points for %s", FUDGE_EARLY,
                    "submitting before the original deadline.");
            in.nextLine(); // make sure cursor move to next line.
        }

        quiz.fetchSubmissions();
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

            if (ADJUST_DAYS < 0) { // only fudge general, no other adjustment
                score.setFudgePoints(FUDGE_GENERAL);
                score.leaveComment(COMMENT_GENERAL);
            } else {
                int status = Utils.lateStatus(DEADLINE, subDate, ADJUST_DAYS);
                adjustEarly(score, submission, status);
            }
            score.setStudentID(studentID);
            ALL_GRADES.putIfAbsent(scoreID, score);
        } catch (Exception e) {
            Utils.printWarning(submission.toString(), e);
        }
    }

    private static void adjustEarly(QuizScore score, QuizSubmission submission, int status) {
        // 0-early; 1-normal; 2-late.
        double pts = FUDGE_GENERAL;
        String comment = COMMENT_GENERAL.isEmpty() ?
                "" : COMMENT_GENERAL;
        if (status == 0 && submission.getPercentage() > 0.2) {
            String and = comment.isEmpty() ? "" : " and ";
            comment += String.format("%s%s", and, COMMENT_EARLY);
            pts += FUDGE_EARLY;
        } else if (status == 1) {
            score.cancelLate();
        }
        score.setFudgePoints(pts);
        score.leaveComment(comment);
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

            JSONObject adjustment = quizScore.getAdjustment();
            if (!adjustment.isEmpty()) {
                int studentID = quizScore.getStudentID();
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