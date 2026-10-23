/*
 * Draw
 *
 * The drawing every game screen needs: Nadia's colours, centred text, text wrapped to fit
 * a box, and her buttons with a label on them. Kept in one place so all the screens
 * look the same and none of them repeats the maths for lining text up.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

final class Draw {

    // the dark navy of every space background, and the light blue of the menu buttons' text
    static final Color NAVY = new Color(0x030F1F);
    static final Color LIGHT_BLUE = new Color(0xA6CEE6);
    // the dark text on the grey buttons and boxes
    static final Color DARK_TEXT = new Color(0x2E3A46);
    // the red of a closed site or a broken part, and the hazard popup's outline
    static final Color DANGER = new Color(0xD93A32);
    static final Color GOOD = new Color(0x5ACB5A);

    // gap between lines of wrapped text, as a fraction of the font size
    private static final double LINE_SPACING = 1.15;

    private Draw() {
    }

    static void smoothText(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // Text centred on centreX with the top of its letters at top. Lines up the letters
    // themselves, not the font's line box, the same as the menu title
    static void centred(Graphics2D g, String text, Font font, Color colour, int centreX, int top) {
        smoothText(g);
        g.setFont(font);
        g.setColor(colour);
        Rectangle2D ink = font.createGlyphVector(g.getFontRenderContext(), text).getVisualBounds();
        double x = centreX - ink.getWidth() / 2 - ink.getX();
        double y = top - ink.getY();
        g.drawString(text, (float) x, (float) y);
    }

    // text with its left edge at x and the top of its letters at top
    static void left(Graphics2D g, String text, Font font, Color colour, int x, int top) {
        smoothText(g);
        g.setFont(font);
        g.setColor(colour);
        Rectangle2D caps = font.createGlyphVector(g.getFontRenderContext(), "H").getVisualBounds();
        g.drawString(text, x, (float) (top - caps.getY()));
    }

    // Breaks text into lines no wider than width, on the spaces. A word longer than the
    // whole width gets a line to itself rather than being cut
    static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String tryLine = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(tryLine) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(tryLine);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    // Wraps text to fit the box and draws it centred in it, each line centred like
    // Nadia's text boxes. Shrinks the font a step at a time if the text is too long to fit
    static void paragraph(Graphics2D g, String text, Font font, Color colour, Rectangle box) {
        smoothText(g);
        Font f = font;
        List<String> lines = wrap(text, g.getFontMetrics(f), box.width);
        while (lineHeight(f) * lines.size() > box.height && f.getSize2D() > 12) {
            f = f.deriveFont(f.getSize2D() - 2);
            lines = wrap(text, g.getFontMetrics(f), box.width);
        }

        int step = lineHeight(f);
        int top = box.y + (box.height - step * lines.size()) / 2;
        g.setFont(f);
        g.setColor(colour);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            int x = box.x + (box.width - fm.stringWidth(l)) / 2;
            int baseline = top + i * step + fm.getAscent();
            g.drawString(l, x, baseline);
        }
    }

    static int lineHeight(Font f) {
        return (int) Math.round(f.getSize2D() * LINE_SPACING);
    }

    // one of Nadia's button images at a size, with a label in the middle
    static ImageButton button(String file, int w, int h, String label, float textSize, Color textColour) {
        BufferedImage art = Assets.scaled(Assets.cached(file), w, h);
        return new ImageButton(art, label, Assets.pixelta(textSize), textColour);
    }

    // the small grey BACK / NEXT button in the bottom right of Nadia's screens
    static ImageButton smallButton(String label) {
        return button("ui/small-button.png", 136, 78, label, 50f, Color.WHITE);
    }

    // the long grey button, for END GAME, REPAIR and the rest
    static ImageButton longButton(String label, int w, int h, float textSize) {
        return button("ui/long-button.png", w, h, label, textSize, Color.WHITE);
    }
}
