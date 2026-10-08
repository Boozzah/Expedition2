/*
 * MainMenuPanel
 *
 * Screen 1, the main menu, laid out to match Nadia's example menu: the planet
 * background, the EXPEDITION title, four menu buttons down the middle, help in the
 * top left and settings + high scores in the top right.
 *
 * It doesn't decide what any button does. It hands the choice to whoever made it
 * (SwingView), which turns it into the MenuChoice the engine is waiting for.
 *
 * All positions are window pixels in the 1280x720 window, measured from Nadia's
 * 4000x2250 example and multiplied by Assets.SCALE.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;

class MainMenuPanel extends JPanel {

    enum Choice { NEW_GAME, LOAD_GAME, HOW_TO_PLAY, QUIT, HELP, SETTINGS, HIGH_SCORES }

    private static final String TITLE = "EXPEDITION";
    private static final float TITLE_SIZE = 171f;
    private static final int TITLE_TOP = 69;            // top of the letters

    private static final float BUTTON_TEXT_SIZE = 60f;
    private static final Color BUTTON_TEXT = new Color(0xA6CEE6);
    private static final int MENU_TOP = 213;            // top of the first button image
    private static final int MENU_STEP = 113;           // top of one button to the next

    private static final int ICON_MARGIN = 72;          // from the left/right edge
    private static final int ICON_TOP = 33;
    private static final int ICON_STEP = 128;           // gear to trophy

    private final BufferedImage background = Assets.load("menu-background.png");
    private final Font titleFont = Assets.pixelta(TITLE_SIZE);

    MainMenuPanel(Consumer<Choice> onChoice) {
        setLayout(null);   // fixed 1280x720 art, everything is placed by hand
        setPreferredSize(new Dimension(Assets.WIDTH, Assets.HEIGHT));

        BufferedImage menuButton = Assets.load("menu-button.png");
        Font buttonFont = Assets.pixelta(BUTTON_TEXT_SIZE);
        String[] labels = {"NEW GAME", "LOAD GAME", "HOW TO PLAY", "QUIT GAME"};
        Choice[] choices = {Choice.NEW_GAME, Choice.LOAD_GAME, Choice.HOW_TO_PLAY, Choice.QUIT};
        int x = (Assets.WIDTH - menuButton.getWidth()) / 2;
        for (int i = 0; i < labels.length; i++) {
            ImageButton b = new ImageButton(menuButton, labels[i], buttonFont, BUTTON_TEXT);
            b.setLocation(x, MENU_TOP + i * MENU_STEP);
            addButton(b, choices[i], onChoice);
        }

        ImageButton help = new ImageButton(Assets.load("help-button.png"), "How to play");
        help.setLocation(ICON_MARGIN, ICON_TOP);
        addButton(help, Choice.HELP, onChoice);

        ImageButton settings = new ImageButton(Assets.load("settings-button.png"), "Settings");
        settings.setLocation(Assets.WIDTH - ICON_MARGIN - settings.getWidth(), ICON_TOP);
        addButton(settings, Choice.SETTINGS, onChoice);

        // still Nadia's full size file, so it is shrunk here. Switch to Assets.load
        // once she sends a 104x104 one, like the other buttons
        ImageButton scores = new ImageButton(Assets.image("score-button.png", false), "High scores");
        scores.setLocation(Assets.WIDTH - ICON_MARGIN - scores.getWidth(), ICON_TOP + ICON_STEP);
        addButton(scores, Choice.HIGH_SCORES, onChoice);
    }

    private void addButton(ImageButton b, Choice choice, Consumer<Choice> onChoice) {
        b.addActionListener(e -> onChoice.accept(choice));
        add(b);
    }

    // background and title are pictures, not buttons, so they are painted straight on
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(background, 0, 0, null);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        Rectangle2D ink = titleFont.createGlyphVector(g2.getFontRenderContext(), TITLE)
                .getVisualBounds();
        double x = (Assets.WIDTH - ink.getWidth()) / 2 - ink.getX();
        double y = TITLE_TOP - ink.getY();
        g2.drawString(TITLE, (float) x, (float) y);
        g2.dispose();
    }
}
