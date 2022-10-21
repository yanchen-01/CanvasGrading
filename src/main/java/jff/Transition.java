package jff;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Stack;

/**
 * A transition with graphic representation.
 */
public class Transition {
    private final double theta;
    private final int distance; // from's center - to's edge
    private final Stack<String> labels;
    private final Point origin; // from's center

    /**
     * Construct a transition that connecting 2 states.
     *
     * @param from the state that the transition is from
     * @param to   the state that the transition is to
     */
    public Transition(State from, State to) {
        origin = from.getCenter();
        Point dest = to == null ? from.getCenter() : to.getCenter();
        distance = to == null ? 0 : (int) origin.distance(dest) - State.RADIUS;
        theta = Math.atan2(dest.y - origin.y, dest.x - origin.x);
        labels = new Stack<>();
    }

    /**
     * Construct a transition that transits to itself.
     *
     * @param self the state that the transition is from and to
     */
    public Transition(State self) {
        this(self, null);
    }

    /**
     * Add a label to the transition.
     *
     * @param label label, currently set in {@link Utils_Draw}
     */
    public void addLabel(String label) {
        labels.push(label);
    }

    /**
     * Draw the transition (currently only straight lines or parabola).
     *
     * @param g2 Graphics2D object to draw the transition
     */
    public void draw(Graphics2D g2) {
        AffineTransform original = g2.getTransform();
        drawConnection(g2);
        drawArrowHead(g2);
        drawLabels(g2);
        g2.setTransform(original);
    }

    private void drawConnection(Graphics2D g2) {
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(Color.black);
        if (distance == 0) { // Pointing to itself
            g2.drawArc(origin.x - 30, origin.y - 100, 60, 120, 0, 180);
        } else {
            g2.rotate(theta, origin.x, origin.y + 10);
            g2.drawLine(origin.x, origin.y, origin.x + distance, origin.y);
        }
    }

    private void drawArrowHead(Graphics2D g2) {
        int[] xPoints = {origin.x + distance - 20, origin.x + distance, origin.x + distance - 20};
        int[] yPoints = {origin.y - 10, origin.y, origin.y + 10};
        if (distance == 0) { // Pointing to itself, g2 haven't rotate yet
            g2.rotate(Math.PI / 2, origin.x, origin.y);
            g2.translate(-40, -30); // after rotate 90, swap x and y.
        }
        g2.drawPolyline(xPoints, yPoints, 3);
    }

    private void drawLabels(Graphics2D g2) {
        Font font = new Font("TimesRoman", Font.PLAIN, 25);
        g2.setFont(font);
        if (distance == 0) {
            g2.rotate(-Math.PI / 2, origin.x + 30, origin.y + 40);
        }
        int y = origin.y - 10;
        while (!labels.isEmpty()) {
            String label = labels.pop();
            FontMetrics metrics = g2.getFontMetrics();
            int labelW = metrics.stringWidth(label);
            int x = distance == 0 ? -labelW :
                    (distance - labelW + State.RADIUS) / 2;
            g2.drawString(label, origin.x + x, y);
            y -= 25;
        }
    }
}
