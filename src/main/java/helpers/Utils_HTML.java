package helpers;

import obj.Answer;
import obj.Question;
import obj.QuestionSet;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.Scanner;

import static constants.FolderNames.GRADING_FOLDER;

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
            // Use reluctant quantifiers to remove each [LaTeX :...]
            line = line.replaceAll("\\[(LaTeX:)?.*?]", "");
            Scanner lineScan = new Scanner(line);
            while (lineScan.hasNext()) {
                String current = lineScan.next();
                if (current.contains("equation_images")
                        || current.contains("preview?verifier"))
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
            int width = content.contains("equation_images") ?
                    0 : 350;
            return getHTMLImg(url, width) + tail;
        } catch (Exception e) {
            return content;
        }
    }

    public static String getHTMLEmbed(String filePath, int width) {
        return getHTMLEmbed(filePath, width, false);
    }

    public static String getHTMLEmbed(String filePath) {
        return getHTMLEmbed(filePath, 0);
    }

    public static String getHTMLImg(String filePath, int width) {
        return getHTMLEmbed(filePath, width, true);
    }

    public static String getHTMLImg(String filePath) {
        return getHTMLEmbed(filePath, 0, true);
    }

    private static String getHTMLEmbed(String filePath, int width, boolean image) {
        // make sure it's an image or not
        String temp = filePath.toLowerCase();
        if (!image) {
            image = temp.matches(".+\\.(png|jpg)");
        }
        String tag = image ? "img" : "embed";
        JSONObject attributes = new JSONObject();
        attributes.put("src", filePath);
        attributes.put("alt", filePath);
        // enforce the size of pdf...
        if (temp.endsWith("pdf")) {
            width = 500;
            attributes.put("height", 200);
        }
        if (width != 0)
            attributes.put("width", width);

        return getHTMLElement(tag, attributes, "");
    }

    public static String getHTMLHref(String url) {
        String display = url.replace("temp-", "");
        return getHTMLHref(url, display, true);
    }

    public static String getHTMLHref(String url, Object display, boolean newTab) {
        JSONObject attributes = new JSONObject();
        attributes.put("href", url);
        if (newTab) {
            attributes.put("target", "_blank");
            attributes.put("rel", "noopener noreferrer");
        }
        return getHTMLElement("a", attributes, display.toString());
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

    public static String getBoldText(String content, String alternate) {
        String[] strong = content.split("\u003c/*strong\u003e");
        return strong.length < 2 ? alternate
                : strong[1].replaceAll("\\p{Punct}", "");
    }

    public static String getHTMLElement(String tag, JSONObject attributes, String body) {
        String closing = body.isEmpty() ? "/>"
                : String.format(">%s</%s>", body, tag);
        if (tag.equals("p") || tag.startsWith("h"))
            closing = closing + "\n";
        if (attributes == null)
            return String.format("<%s%s", tag, closing);

        StringBuilder as = new StringBuilder();
        for (String a : attributes.keySet()) {
            String attribute = String.format(" %s=\"%s\"", a, attributes.get(a));
            as.append(attribute);
        }
        return String.format("<%s%s%s", tag, as, closing);
    }

    public static String getHTMLParagraph(String body) {
        return getHTMLElement("p", null, body);
    }
}
