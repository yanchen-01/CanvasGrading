package obj;

import helpers.Utils;
import helpers.Utils_HTML;
import helpers.Utils_HTTP;
import jff.JffQuestion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

import static constants.JsonKeywords.*;
import static constants.Parameters.API;
import static constants.Parameters.ASSIGNMENTS;

public class Quiz {
    private final HashMap<Integer, QuizSubmission> submissions;
    private final HashMap<Integer, Question> questions;
    private final HashMap<Integer, QuestionSet> shortAnswers;
    private final HashMap<Integer, JffQuestion> jffQuestions;
    private final HashSet<Integer> uploadQuestions;
    private final String deadline, url, assignmentUrl;
    private final int numOfQuestions;
    private final double total;

    public Quiz(String oldUrl) throws Exception {
        submissions = new HashMap<>();
        questions = new HashMap<>();
        shortAnswers = new HashMap<>();
        uploadQuestions = new HashSet<>();
        jffQuestions = new HashMap<>();

        url = generateUrl(oldUrl, true);

        JSONObject quizJSON = Utils_HTTP.getJSON(url);
        if (oldUrl.contains("quizzes"))
            oldUrl = quizJSON.getString(SPEED_GRADER);
        assignmentUrl = generateUrl(oldUrl, false);
        total = quizJSON.getDouble(POINTS);
        deadline = quizJSON.getString(DUE_AT);
        String title = quizJSON.getString("title");
        numOfQuestions = title.matches(ASSIGNMENTS) ?
                quizJSON.getInt(NUM_OF_QS) :
                fetchNumOfQuestions(url + "/statistics");
    }

    private int fetchNumOfQuestions(String url) {
        String temp = Utils_HTTP.getData(url);
        JSONObject statistics = new JSONObject(temp);
        JSONArray array = statistics.getJSONArray("quiz_statistics");
        JSONArray questions = array.getJSONObject(0)
                .getJSONArray("question_statistics");
        return questions.length();
    }

    public HashMap<Integer, JffQuestion> getJffQuestions() {
        return jffQuestions;
    }

    public HashSet<Integer> getUploadQuestions() {
        return uploadQuestions;
    }

    public HashMap<Integer, QuizSubmission> getSubmissions() {
        return submissions;
    }

    public HashMap<Integer, Question> getQuestions() {
        return questions;
    }

    public HashMap<Integer, QuestionSet> getShortAnswers() {
        return shortAnswers;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getUrl() {
        return url;
    }

    public String getAssignmentUrl() {
        return assignmentUrl;
    }

    public void fetchSubmissionsAndQuestions() {
        fetch(true);
    }

    public void fetchSubmissions() {
        fetch(false);
    }

    private String generateUrl(String oldUrl, boolean quiz) throws Exception {
        oldUrl = Utils.getApiUrl(oldUrl);
        if (quiz && oldUrl.contains("quizzes"))
            return oldUrl;
        else if (oldUrl.contains("speed_grader")) {
            String temp = oldUrl.replace("gradebook/speed_grader?assignment_id=",
                    "assignments/");
            temp = temp.replaceAll("&student_id.*", "");
            if (!quiz) return temp;
            return generateQuizUrl(temp);
        } else throw new Exception("Wrong url");
    }

    private String generateQuizUrl(String assignmentUrl) throws Exception {
        try {
            JSONObject json = Utils_HTTP.getJSON(assignmentUrl);
            int quizID = json.getInt(QUIZ_ID);
            assignmentUrl = assignmentUrl.replaceAll("assignments/.*", "quizzes/");
            return assignmentUrl + quizID;
        } catch (JSONException e) {
            throw new Exception(Utils.exceptionMsg("get quiz URL - not a Quiz!", e));
        }
    }

    private void fetch(boolean both) {
        String fetch = both ? "questions" : "submissions";
        Utils.printProgress("fetching " + fetch);

        String subURL = url + "/submissions?per_page=200";
        JSONObject json = Utils_HTTP.getJSON(subURL);
        JSONArray submissions = json.getJSONArray("quiz_submissions");
        for (int i = 0; i < submissions.length(); i++) {
            JSONObject current = submissions.getJSONObject(i);
            if (current.getString(STATUS).equals("untaken"))
                continue;
            int subID = current.getInt(ID);
            addStudent(current, subID);
            if (both && questions.size() < numOfQuestions)
                addQuestions(subID);
        }
        Utils.printDoneProcess(fetch + " fetched");
    }

    private void addStudent(JSONObject submission, int subID) {
        int student_id = submission.getInt(USER_ID);
        // Set submission id & number of attempts
        int attempt = submission.getInt("attempt");
        QuizSubmission s = new QuizSubmission(subID, attempt);
        if (submission.getString(STATUS).equals("complete")) {
            s.setSubDate(submission.getString(SUBMIT_DATE));
            double score = submission.getDouble(SCORE);
            s.setPercentage(score / total);
        }
        submissions.put(student_id, s);
    }

    private void addQuestions(int sub_id) {
        String url = String.format("%s/quiz_submissions/%d/questions",
                API, sub_id);
        JSONObject json = Utils_HTTP.getJSON(url);
        JSONArray qs = json.getJSONArray("quiz_submission_questions");
        for (int i = 0; i < qs.length(); i++) {
            JSONObject current = qs.getJSONObject(i);
            String type = current.getString(TYPE);
            int id = current.getInt(ID);
            if (type.equals(TEXT_ONLY)
                    || questions.containsKey(id))
                continue;

            Question question = new Question(id, type);
            // Other info only matters to essay or file upload questions
            if (type.equals(ESSAY)
                    || type.equals(UPLOAD)) {
                String content = current.getString(CONTENT);
                if (content.contains(".jff") && type.equals(UPLOAD)) {
                    question = new JffQuestion(id);
                    jffQuestions.put(id, (JffQuestion) question);
                }
                question.setContent(content);
                updateQuestion(current);
                int groupId = current.getInt(ID);
                String name = current.getString(Q_NAME);
                double score = current.getDouble(POINTS);
                shortAnswers.computeIfAbsent(groupId, k -> new QuestionSet(name, score));
                QuestionSet set = shortAnswers.get(groupId);
                set.add(question);

                if (type.equals(UPLOAD))
                    uploadQuestions.add(id);
            }

            this.questions.put(id, question);
            if (this.questions.size() >= numOfQuestions) break;
        }
    }

    private void updateQuestion(JSONObject q) {
        Object groupID = q.get(Q_GROUP_ID);
        if (!(groupID instanceof Integer)) {
            q.put(POINTS, -1.0);
            return;
        }

        String url = this.url + "/groups/" + groupID;
        JSONObject group = Utils_HTTP.getJSON(url);
        String name = group.getString(NAME);
        double score = group.getDouble(GROUP_POINTS);
        int pickCount = group.getInt(PICK_COUNT);
        if (pickCount != 1) {
            String content = q.getString(CONTENT);
            int qID = q.getInt(ID);
            String keyword = Utils_HTML.getBoldText(content, String.valueOf(qID));
            name = name + "-" + keyword;
            groupID = qID;
        }
        q.put(ID, groupID);
        q.put(Q_NAME, name);
        q.put(POINTS, score);
    }
}