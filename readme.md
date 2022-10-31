# For Canvas Grading by questions. 
## Tech Specs
- JAVA 17
- IntelliJ IDEA
- Maven

## How To Use
Currently, no GUI. Runnable in grading folder: 
- Setup - generates files needed for grading by question: 
  - HTML files for each question and submissions by students. 
  - JSON files to handle all-or-nothing MC and unanswered.  
- Upload - uploads scores and comments after grading.

For more details, check the code and readme under [grading folder (to be updated)](https://github.com/yanchen-01/CanvasGrading/tree/master/src/main/java/grading).

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