/*
 * HowToPlayScreen
 *
 * How to Play from the main menu: Nadia's book over the planet, BACK in the bottom
 * right, and the settings gear in the top right like her example.
 */
package expedition.ui.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

class HowToPlayScreen extends GameScreen {

    static final Object BACK = new Object();

    private static final int BOOK_X = 245;
    private static final int BOOK_Y = 58;
    private static final int BACK_X = 1117;
    private static final int BACK_Y = 622;
    private static final int GEAR_X = 1104;
    private static final int GEAR_Y = 33;

    private final BufferedImage background = Assets.cached("menu-background.png");
    private final BookPanel book = new BookPanel();

    HowToPlayScreen(Consumer<Object> answers, Runnable openSettings) {
        super(answers);
        book.setLocation(BOOK_X, BOOK_Y);
        add(book);

        ImageButton back = Draw.smallButton("BACK");
        back.setLocation(BACK_X, BACK_Y);
        back.addActionListener(e -> answer(BACK));
        add(back);

        ImageButton gear = new ImageButton(Assets.load("settings-button.png"), "Settings");
        gear.setLocation(GEAR_X, GEAR_Y);
        gear.addActionListener(e -> openSettings.run());
        add(gear);
    }

    // EDT only
    void show(List<String> pages) {
        book.show(pages);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        paintBackground(g2, background);
        g2.dispose();
    }
}
