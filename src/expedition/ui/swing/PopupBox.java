/*
 * PopupBox
 *
 * The box a hazard, a jump event or a message pops up in, on top of the screen. The story
 * goes at the top, then what it did ("Mikela: health -20"), then DONE. Hazards get the red
 * outline and a shake from Nadia's sketch, so they feel different from good news.
 *
 * Sized to its text, up to most of the window, so a short message isn't a huge box.
 */
package expedition.ui.swing;

import expedition.ui.screen.Popup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

class PopupBox extends JComponent {

    private static final int WIDTH = 780;
    private static final int PADDING = 48;
    private static final int GAP = 14;
    private static final int BUTTON_W = 136;
    private static final int BUTTON_H = 78;
    private static final int MIN_HEIGHT = 260;
    private static final int MAX_HEIGHT = 660;
    private static final int OUTLINE = 5;

    // the shake: how far, how many moves, and how often
    private static final int SHAKE_PIXELS = 7;
    private static final int SHAKE_MOVES = 12;
    private static final int SHAKE_MS = 30;

    private final Popup popup;
    private final Font titleFont = Assets.pixelta(38f);
    private final Font textFont = Assets.pixelta(28f);
    private final Font lineFont = Assets.pixelta(26f);
    private final BufferedImage box;
    private final List<String> textLines;
    private final List<String> resultLines = new ArrayList<>();
    private boolean done = false;

    PopupBox(Popup popup, Runnable onDone) {
        this.popup = popup;
        setLayout(null);

        // works out the wrapped lines first, so the box can be made the right height
        FontMetrics textMetrics = getFontMetrics(textFont);
        FontMetrics lineMetrics = getFontMetrics(lineFont);
        int textWidth = WIDTH - 2 * PADDING;
        textLines = popup.text().isEmpty() ? List.of() : Draw.wrap(popup.text(), textMetrics, textWidth);
        for (String line : popup.lines()) {
            resultLines.addAll(Draw.wrap(line, lineMetrics, textWidth));
        }

        int height = PADDING + titleHeight() + textLines.size() * Draw.lineHeight(textFont)
                + (resultLines.isEmpty() ? 0 : GAP + resultLines.size() * Draw.lineHeight(lineFont))
                + GAP + BUTTON_H + PADDING / 2;
        height = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height));
        setSize(WIDTH, height);
        box = Assets.scaled(Assets.cached("ui/text-box.png"), WIDTH, height);

        ImageButton doneButton = Draw.smallButton("DONE");
        doneButton.setLocation(WIDTH - BUTTON_W - PADDING / 2, height - BUTTON_H - PADDING / 3);
        // only the first click counts, the box is gone after it
        doneButton.addActionListener(e -> {
            if (!done) {
                done = true;
                onDone.run();
            }
        });
        add(doneButton);
    }

    // EDT only. A hazard shakes once when it appears
    void shakeIfHazard() {
        if (popup.kind() != Popup.Kind.HAZARD) {
            return;
        }
        int startX = getX();
        int[] moves = {0};
        Timer shake = new Timer(SHAKE_MS, null);
        shake.addActionListener(e -> {
            moves[0]++;
            if (moves[0] >= SHAKE_MOVES) {
                setLocation(startX, getY());
                shake.stop();
                return;
            }
            setLocation(startX + (moves[0] % 2 == 0 ? SHAKE_PIXELS : -SHAKE_PIXELS), getY());
        });
        shake.start();
    }

    private int titleHeight() {
        return popup.title().isEmpty() ? 0 : Draw.lineHeight(titleFont) + GAP;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(box, 0, 0, null);
        if (popup.kind() == Popup.Kind.HAZARD) {
            g2.setColor(Draw.DANGER);
            g2.setStroke(new BasicStroke(OUTLINE));
            g2.drawRect(OUTLINE / 2, OUTLINE / 2, getWidth() - OUTLINE, getHeight() - OUTLINE);
        }

        int y = PADDING;
        if (!popup.title().isEmpty()) {
            Color titleColour = popup.kind() == Popup.Kind.HAZARD ? new Color(0xFFD7D2) : Color.WHITE;
            Draw.centred(g2, popup.title(), titleFont, titleColour, getWidth() / 2, y);
            y += titleHeight();
        }

        Draw.smoothText(g2);
        y = drawLines(g2, textLines, textFont, Color.WHITE, y, true);
        if (!resultLines.isEmpty()) {
            y += GAP;
            drawLines(g2, resultLines, lineFont, new Color(0xE4EEF5), y, false);
        }
        g2.dispose();
    }

    // returns where the next line would go
    private int drawLines(Graphics2D g2, List<String> lines, Font font, Color colour, int top, boolean centred) {
        g2.setFont(font);
        g2.setColor(colour);
        FontMetrics fm = g2.getFontMetrics();
        int y = top;
        Rectangle area = new Rectangle(PADDING, 0, getWidth() - 2 * PADDING, 0);
        for (String line : lines) {
            int x = centred ? area.x + (area.width - fm.stringWidth(line)) / 2 : area.x;
            g2.drawString(line, x, y + fm.getAscent());
            y += Draw.lineHeight(font);
        }
        return y;
    }
}
