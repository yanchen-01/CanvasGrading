package constants;

import static constants.FolderNames.BY_STUDENT;
import static constants.FolderNames.INDEX;

public class HtmlElements {
    public final static String QUESTION_NAME = "[[question name]]";
    public final static String QUESTION_SCORE = "[[question score]]";
    public final static String QUESTION_ID = "[[question score id]]";
    public final static String QUESTION_CONTENT = "[[question content]]";
    private final static String STYLE = """
            <style>
                h3 {
                    position: fixed;
                    top: 0;
                    width: 200px;
                    height: 25px;
                    overflow-y: auto;
                }
                
                .sidenav {
                    position: fixed;
                    top: 50px;
                    width: 200px;
                    background: #0055A2;
                    padding: 8px 0;
                }
                
                .sidenav a {
                    padding: 6px 8px 6px 8px;
                    text-decoration: none;
                    color: #FFFFFF;
                    display: block;
                }
                
                .sidenav a:hover {
                    color: #E5A823;
                }
                                
                .question {
                    width: available;
                    background: #eee;
                }
                
                .material-icons {
                    vertical-align: middle;
                    margin-right: 5px;
                }
                
                .previous {
                    flex: 1;
                    border-left: 2px solid #939597;
                    padding: 10px;
                    background: #D2E7FD;
                }
            </style>
            """;

    public final static String Q_HTML_HEAD = String.format("""
            <head>
                <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
                <meta charset="UTF-8">
                <script src="../script.js"></script>
                <title>%s</title>
                %s
            </head>
            """, QUESTION_NAME, STYLE);
    public final static String Q_HTML_SIDEBAR = String.format("""
            <div class="sidenav">
                <input type="file" id="grading" accept=".txt" style="display:none">
                <a href='javascript:' onclick="load('grading', loadSaved)"><i class="material-icons">file_upload</i>Load Results</a>
                <input type="file" id="rubrics" accept=".txt" style="display:none">
                <a href='javascript:' onclick="load('rubrics', loadRubric)"><i class="material-icons">file_upload</i>Load
                    Rubrics</a>
                <a href='javascript:' onclick="saveRubrics()"><i class="material-icons">save</i>Save Rubrics</a>
                <a href='javascript:' onclick="saveGrading()"><i class="material-icons">save</i>Save Grading</a>
                <a href="../%s.html">Back to Question List</a>
                <a rel="noopener noreferrer" target="_blank" href="../%s.html">Submissions by Students</a>
            </div>
            """, INDEX, BY_STUDENT);

    public final static String QUESTION_TITLE = String.format("""
            <h3><span id="qNum">%s</span> (score <span id="score">%s</span>)</h3>
            """, QUESTION_NAME, QUESTION_SCORE);

    public final static String QUESTION_DIV = String.format("""
            <div class="question">
                <p>(Qid: %s)</p>
                %s
            </div>
            """, QUESTION_ID, QUESTION_CONTENT);
}
