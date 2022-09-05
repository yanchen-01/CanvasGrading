package others;

import helpers.Utils;
import obj.Student;

import java.util.*;

/**
 * Onetime setup: generate student hashmap.
 * <p> If need to update, update the csv then run this to update the maps.
 * <br>Please commit and push if changes!
 */
public class SetStudentHashSet {
    static HashMap<Integer, Student> students;
    static String className;
    public static void main(String[] args) {
        try {
            while(true){
                Scanner in = new Scanner(System.in);
                System.out.print("Enter class name (CS154, etc.) or q to quit: ");
                className = in.nextLine().toUpperCase();
                if(className.equalsIgnoreCase("q")) break;
                Utils.readCSV(className, SetStudentHashSet::setStudents);
            }

        } catch (Exception e) {
            System.out.println("Terminated: " + e.getMessage());
        }
    }

    static void setStudents(List<String[]> content) {
        students = new HashMap<>(70);
        for (String[] row : content) {
            // row[0] = name, row[1] = id
            int id = Integer.parseInt(row[1]);
            Student student = new Student(row[0]);
            students.put(id, student);
        }
        Utils.saveObject(className, students);
        System.out.println(className + " file saved");
    }
}
