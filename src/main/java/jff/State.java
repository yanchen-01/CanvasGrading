package jff;

import java.awt.*;

/**
 * A state with graphic representation.
 */
public class State {
    /**
     * The radius of the state, currently 50
     */
    public static final int RADIUS = 50;

    private static final int DIAMETER = RADIUS * 2;
    private final int GAP = RADIUS / 5;
    private final Point center;
    private final String name;
    private final int originX;
    private final int originY;
    private boolean isFinal = false;
    private boolean isInitial = false;

    /**
     * Construct a state.
     *
     * @param x x position of the state center
     * @param y y position of the state center
     * @param name name of the state
     */
    public State(String x, String y, String name) {
        int centerX = (int) Double.parseDouble(x) * 2;
        int centerY = (int) Double.parseDouble(y) * 2;
        center = new Point(centerX, centerY);
        this.originX = centerX - RADIUS;
        this.originY = centerY - RADIUS;
        this.name = name;
    }

    /**
     * Get the center of the state graphic.
     *
     * @return the center of the state graphic
     */
    public Point getCenter() {
        return center;
    }

    /**
     * Set the isFinal to true.
     */
    public void isFinal() {
        isFinal = true;
    }

    /**
     * Set the isInitial to true.
     */
    public void isInitial() {
        isInitial = true;
    }

    /**
     * Draw the state (roughly) following JFlap conversion.
     *
     * @param g2 Graphics2D object to draw the state
     */
    public void draw(Graphics2D g2) {
        drawState(g2);
        drawName(g2);
        if (isInitial)
            drawTriangle(g2);
        if (isFinal)
            g2.drawOval(originX + GAP, originY + GAP, (RADIUS - GAP) * 2, (RADIUS - GAP) * 2);
    }

    private void drawState(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 180));
        g2.fillOval(originX, originY, DIAMETER, DIAMETER);
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(originX, originY, DIAMETER, DIAMETER);
    }

    private void drawName(Graphics2D g2) {
        Font font = new Font("TimesRoman", Font.PLAIN, 25);
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        int x = center.x - metrics.stringWidth(name) / 2;
        // Determine the Y coordinate for the text
        // (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = center.y - metrics.getHeight() / 2 + metrics.getAscent();
        g2.drawString(name, x, y);
    }

    private void drawTriangle(Graphics2D g2) {
        int[] xPoints = {originX - GAP * 3, originX, originX - GAP * 3};
        int[] yPoints = {originY + GAP, center.y, originY + DIAMETER - GAP};
        g2.drawPolygon(xPoints, yPoints, 3);
    }
}
