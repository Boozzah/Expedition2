/*
 * GameScreen
 *
 * What every one of Nadia's game screens has in common: it is the size of the window,
 * everything on it is placed by hand like the main menu, and it hands the player's answer
 * back to SwingView for the waiting engine.
 *
 * A screen only answers once per question. SwingView calls startWaiting() each time it
 * asks something, and the first click after that is the answer. A double click can't
 * leave a second answer in the queue for the next question to pick up by mistake.
 */
package expedition.ui.swing;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JPanel;

abstract class GameScreen extends JPanel {

    private final Consumer<Object> answers;
    private boolean waiting = false;

    GameScreen(Consumer<Object> answers) {
        this.answers = answers;
        setLayout(null);   // fixed 1280x720 art, everything is placed by hand
        setPreferredSize(new Dimension(Assets.WIDTH, Assets.HEIGHT));
        setBackground(Draw.NAVY);
    }

    // EDT only. Called when the engine asks this screen a new question
    void startWaiting() {
        waiting = true;
    }

    // EDT only. The engine has stopped asking, e.g. the run was left from the in-game menu,
    // so a late click or the cockpit's lever timer can't answer a question nobody asked
    void stopWaiting() {
        waiting = false;
    }

    // EDT only. True while the engine is waiting on this screen
    boolean isWaiting() {
        return waiting;
    }

    // hands the answer to the engine, the first one per question only
    protected void answer(Object value) {
        if (!waiting) {
            return;
        }
        waiting = false;
        answers.accept(value);
    }

    // a full window background, stretched if it isn't exactly window sized
    protected void paintBackground(Graphics2D g, BufferedImage background) {
        g.drawImage(background, 0, 0, Assets.WIDTH, Assets.HEIGHT, null);
    }
}
