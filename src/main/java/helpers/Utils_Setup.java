package helpers;

import jff.Constants_JFF;
import obj.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static constants.FolderNames.*;
import static constants.JsonKeywords.*;
import static constants.Parameters.ASSIGNMENT_URL;

/**
 * Util methods related to set up
 */
public class Utils_Setup {

    public static HashMap<Integer, Student> STUDENTS;
    public static HashMap<Integer, Question> QUESTIONS = new HashMap<>();
    public static HashMap<String, QuestionSet> SHORT_ANSWERS = new HashMap<>();
    static List<String[]> CONTENTS;

    /**
     * Set the hashmap of the students.
     * <p>Key is the student ID and value is a student object.
     * <br> The student object contains the submission ID and attempt number.
     * <br> Student name will be updated when {@link #readSubmissions(List)}.
     *
     * @param submissions_json json response from submissions
     */
    public static void setStudents(String submissions_json) {
        STUDENTS = new HashMap<>();
        JSONObject json = new JSONObject(submissions_json);
        JSONArray submissions = json.getJSONArray("quiz_submissions");

        for (int i = 0; i < submissions.length(); i++) {
            JSONObject current = submissions.getJSONObject(i);

            int student_id = current.getInt("user_id");
            // Set submission id & number of attempts
            int sub_id = current.getInt("id");
            int attempt = current.getInt("attempt");
            STUDENTS.put(student_id, new Student(sub_id, attempt));
        }
    }

    /**
     * Set a map of questions and a map of short answers.
     * <ul>
     *     <li>QUESTION map: key is id, value is a question object with qID and type; </li>
     *     <li>SHORT_ANSWERS map: key is qName, value is a question set object with
     *     different versions of the question. </li>
     * </ul>
     *
     * @param questions_json json response from questions
     */
    public static void setQuestions(String questions_json) {
        HashSet<Question> JFFs = new HashSet<>();
        JSONArray qs = new JSONArray(questions_json);
        for (int i = 0; i < qs.length(); i++) {
            JSONObject current = qs.getJSONObject(i);
            int id = current.getInt(ID);
            String type = current.getString(TYPE);
            Question question = new Question(id, type);
            // Other info only matters to essay or file upload questions
            if (type.equals(ESSAY)
                    || type.equals(UPLOAD)) {
                double score = current.getDouble(POINTS);
                String name = current.getString(NAME);
                String content = current.getString(CONTENT);
                content = name.startsWith("Design") ?
                        name + "\n" + content : content;
                question.setContent(content);
                SHORT_ANSWERS.computeIfAbsent(name, k -> new QuestionSet(name, score));
                QuestionSet set = SHORT_ANSWERS.get(name);
                set.add(question);

                if (type.equals(UPLOAD)) {
                    Utils.makeFolder(JFF_FOLDER + "/" + id);
                    Utils.writeToFile(JFF_RESULTS + "/" + id + "p", score + "\n");
                    JFFs.add(question);
                }
            }
            QUESTIONS.put(id, question);
            if (!JFFs.isEmpty())
                Utils.saveObject(Constants_JFF.JFF_QUESTIONS, JFFs);
        }
    }

    /**
     * Read the submission cvs file, then...
     * <ol>
     *     <li>generate json files for scores of MC and unanswered; </li>
     *     <li>add student answers to the question objects in the maps; </li>
     *     <li>generate a html that contains all students' submission links.</li>
     * </ol>
     *
     * @param contents content of the csv file
     */
    public static void readSubmissions(List<String[]> contents) {
        CONTENTS = contents;
        String[] headers = CONTENTS.remove(0);
        StringBuilder htmlContent = new StringBuilder();
        for (String[] row : CONTENTS) {
            // Get student - name: first column; id: second column.
            Student student = STUDENTS.get(Integer.parseInt(row[1]));
            student.setName(row[0]);

            // Append url to submissions_by_students.html
            String url = ASSIGNMENT_URL + "/submissions/" + student.getSubID();
            htmlContent.append(String.format(
                    "<p><a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a></p>",
                    url, student));

            // Read student submission.
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
                if (question == null) continue;
                String type = question.getType();
                String studentAnswer = row[i];
                String pt = row[i + 1];
                // pt is empty means current student didn't get this version of the question
                // so continue to the next column
                if (type.equals(TEXT_ONLY) || pt.isEmpty()) continue;

                double score = Double.parseDouble(pt);
                if (studentAnswer.isBlank() ||
                        (type.equals(MC) && score != 1.0 && score != 0.0))
                    questionJSON.put(qID_string, new Score(qID_string, 0).generateJSON());
                else if (type.equals(ESSAY))
                    question.addStudentAnswer(new Answer(student, studentAnswer));
                else if (type.equals(UPLOAD)) {
                    String picName = String.format("%s/%d-%d_%d.png", JFF_FOLDER,
                            student.getAttempt(), student.getSubID(), qID);
                    if (new File(picName).exists())
                        studentAnswer = String.format("<img src=\"../%s\" width=\"350\">", picName);
                    else
                        studentAnswer = "<span style=\"color:red\">Failed pre-checks. Load pre-check results or manually check. </span>";
                    question.addStudentAnswer(new Answer(student, studentAnswer));
                }

                i++;
            }

            if (!questionJSON.isEmpty())
                Utils.writeScoreAndCommentJSON
                        (questionJSON, JSON_FOLDER + "/" + student.getSubID(), student.getAttempt());
        }
        // Write submissions_by_students.html
        Utils_HTML.writeToHTMLFile("submissions_by_students", htmlContent.toString());
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
            indexContent.append(String.format(
                    "<p><a href=\"%s/%2$s.html\" target=\"_blank\" rel=\"noopener noreferrer\">%2$s</a></p>\n",
                    GRADING_FOLDER, summary));
        });
        indexContent.append(String.format("""
                <p><a href="%1$s" target="_blank" rel="noopener noreferrer">%1$s</a></p>
                </body>""", "submissions_by_students.html"));
        Utils_HTML.writeToHTMLFile(INDEX, indexContent.toString());
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

            content.append(String.format("""
                        <h3>%d<br>%s</h3>
                    """, id, q.getContent()));
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
                            <button onclick="updateRubric('s_%3$s')">Update selected rubric</button>
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
                """, answer.getStudent(), result, elementID, sID);
    }

}