/*
 * NoticeBar
 *
 * The small box near the bottom of the window for the things the game says along the way,
 * like "Run saved." or "Hauled 64 oil." In the console these are printed lines, in the
 * window they appear here on top of whatever screen is showing and fade after a few seconds.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

class NoticeBar extends JComponent {

    private static final int MAX_LINES = 3;
    private static final int SHOW_MS = 5000;
    private static final int WIDTH = 760;
    private static final int BOTTOM_MARGIN = 26;
    private static final int PADDING = 12;
    private static final Color BOX = new Color(3, 15, 31, 215);

    private final Font font = Assets.pixelta(24f);
    private final List<String> lines = new ArrayList<>();

    // clears the notices once nothing new has come in for a while
    private final Timer fade = new Timer(SHOW_MS, e -> clear());

    NoticeBar() {
        fade.setRepeats(false);
        setVisible(false);
    }

    // EDT only
    void add(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        lines.add(line.trim());
        while (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
        int height = PADDING * 2 + lines.size() * Draw.lineHeight(font);
        setBounds((Assets.WIDTH - WIDTH) / 2, Assets.HEIGHT - BOTTOM_MARGIN - height, WIDTH, height);
        setVisible(true);
        repaint();
        fade.restart();
    }

    void clear() {
        lines.clear();
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(BOX);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(Draw.LIGHT_BLUE);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        Draw.smoothText(g2);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int x = (getWidth() - fm.stringWidth(line)) / 2;
            g2.drawString(line, x, PADDING + i * Draw.lineHeight(font) + fm.getAscent());
        }
        g2.dispose();
    }
}
