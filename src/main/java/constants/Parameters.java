package constants;

public class Parameters {
    public static String AUTH = "";
    public static String QUIZ_URL = ""; // TODO: remove this
    public static String API = "https://sjsu.instructure.com/api/v1";
    public static String REPORT_NAME = "temp-report";
    public static final String ASSIGNMENTS = "(Programming )?(Assignment|Exercise).*";
    public static final String MIDTERM = "Midterm.*";
    public static final String API_TOKEN = "TOKEN";
    public static final String USERNAME = "GIT_USER_NAME";
    public static final String PASSWORD = "GIT_PASSWORD";
    public static final String CAL_FOLDER = "../154Cal";
    public static final String CAL_URL = "https://github.com/yanchen-01/154Cal.git";
    // TODO: need to think if this can be moved to PostPoints.java
    public enum COURSE {
        CS154(1578442, 4607163, 1.5),
        CS46A(1570694, 4598868, 2),
        CS175(1578812, 4607217, 1);

        public final int courseID;
        public final int testStudent;
        public final double each;
        public final boolean calculator;
        COURSE (int courseID, int testStudent, double each) {
            this.courseID = courseID;
            this.testStudent = testStudent;
            this.each = each;
            calculator = courseID == 1578442;
        }
    }
}
