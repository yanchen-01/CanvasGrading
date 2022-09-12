package helpers;

import obj.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import static constants.FolderNames.GRADING_FOLDER;
import static constants.FolderNames.JSON_FOLDER;

public class Utils_Setup {

    public static HashMap<Integer, Student> STUDENTS;
    public static HashMap<Integer, Question> QUESTIONS = new HashMap<>();
    public static HashMap<String, QuestionSet> SHORT_ANSWERS = new HashMap<>();
    static List<String[]> CONTENTS;

    /**
     * Set the submission id and number of attempts
     * to the student map and generate a html
     * that contains all students' submission links.
     *
     * @param className        the name of the class (CS154, etc.)
     * @param submissions_json json response from submissions
     * @throws Exception when there's any student not found
     */
    public static void setStudents(String className, String submissions_json) throws Exception {
        @SuppressWarnings("unchecked")
        HashMap<Integer, Student> s = (HashMap<Integer, Student>) Utils.getObjectFromFile(className);
        STUDENTS = s;
        JSONObject json = new JSONObject(submissions_json);
        JSONArray submissions = json.getJSONArray("quiz_submissions");
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < submissions.length(); i++) {
            JSONObject current = submissions.getJSONObject(i);

            int student_id = current.getInt("user_id");
            Student student = STUDENTS.get(student_id);
            if (student == null)
                throw new Exception(student_id + " not found. " +
                        "Please make sure student hash map is up to date. ");

            // Set submission id & number of attempts
            int sub_id = current.getInt("id");
            int attempt = current.getInt("attempt");
            student.setInfo(sub_id, attempt);

            // Append url
            String url = current.getString("html_url");
            content.append(String.format(
                    "<p><a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a></p>",
                    url, student));
        }
        Utils_HTML.writeToHTMLFile("submissions_by_students", content.toString());
    }

    /**
     * Set a map of questions (key is id) and a map of short answers (key is name of question).
     * TODO: update for same question with different versions
     *
     * @param questions_json json response from questions
     */
    public static void setQuestions(String questions_json) {
        JSONArray qs = new JSONArray(questions_json);
        for (int i = 0; i < qs.length(); i++) {
            JSONObject current = qs.getJSONObject(i);
            int id = current.getInt("id");
            String type = current.getString("question_type");
            Question question = new Question(id, type);
            // total score, name and content only matter for short answers
            if (type.equals("essay_question")) {
                double score = current.getDouble("points_possible");
                String name = current.getString("question_name");
                String content = current.getString("question_text");
                question.setContent(content);
                QuestionSet set = new QuestionSet(name, score);
                set.add(question);
                SHORT_ANSWERS.put(name, set);
            }
            QUESTIONS.put(id, question);
        }
    }

    /**
     * Read the submission cvs file, then
     * <ol>
     *     <li>Generate json files for scores of MC and unanswered. </li>
     *     <li>Generate a hashmap for short answers (key = summary of the question ,
     *     value = a question set contains different versions of the question)</li>
     * </ol>
     *
     * @param contents content of the csv file
     */
    public static void readSubmissions(List<String[]> contents) {
        CONTENTS = contents;
        String[] headers = CONTENTS.remove(0);
        for (String[] row : CONTENTS) {
            Student student = STUDENTS.get(Integer.parseInt(row[1]));
            JSONObject questionJSON = new JSONObject();
            for (int i = 2; i < headers.length - 1; i++) {
                String currentQ = headers[i];
                // If not the latest attempt, break inner loop and go to next row
                if (currentQ.equals("attempt")
                        && !row[i].equals(student.getAttempt() + ""))
                    break;
                // If not a question column, continue to the next column
                if (!currentQ.contains(":")) continue;
                // if this is a question column, get the answer
                String qID_string = currentQ.substring(0, currentQ.indexOf(":"));
                int qID = Integer.parseInt(qID_string);
                Question question = QUESTIONS.get(qID);
                String type = question.getType();
                String studentAnswer = row[i];
                String pt = row[i + 1];
                // pt is empty means current student didn't get this version of the question
                // so continue to the next column
                if (pt.isEmpty()) continue;

                double score = Double.parseDouble(pt);
                if (studentAnswer.isBlank() ||
                        (type.equals("multiple_answers_question") && score != 1.0 && score != 0.0))
                    questionJSON.put(qID_string, new Score(qID_string, 0).generateJSON());
                else if (type.equals("essay_question"))
                    question.addStudentAnswer(new Answer(student, studentAnswer));

                i++;
            }

            if (!questionJSON.isEmpty())
                Utils.writeScoreAndCommentJSON
                        (questionJSON, JSON_FOLDER + "/" + student.getSubID(), student.getAttempt());
        }

    }

    /**
     * Generate html files needed.
     * <ul>
     *     <li>HTML for Each question (with student submissions) is in "results" folder</li>
     *     <li>index.html that links to each question html and submissions_by_student.html is
     *     in the root folder</li>
     * </ul>
     */
    public static void generateHTMLs() {
        StringBuilder indexContent = new StringBuilder("<body>");
        SHORT_ANSWERS.forEach((summary, qs) -> {
            writeQuestionHTML(summary, qs);
            indexContent.append(String.format("<p><a href=\"%s/%2$s.html\">%2$s</a></p>\n",
                    GRADING_FOLDER, summary));
        });
        indexContent.append(String.format("""
                <p><a href="%1$s">%1$s</a></p>
                </body>""", "submissions_by_students.html"));
        Utils_HTML.writeToHTMLFile("index", indexContent.toString());
    }

    private static void writeQuestionHTML(String summary, QuestionSet qs) {
        StringBuilder content = new StringBuilder(String.format("""
                <head>
                    <meta charset="UTF-8">
                    <script src="../script.js"></script>
                    <title>%1$s</title>
                </head>
                <body>
                    <p><a href="../index.html">back to index</a></p>
                    <p id="qNum">%s</p>
                    <p id="score">%.1f</p>
                    <input type="file" id="grading" accept=".txt" style="display:none">
                    <button onclick="load('grading', loadSaved)">Load saved results</button>
                    <input type="file" id="rubrics" accept=".txt" style="display:none">
                    <button onclick="load('rubrics', loadRubric)">Load saved rubrics</button>
                """, summary, qs.getScore()));
        for (Question q : qs) {
            int id = q.getId();
            String questionContent = Utils_HTML.parseToHtmlParagraph(q.getContent());

            content.append(String.format("""
                        <h3>%d<br>%s</h3>
                    """, id, questionContent.replaceAll("</*p>", "")));
            while (!q.getAnswers().empty())
                content.append(getAnswerDiv(id, q.getAnswers().pop()));
        }
        // Save button
        content.append("""
                        <button onclick="saveGrading()">Save Grading</button>
                        <button onclick="saveRubrics()">Save Rubrics</button>
                    </body>
                """);
        Utils_HTML.writeToHTMLFile(GRADING_FOLDER + "/" + summary, content.toString());
    }

    private static String getAnswerDiv(int qID, Answer answer) {
        int sID = answer.getStudent().getSubID();
        String name = answer.getStudent().toString();
        int attempt = answer.getStudent().getAttempt();
        String content = answer.getAnswer();
        String result = Utils_HTML.parseToHtmlParagraph(content);
        String elementID = attempt + "-" + sID + "_" + qID;
        return String.format("""
                <div>
                    <p><b>%s</b></p>
                    %s
                    <hr>
                    <div style="display: flex; flex-flow: column wrap; color: blueviolet">
                        <label>Add a new rubric:
                            <input type="text" id="%4$s" size="60">
                            <input type="button" onclick="addRubric('%3$s','%4$s')" value="ADD">
                        </label>
                        <label style="display: flex; flex-flow: row wrap; width: 600px; justify-content: flex-end">
                            Choose a rubric:
                            <select id="s_%3$s" onchange="applyRubric('%3$s', 's_%3$s')"
                                style="flex-basis: 60%%; flex-grow: 1" size="3">
                                <option value="No rubrics">No rubrics, either add one or load saved file (each line is a rubric)
                                </option>
                            </select>
                            <button onclick="deleteRubric('s_%3$s')">Delete selected rubric</button>
                            <button onclick="clearRubric()">Clear all rubrics</button>
                        </label>
                        <label style="display: flex; flex-flow: column wrap; width: 600px">
                            Or directly enter below:
                            <textarea id="%3$s" rows="5"></textarea>
                            <button style="width: fit-content" onclick="clearComment('%3$s')">Clear all comments for this student</button>
                        </label>
                    </div>
                </div>
                """, name, result, elementID, sID);
    }

}