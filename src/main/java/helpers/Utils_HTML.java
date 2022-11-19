package helpers;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Util methods related to HTML
 */
public class Utils_HTML {
    /**
     * Parse a string content to a paragraph in HTML.
     * TODO: update it if see parsing errors
     *
     * @param content the content to be parsed
     * @return the content with paragraph tag
     */
    public static String parseToHtmlParagraph(String content) {
        StringBuilder result = new StringBuilder();
        Scanner scan = new Scanner(content);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            line = line.replaceAll("\\[LaTeX:.*]", "");
            Scanner lineScan = new Scanner(line);
            while (lineScan.hasNext()) {
                String current = lineScan.next();
                if (current.contains("equation_images") || current.contains("https"))
                    current = parseToHtmlImg(current);

                result.append(" ").append(current);
            }
            result.append("<br>");
            lineScan.close();
        }
        scan.close();

        return "<p>" + result + "</p>";
    }

    /**
     * Parse content with image url to an image in HTML.
     *
     * @param content the content to be parsed
     * @return the content with img tag.
     * <br>If anything wrong with parsing, will just return the original content.
     */
    public static String parseToHtmlImg(String content) {
        try {
            content = content.replaceFirst("\\(", "");
            if (!content.startsWith("https:"))
                content = "https://sjsu.instructure.com" + content;
            if (content.matches(".*scale=1\\)\\S.*"))
                content = content.replace("scale=1)", "scale=1) ");
            Scanner s = new Scanner(content);
            String url = s.next();
            int splitIndex = url.lastIndexOf(")");
            StringBuilder tail = new StringBuilder();
            tail.append(url.substring(splitIndex + 1));
            url = url.substring(0, splitIndex);
            while (s.hasNext()) {
                tail.append(s.next()).append(" ");
            }
            s.close();
            return "<img src=\"" + url + "\"/>" + tail;
        } catch (Exception e) {
            return content;
        }
    }

    /**
     * Write an HTML file.
     *
     * @param filename name of the file (without .html)
     * @param content  content of the file
     */
    public static void writeToHTMLFile(String filename, String content) {
        try (PrintWriter writer = new PrintWriter(filename + ".html")) {
            writer.printf("""
                    <!DOCTYPE html>
                    <html lang="en">
                    %s
                    """, content);
            writer.println("</html>");
        } catch (Exception e) {
            System.out.println("Something wrong when write " + filename);
            e.printStackTrace();
        }
    }
}
