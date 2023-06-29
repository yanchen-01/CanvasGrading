package others;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.Utils;
import helpers.Utils_HTML;
import helpers.Utils_HTTP;
import obj.Assignment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

public class UpdateInfo {
    static String URL, COURSE_URL, MATERIALS_URL, UPDATE_TIME;
    static Document MATERIALS, HOMEPAGE;
    static boolean NOTIFY = false;

    public static void main(String[] args) throws ParseException {
        Utils.runFunctionality(UpdateInfo::update);
    }

    static void update(Scanner in) throws Exception {
        URL = Utils.askForParam(in, "related file/assignment url");
        URL = Utils.getApiUrl(URL);
        COURSE_URL = URL.replaceAll("/(files|assignments|quizzes).*", "");
        MATERIALS_URL = COURSE_URL + "/pages/course-materials";
        HOMEPAGE = Utils.getHtmlDoc(COURSE_URL + "/front_page", true);
        MATERIALS = Utils.getHtmlDoc(MATERIALS_URL, true);
        if (URL.contains("files")) {
            URL = URL.replaceAll("files/.+preview=", "files/");
            CourseFile file = Utils.getObjFromURL(URL, CourseFile.class);
            UPDATE_TIME = file.updateTime;
            handleFile(file, in);
        } else {
            handleAssignment();
        }
        Utils_HTML.writeToHTMLFile("temp-home", HOMEPAGE.body().html());
        Utils_HTML.writeToHTMLFile("temp-material", MATERIALS.body().html());
        Utils.printDoneProcess("Setup done");
        String p = Utils.askForParam(in, "post? (Y/N)");
        if (!p.equals("Y")){
            Utils.printDoneProcess("Posting cancelled, bye");
            return;
        }
        String n = Utils.askForParam(in, "notify? (Y/N)");
        NOTIFY = n.equals("Y");

        postPage(COURSE_URL + "/front_page", HOMEPAGE);
        postPage(MATERIALS_URL, MATERIALS);

        Utils.printDoneProcess("Homepage and course materials page updated, double-check on Canvas");
    }

    static void postPage(String url, Document document) {
        ObjectMapper mapper = new ObjectMapper(); // create once, reuse
        ObjectNode home = mapper.createObjectNode();
        home.putObject("wiki_page")
                .put("body", document.body().html())
                .put("notify_of_update", NOTIFY);
        Utils_HTTP.putData(url, home.toString());
        NOTIFY = false;
    }

    static void handleAssignment() throws JsonProcessingException {
        Quizz assignment = Utils.getObjFromURL(URL, Quizz.class);
        Date due = Utils.parseISODate(assignment.due, 0);

        Element a = new Element("a");
        a.attr("title", assignment.getName());
        a.attr("href", Utils.getOriginalUrl(URL));
        a.text(assignment.getName());

        String li = String.format("<li>%s: %s", Utils.formatDate(due), a);
        Elements as = HOMEPAGE.select("h4:contains(Assignment(s) Due) + ul > li");
        Objects.requireNonNull(as.last()).before(li);

        int num = Utils.extractInt(assignment.getShortName());
        a.text(assignment.getShortName());
        Element cell = MATERIALS.select("td[colspan=3]").get(num);
        String original = cell.html();
        original = original.replaceAll(assignment.getShortName(), a.outerHtml());
        cell.html(original);
    }

    static void handleFile(CourseFile file, Scanner in) {
        Element e = HOMEPAGE.select("li:has(a:contains(Files))").first();
        Element f = HOMEPAGE.getElementById("file_update_time");
        if (e == null || f == null)
            throw new RuntimeException("fail to update pages, manually do it");

        f.html(UPDATE_TIME);
        e.remove();
        Elements anns = HOMEPAGE.select("h4:contains(Latest Updates) + ul > li");
        Objects.requireNonNull(anns.first()).before(e);

        String filename = file.filename;
        if (filename.contains("Solution"))
            handleSolution(filename);
        else handleNotes(filename, in);
    }

    static void handleSolution(String filename) {
        Element solution = getFileElement(filename);
        int num = Utils.extractInt(filename);
        int current = num * 3 + 1;
        Elements rows = MATERIALS.getElementsByTag("tr");
        updateAnnouncement(solution, num);

        unBold(rows, current);
        Lesson[] lessons = boldCurrentAndGetLessons(rows, current);

        String temp = addSolutionLinkToMaterials(solution, num);
        updateHomepage(temp, num, lessons);
    }

    static void updateAnnouncement(Element solution, int num) {
        solution.text("Assignment " + num + " Solution");
        Element e = HOMEPAGE.select("li:has(a[title~=Assignment\\d+Solution.pdf])").first();
        if (e == null)
            throw new RuntimeException("fail to update pages, manually do it");
        e.remove();
        Elements anns = HOMEPAGE.select("h4:contains(Latest Updates) + ul > li");
        String s = String.format("<li>%s: %s Posted", UPDATE_TIME, solution);
        Objects.requireNonNull(anns.first()).before(s);
    }

    static void unBold(Elements rows, int current) {
        for (int i = current; i < current + 3; i++) {
            rows.get(i).getElementsByTag("strong").unwrap();
        }
    }

    static Lesson[] boldCurrentAndGetLessons(Elements rows, int current) {
        Lesson[] lessons = new Lesson[2];
        for (int i = current + 3; i < current + 6; i++) {
            Element row = rows.get(i);
            row.attr("style", "height: 28px;");
            Elements tds = row.getElementsByTag("td");
            int size = tds.size();
            for (int j = 0; j < size; j++) {
                Element td = tds.get(j);
                String content = td.html();
                td.html("<strong>" + content);
                if (i == current + 5) continue;
                int k = i - current - 3;

                if (j == size - 2 && !td.text().equals("/"))
                    lessons[k] = new Lesson(td.text());
                else if (j == size - 1 && lessons[k] != null) {
                    td.attr("id", "L" + lessons[k].num);
                    lessons[k].name = td.text().replace(" (Recording)", "");
                }
            }
        }
        return lessons;
    }

    static String addSolutionLinkToMaterials(Element solution, int num) {
        solution.text("Solution");
        Element cell = MATERIALS.select("td[colspan=3]").get(num);
        String original = cell.html();
        String temp = original.replace("Solution", solution.outerHtml());
        cell.html(temp);
        return original;
    }

    static void updateHomepage(String source, int num, Lesson[] lessons) {
        String start = source.replaceAll(".*\\(", "");
        start = "2023/" + start.replaceAll("\\).*", "");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDate startDate = LocalDate.parse(start, formatter).plusDays(1);
        LocalDate endDate = startDate.plusDays(6);

        formatter = DateTimeFormatter.ofPattern("MMM. dd");
        String currentWeek = String.format("%d (%s - %s)",
                num + 1, startDate.format(formatter), endDate.format(formatter));
        Element w = HOMEPAGE.getElementById("week_info");
        Objects.requireNonNull(w).text(currentWeek);

        Elements ls = HOMEPAGE.select("h4:contains(Course Materials) + ul > li");
        for (int i = 0; i < ls.size() - 1; i++) {
            ls.get(i).remove();
        }
        for (int i = 0; i < 4; i++) {
            int j = i > 1 ? i - 2 : i;
            Lesson lesson = lessons[j];
            if (lesson == null) continue;
            String pre = i > 1 ? "Recording" : "Lecture Notes";
            String l = String.format("<li id=\"%s%s\">%s - Lesson %s", pre.charAt(0), lesson.num, pre, lesson);
            Objects.requireNonNull(ls.last()).before(l);
        }
    }

    static void handleNotes(String filename, Scanner in) {
        String num = filename.split("-")[1];
        String rNum = num.replace('L', 'R');
        Element notes = getFileElement(filename);
        addLinkToElement(HOMEPAGE, num, notes);
        String url = Utils.askForParam(in, "Recording url");
        Element recording = getUrlElement(url);
        addLinkToElement(HOMEPAGE, rNum, recording);

        updateMaterialsPage(num, notes, recording);
    }

    static void addLinkToElement(Document doc, String id, Element link) {
        Element element = doc.getElementById(id);
        if (element == null)
            throw new RuntimeException("fail to update pages, manually do it");
        if (link.text().isBlank())
            link.text(element.text());
        element.html(link.outerHtml());
    }

    static void updateMaterialsPage(String num, Element notes, Element recording) {
        Element element = MATERIALS.getElementById(num);
        if (element == null)
            throw new RuntimeException("fail to update pages, manually do it");
        String notesName = element.text().replace(" (Recording)", "");
        notes.text(notesName);
        recording.text("Recording");
        String result = String.format("<strong>%s (%s)</strong>",notes, recording);
        element.html(result);
    }

    static Element getFileElement(String filename) {
        Element result = new Element("a");
        result.attr("class", "instructure_file_link instructure_scribd_file inline_disabled");
        result.attr("title", filename);
        result.attr("href", Utils.getOriginalUrl(URL));
        result.attr("target", "_blank");
        result.attr("data-api-endpoint", URL);
        result.attr("data-api-returntype", "File");
        return result;
    }

    static Element getUrlElement(String url) {
        Element result = new Element("a");
        result.attr("class", "inline_disabled");
        result.attr("href", url);
        result.attr("target", "_blank");
        result.attr("rel", "noopener noreferrer");
        return result;
    }

    static class CourseFile {
        @JsonProperty("filename")
        String filename;
        String updateTime;

        @JsonProperty("modified_at")
        public void setUpdateDate(String updateTime) {
            Date date = Utils.parseISODate(updateTime, 0);
            this.updateTime = Utils.formatDate(date);
        }
    }

    static class Lesson {
        String num;
        String name;

        Lesson(String num) {
            this.num = num;
            name = "";
        }

        @Override
        public String toString() {
            return num + " - " + name;
        }
    }

    static class Quizz extends Assignment {
        @JsonProperty("title")
        private String name;
        @JsonProperty("due_at")
        private String due;
    }

}
