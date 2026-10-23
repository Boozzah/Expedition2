/*
 * ChoiceBox
 *
 * A popup with a question and a column of answers, for the few choices that don't have
 * a screen of their own, like what to throw out when the hold is full.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

class ChoiceBox extends JComponent {

    private static final int WIDTH = 700;
    private static final int PADDING = 40;
    private static final int TEXT_HEIGHT = 110;
    private static final int BUTTON_W = 480;
    private static final int BUTTON_H = 64;
    private static final int BUTTON_STEP = 72;

    private final String text;
    private final Font textFont = Assets.pixelta(30f);
    private final BufferedImage box;
    private boolean done = false;

    // onPick is given the position of the answer clicked
    ChoiceBox(String text, List<String> answers, IntConsumer onPick) {
        this.text = text;
        setLayout(null);
        int height = PADDING + TEXT_HEIGHT + answers.size() * BUTTON_STEP + PADDING;
        setSize(WIDTH, height);
        box = Assets.scaled(Assets.cached("ui/text-box.png"), WIDTH, height);

        for (int i = 0; i < answers.size(); i++) {
            int pick = i;
            ImageButton b = Draw.longButton(answers.get(i), BUTTON_W, BUTTON_H, 34f);
            b.setLocation((WIDTH - BUTTON_W) / 2, PADDING + TEXT_HEIGHT + i * BUTTON_STEP);
            // only the first click counts, the box is gone after it
            b.addActionListener(e -> {
                if (!done) {
                    done = true;
                    onPick.accept(pick);
                }
            });
            add(b);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(box, 0, 0, null);
        Draw.paragraph(g2, text, textFont, Color.WHITE, new Rectangle(PADDING, PADDING - 10, WIDTH - 2 * PADDING, TEXT_HEIGHT));
        g2.dispose();
    }
}
