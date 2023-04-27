package grading;

import helpers.Utils;
import helpers.Utils_HTML;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import static constants.FolderNames.GRADING_FOLDER;

public class MergeQuestions {
    public static void main(String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            String quit;
            do {
                Utils.printPrompt("Filename of the reference question (without .html)");
                String referFile = in.nextLine();
                File refer = new File(GRADING_FOLDER + "/" + referFile + ".html");
                HashMap<String, Element> submissions = getSubmissions(refer);
                Utils.printPrompt("Filename of the question to be graded (without .html)");
                String mergedFile = in.nextLine();
                File merged = new File(GRADING_FOLDER + "/" + mergedFile + ".html");
                mergeSubmissions(merged, submissions);
                Utils.printPrompt("Keep merging? (Y/N)");
                quit = in.nextLine();
            } while (quit.equals("Y"));
        } catch (Exception e) {
            Utils.printFatalError(e);
        }
    }

    static HashMap<String, Element> getSubmissions(File file) throws IOException {
        HashMap<String, Element> result = new HashMap<>();
        Document doc = Jsoup.parse(file);
        result.put("q", doc.select("div.question").first());
        Elements answers = doc.select("div.submission");
        for (Element answer : answers) {
            String key = answer.select("p.student").text();
            Element value = answer.select("div.answer > p").first();
            result.put(key, value);
        }
        return result;
    }

    static void mergeSubmissions(File file, HashMap<String, Element> subMap) throws IOException {
        Document doc = Jsoup.parse(file);
        Elements submissions = doc.select("div.submission");
        Element question = subMap.get("q");
        String qText = String.format("Answer to related question - %s... <br><br>",
                question.ownText().substring(0, 100));
        for (Element submission : submissions) {
            String key = submission.select("p.student").text();
            Element previous = subMap.get(key);
            if (previous == null) continue;
            Element answer = submission.select("div.answer").first();
            assert answer != null;
            answer.attr("style", "display: flex");
            Element current = answer.select("p").first();
            assert current != null;
            current.attr("style", "flex: 1");

            previous.prepend(qText);
            previous.addClass("previous");
            answer.append(previous.toString());
        }

        String filename = file.getAbsolutePath();
        Utils_HTML.writeToHTMLFile(filename, doc.html());
        Utils.printDoneProcess(filename + " updated");
    }
}
