/*
 * FuelGauge
 *
 * Nadia's E to F fuel gauge with "175/350" over it. The art is the dial and the red pivot,
 * the needle is drawn here at the angle for how full the tank is, so one picture covers
 * every amount of fuel rather than needing a drawing for each.
 */
package expedition.ui.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

class FuelGauge extends JComponent {

    // measured on the gauge art (171x69): the middle of the red pivot, and the needle's
    // angle pointing at E and at F, in degrees anticlockwise from pointing right
    private static final double PIVOT_X = 85.5;
    private static final double PIVOT_Y = 64;
    private static final double EMPTY_ANGLE = 149;
    private static final double FULL_ANGLE = 31;
    private static final double NEEDLE_LENGTH = 52;
    private static final int ART_WIDTH = 171;

    private static final int LABEL_HEIGHT = 30;

    private final BufferedImage dial;
    private final double scale;
    private final Font labelFont;
    private final Color labelColour;
    private int fuel = 0;
    private int capacity = 1;

    // width is how wide the dial is drawn, the label goes above it
    FuelGauge(int width, Color labelColour) {
        BufferedImage art = Assets.cached("ui/fuel-gauge.png");
        scale = (double) width / ART_WIDTH;
        dial = Assets.scaled(art, width, (int) Math.round(art.getHeight() * scale));
        labelFont = Assets.pixelta((float) (LABEL_HEIGHT * 0.8));
        this.labelColour = labelColour;
        setSize(width, LABEL_HEIGHT + dial.getHeight());
    }

    // EDT only
    void setFuel(int fuel, int capacity) {
        this.fuel = fuel;
        this.capacity = Math.max(1, capacity);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Draw.centred(g2, fuel + "/" + capacity, labelFont, labelColour, getWidth() / 2, 2);
        g2.drawImage(dial, 0, LABEL_HEIGHT, null);

        // Condensed Oil can take the tank over full, the needle just stops at F
        double fraction = Math.max(0, Math.min(1, (double) fuel / capacity));
        double angle = Math.toRadians(EMPTY_ANGLE + (FULL_ANGLE - EMPTY_ANGLE) * fraction);
        double px = PIVOT_X * scale;
        double py = LABEL_HEIGHT + PIVOT_Y * scale;
        double length = NEEDLE_LENGTH * scale;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke((float) Math.max(2, 3 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) px, (int) py, (int) (px + Math.cos(angle) * length), (int) (py - Math.sin(angle) * length));
        g2.dispose();
    }
}
