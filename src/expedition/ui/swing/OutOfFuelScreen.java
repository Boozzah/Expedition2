/*
 * OutOfFuelScreen
 *
 * Running out of fuel part way through a jump, from Nadia's example: the ship drifting in
 * space, END GAME, and BACK IN TIME to go back to the last planet for a price in points.
 * Back in time is greyed out when the last planet has no oil left to go back for. What back
 * in time costs is explained in the ☰ menu's Help instead of on the screen, and the button's
 * tooltip says it too.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

class OutOfFuelScreen extends GameScreen {

    static final Object END_GAME = new Object();
    static final Object BACK_IN_TIME = new Object();

    private static final int TITLE_TOP = 50;
    private static final int SHIP_X = 195;
    private static final int SHIP_Y = 240;
    private static final int SHIP_W = 205;

    private final BufferedImage background = Assets.scaled(Assets.cached("backgrounds/space-planet.png"), Assets.WIDTH, Assets.HEIGHT);
    private final BufferedImage shipArt = Assets.scaledToWidth(Assets.cached("ship/spaceship.png"), SHIP_W);
    private final Font titleFont = Assets.pixelta(58f);
    private final ImageButton backInTime;

    OutOfFuelScreen(Consumer<Object> answers) {
        super(answers);
        ImageButton end = Draw.longButton("END GAME", 205, 54, 38f);
        end.setLocation(22, 646);
        end.addActionListener(e -> answer(END_GAME));
        add(end);

        backInTime = Draw.longButton("BACK IN TIME", 205, 54, 38f);
        backInTime.setLocation(250, 646);
        backInTime.addActionListener(e -> answer(BACK_IN_TIME));
        add(backInTime);
    }

    // EDT only
    void show(boolean canGoBack, int pointsCost) {
        backInTime.setEnabled(canGoBack);
        backInTime.setToolTipText(canGoBack
                ? "Go back to the last planet for " + pointsCost + " points"
                : "There is no oil left on the last planet to go back for");
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(background, 0, 0, null);
        Draw.centred(g2, "You ran out of fuel...", titleFont, Color.WHITE, Assets.WIDTH / 2, TITLE_TOP);
        g2.drawImage(shipArt, SHIP_X, SHIP_Y, null);
        g2.dispose();
    }
}
