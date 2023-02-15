# For Canvas Grading by questions. 
## Tech Specs
- JAVA 17 (Java 15 & Above)
- IntelliJ IDEA
- Maven

## How To Use
Currently, no GUI. Runnable files are in [grading folder](https://github.com/yanchen-01/CanvasGrading/tree/master/src/main/java/grading) 
and [others folder](https://github.com/yanchen-01/CanvasGrading/tree/master/src/main/java/others).

- Before using, need to get a [Canvas API Token](https://canvas.instructure.com/doc/api/file.oauth.html) (Account -> Settings -> "+ New Access Token"). 
  - Save the token on your local device (otherwise, you won't be able to find it again.)
  - If you want to save it under the project (otherwise, you will need to enter it every time you use the program), 
  create a class named "PrivateParams" and assign the token value to a String variable named "AUTH". PrivateParams is git-ignored, so it won't be committed and pushed. 
- For most of the functionalities, you will need to provide the assignment URL. It can be either...
  - The assignment detail page: https://\<domain>.instructure.com/courses/\<courseID>/assignment/\<assignmentID>
  - Or the SpeedGrader page: https://\<domain>.instructure.com/courses/\<courseID>/gradebook/speed_grader?assignment_id=\<assignmentID>&student_id=\<studentID>

### Main Functionalities
- Grade Canvas Quiz by question. 
  - First, run grading/Setup to set up. After setting up, MCs will be graded all-or-nothing and unanswered will be given a 0. Also, for each question, a html file will be generated, and you are grading on those html files. 
  - After done grading, put all grading results in one folder and run grading/Upload to upload your grading results. 
  - (For my classes only...) Post the points by running grading/PostPoints.
- Grade Canvas Assignment by pre-set Rubrics (NOT support group assignments yet).
  - First, run grading/GradeByRubrics, select generate a template (a csv file). Record the grades (comments & scores) on the csv file. 
  - After done grading, run grading/GradeByRubrics again but select upload to upload. 
- Hide quiz results for all quizzes - run others/HideResults.
- Grade discussions (all-or-nothing for total <= 1) - run others/GradeDiscussions.
- (Not generalized yet) Batch changing assignment date for selected assignments (using regex).
- (Not generalized yet) Batch giving another attempt to those students meet certain criteria. 

## Folders/Files
- [/src/main/java/](https://github.com/yanchen-01/CanvasGrading/tree/master/src/main/java) - all source code in Java
  - /constants/: folder names, parameters
  - /grading/: runnable files
  - /helpers/: utility classes
  - /jff/: classes and objects related to .jff
  - /obj/: customized object-classes
  - /others/: for other functionalities
  - For more details, check [JavaDoc](https://yanchen-01.github.io/CanvasGradingJavaDoc/)
- /script.js - control the html files that are generated after setup
- /.idea/ - libraries, etc.
- others - for build project, gitignore, etc.