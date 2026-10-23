/*
 * ImageButton
 *
 * A button that is just one of Nadia's images, with optional text on top.
 * Clicks only count on the drawn part of the image, not on its see-through
 * edges, so buttons that sit close together never steal each other's clicks.
 */
package expedition.ui.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JButton;

class ImageButton extends JButton {

    // Where the middle of the text sits, as a fraction of the image height.
    // Measured from Nadia's example menu: the lighter face of the long button
    // sits a little above the middle of the image
    private static final double TEXT_CENTRE = 0.48;

    private static final float DISABLED_ALPHA = 0.35f;

    private final BufferedImage image;
    private final String label;
    private final Color textColour;

    // an icon-only button, the tooltip says what it does
    ImageButton(BufferedImage image, String tooltip) {
        this(image, null, null, null);
        setToolTipText(tooltip);
        getAccessibleContext().setAccessibleName(tooltip);
    }

    ImageButton(BufferedImage image, String label, Font font, Color textColour) {
        this.image = image;
        this.label = label;
        this.textColour = textColour;
        if (font != null) {
            setFont(font);
        }
        if (label != null) {
            getAccessibleContext().setAccessibleName(label);
        }

        // the image is the whole button, Swing draws nothing of its own
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setSize(new Dimension(image.getWidth(), image.getHeight()));
        setPreferredSize(getSize());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // a button that can't be clicked right now is faded out
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DISABLED_ALPHA));
        }
        g2.drawImage(image, 0, 0, null);

        if (label != null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(textColour);

            // centre the letters themselves, not the font's line box. Vertically it
            // centres the capitals, so the tail on a Q doesn't push the text up
            Rectangle2D ink = getFont().createGlyphVector(g2.getFontRenderContext(), label)
                    .getVisualBounds();
            Rectangle2D caps = getFont().createGlyphVector(g2.getFontRenderContext(), "H")
                    .getVisualBounds();
            double x = (image.getWidth() - ink.getWidth()) / 2 - ink.getX();
            double y = image.getHeight() * TEXT_CENTRE - caps.getHeight() / 2 - caps.getY();
            g2.drawString(label, (float) x, (float) y);
        }
        g2.dispose();
    }

    // only the visible part of the image is clickable
    @Override
    public boolean contains(int x, int y) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return false;
        }
        return (image.getRGB(x, y) >>> 24) != 0;
    }
}
