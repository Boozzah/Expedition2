/*
 * LoreScreen
 *
 * The story at the start of a new run, from Nadia's lore screen: a title over the planet,
 * the story in her grey text box, and NEXT in the bottom right to start playing.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

class LoreScreen extends GameScreen {

    static final Object NEXT = new Object();

    private static final int TITLE_TOP = 118;
    private static final Rectangle BOX = new Rectangle(250, 215, 793, 405);
    // where the text goes inside the box, clear of its border
    private static final Rectangle TEXT = new Rectangle(305, 245, 683, 340);
    private static final int NEXT_X = 1117;
    private static final int NEXT_Y = 622;

    private final BufferedImage background = Assets.cached("menu-background.png");
    private final BufferedImage box = Assets.scaled(Assets.cached("ui/text-box.png"), BOX.width, BOX.height);
    private final Font titleFont = Assets.pixelta(84f);
    private final Font textFont = Assets.pixelta(30f);

    private String title = "";
    private String text = "";

    LoreScreen(Consumer<Object> answers) {
        super(answers);
        ImageButton next = Draw.smallButton("NEXT");
        next.setLocation(NEXT_X, NEXT_Y);
        next.addActionListener(e -> answer(NEXT));
        add(next);
    }

    // EDT only
    void show(String title, String text) {
        this.title = title;
        this.text = text;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        paintBackground(g2, background);
        Draw.centred(g2, title, titleFont, Color.WHITE, Assets.WIDTH / 2, TITLE_TOP);
        g2.drawImage(box, BOX.x, BOX.y, null);
        Draw.paragraph(g2, text, textFont, Color.WHITE, TEXT);
        g2.dispose();
    }
}
