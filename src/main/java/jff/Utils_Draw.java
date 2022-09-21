package jff;

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

import static jff.Utils_JFF.*;

/**
 * Utility class for drawing an automaton based on a .jff file.
 */
public class Utils_Draw {

    static HashMap<String, State> states;
    static HashMap<String, Transition> transitions;
    static HashMap<String, Integer> outGoingTransitions;
    static int width, height;

    /**
     * Draw jff to png.
     *
     * @param inFile the input file in .jff
     * @param outFilename the name of the output file (without extension)
     */
    public static void drawJff(File inFile, String outFilename) {
        initialize();
        Document doc = Utils_JFF.getDoc(inFile);
        assert doc != null;
        goOverElements(doc, "state", Utils_Draw::setState);
        goOverElements(doc, "transition", Utils_Draw::setTransition);
        saveImage(outFilename);
        if (checkDFA && isDFA)
            checkDFAFinalStep();
    }

    private static void initialize() {
        isDFA = true;
        width = 0;
        height = 0;
        states = new HashMap<>();
        transitions = new HashMap<>();
        outGoingTransitions = new HashMap<>();
    }

    private static void goOverElements(Document doc, String tag, ParseElement parser) {
        NodeList statesList = doc.getElementsByTagName(tag);
        for (int i = 0; i < statesList.getLength(); i++) {
            Node nNode = statesList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
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

        String label = getContent(element, "read");
        label = label.isEmpty() ? "\u039B" : label;
        String id = from + "-" + to;
        final Transition transition = from.equals(to) ?
                new Transition(fromState) : new Transition(fromState, toState);
        transitions.putIfAbsent(id, transition);
        transitions.get(id).addLabel(label);

        if (checkDFA) {
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
            width += 100;
            height += 100;
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bufferedImage.createGraphics();
            g2.setColor(Color.white);
            g2.fillRect(0, 0, width, height);

            transitions.forEach((id, transition) -> transition.draw(g2));
            states.forEach((id, state) -> state.draw(g2));

            g2.dispose();
            File file = new File(outFilename + ".png");
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            System.out.println("!Warning: fail to draw jff to " + outFilename + ".png");
        }
    }

    private static void checkDFAFinalStep() {
        for (Integer i : outGoingTransitions.values()) {
            if (i != 1) {
                isDFA = false;
                break;
            }
        }
        checkDFA = false;
    }
}