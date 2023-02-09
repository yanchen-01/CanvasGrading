package constants;

public class Parameters {
    public static String AUTH = "";
    public static String API_URL = "";
    public static String ASSIGNMENT_URL = "";
    public static String SUBMISSION_FOLDER = "";
    public static String API = "https://sjsu.instructure.com/api/v1";
    public static final String ASSIGNMENTS = "(Programming )?(Assignment|Exercise).*";
    public static final String MIDTERM = "Midterm.*";
    public enum COURSE {
        CS154(1557837, 4575659, 1.5),
        CS166(1560831, 4574232, 3),
        CS175(1557385, 4574237, 1);

        public final int courseID;
        public final int testStudent;
        public final double each;
        COURSE (int courseID, int testStudent, double each) {
            this.courseID = courseID;
            this.testStudent = testStudent;
            this.each = each;
        }
    }
}
