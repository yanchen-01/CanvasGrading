package jff;

import helpers.Utils;
import obj.FileInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static jff.Constants_JFF.NOT_DFA;
import static jff.Constants_JFF.TURING_WITH_BLOCKS;
import static jff.Utils_JFF.getContent;

/**
 * Utility class for drawing an automaton based on a .jff file.
 */
public class Utils_Draw {

    static HashMap<String, State> states;
    static HashMap<String, Transition> transitions;
    static HashMap<String, Integer> outGoingTransitions;
    static int width, height;
    static String machineType;
    static boolean isDFA, checkDFA;

    /**
     * Draw jff to png.
     *
     * @param inFile   the input file in .jff
     * @param fileInfo the info about the file
     */
    public static void drawJff(File inFile, FileInfo fileInfo) {
        initialize(fileInfo);
        Document doc = Utils_JFF.getDoc(inFile);
        assert doc != null;
        machineType = Utils_JFF.getContent(doc.getDocumentElement(), "type");
        assert machineType != null;
        String state = machineType.equals(TURING_WITH_BLOCKS) ?
                "block" : "state";
        goOverElements(doc, state, Utils_Draw::setState);
        goOverElements(doc, "transition", Utils_Draw::setTransition);

        if (checkDFA && isDFA)
            isDFA = checkDFAFinalStep();
        if (!isDFA)
            fileInfo.setError(NOT_DFA);

        saveImage(fileInfo.getFullName());
    }

    private static void initialize(FileInfo info) {
        checkDFA = isDFA = info.getJffType().equals("dfa");
        width = height = 0;
        states = new HashMap<>();
        transitions = new HashMap<>();
        outGoingTransitions = new HashMap<>();
    }

    private static void goOverElements(Document doc, String tag, ParseElement parser) {
        NodeList statesList = doc.getElementsByTagName(tag);
        for (int i = 0; i < statesList.getLength(); i++) {
            Node nNode = statesList.item(i);
            String parent = nNode.getParentNode().getNodeName();
            if (nNode.getNodeType() == Node.ELEMENT_NODE
                    && parent.equals("automaton")) {
                Element element = (Element) nNode;
                parser.parse(element);
            }
        }
    }

    private static void setState(Element element) {
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        String x = getContent(element, "x");
        String y = getContent(element, "y");
        State state = new State(x, y, name);
        int w = state.getCenter().x;
        if (w > width) width = w;
        int h = state.getCenter().y;
        if (h > height) height = h;
        if (element.getElementsByTagName("initial").getLength() != 0)
            state.isInitial();
        if (element.getElementsByTagName("final").getLength() != 0)
            state.isFinal();
        states.put(id, state);

        if (checkDFA) {
            outGoingTransitions.put(id.concat("a"), 0);
            outGoingTransitions.put(id.concat("b"), 0);
        }
    }

    private static void setTransition(Element element) {
        String from = getContent(element, "from");
        String to = getContent(element, "to");
        State fromState = states.get(from);
        State toState = states.get(to);

        String label = getLabel(element);
        String id = from + "-" + to;
        assert from != null;
        final Transition transition = from.equals(to) ?
                new Transition(fromState) : new Transition(fromState, toState);
        transitions.putIfAbsent(id, transition);
        transitions.get(id).addLabel(label);

        if (checkDFA && isDFA) {
            String out = from.concat(label);
            if (outGoingTransitions.get(out) == null
                    || outGoingTransitions.get(out) != 0)
                isDFA = false;
            else
                outGoingTransitions.put(out, 1);
        }
    }

    private static void saveImage(String outFilename) {
        try {
            width += State.DIAMETER;
            height += State.DIAMETER;
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bufferedImage.createGraphics();
            g2.setColor(Color.white);
            g2.fillRect(0, 0, width, height);

            transitions.forEach((id, transition) -> transition.draw(g2));
            states.forEach((id, state) -> state.draw(g2));
            if (machineType.equals(TURING_WITH_BLOCKS))
                g2.drawString("With building blocks, check manually", 30, 30);
            else if (!isDFA)
                g2.drawString(NOT_DFA, 30, 30);

            g2.dispose();
            if (!outFilename.endsWith(".png"))
                outFilename = outFilename + ".png";
            File file = new File(outFilename);
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            Utils.printWarning("fail to draw jff to %s", e, outFilename);
        }
    }

    private static boolean checkDFAFinalStep() {
        for (Integer i : outGoingTransitions.values()) {
            if (i != 1) {
                return false;
            }
        }
        return true;
    }

    private static String getLabel(Element element) {
        String read = getLabelPart(element, "read");
        if (machineType.equals("pda")) {
            String pop = getLabelPart(element, "pop");
            String push = getLabelPart(element, "push");
            return String.format("%s, %s; %s", read, pop, push);
        } else if (machineType.contains("turing")) {
            String write = getLabelPart(element, "write");
            String move = getLabelPart(element, "move");
            return String.format("%s; %s, %s", read, write, move);
        }
        return read;
    }

    private static String getLabelPart(Element element, String tag) {
        String result = getContent(element, tag);
        if (result == null) return "";
        else if (result.isEmpty())
            return machineType.contains("turing") ? "\u25A1" : "\u03BB";
        else return result;
    }
}