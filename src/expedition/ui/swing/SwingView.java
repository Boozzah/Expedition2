/*
 * SwingView
 *
 * The window version of GameView. The window has two screens and swaps between them:
 * - the main menu, Nadia's MainMenuPanel
 * - the text screen, TextPanel, for every part of the game that has no screen of its own yet
 *
 * How the threads work, every method in here follows these rules:
 * - GameEngine keeps its one long loop that waits for answers. It runs on the main
 *   thread, not on Swing's Event Dispatch Thread (EDT), so waiting never freezes the window.
 * - The engine thread never touches the window itself. Anything that changes the
 *   window goes through SwingUtilities.invokeLater, which runs it on the EDT.
 * - To ask a question, the engine puts buttons on screen and then waits on the answers
 *   queue. When the player clicks, the button drops its answer in the queue and the
 *   engine picks it up and carries on.
 *
 * The queue is a LinkedBlockingQueue, not the SynchronousQueue in the GDD. A
 * SynchronousQueue only works if the engine is already waiting when the click comes in,
 * otherwise the click is lost or the window freezes. A LinkedBlockingQueue holds the
 * answer until the engine gets to it.
 *
 * Closing the window is the same as typing Q in the console. It puts a QUIT marker in
 * the queue, and whatever question the engine is on (or gets to next) throws
 * QuitGameException, so Main still gets to shut the database down properly.
 */
package expedition.ui.swing;

import expedition.exception.QuitGameException;
import expedition.ui.GameView;
import expedition.ui.MenuChoice;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class SwingView implements GameView {

    // names of the two screens in the CardLayout
    private static final String MENU_SCREEN = "menu";
    private static final String TEXT_SCREEN = "text";

    // put in the queue when the window is closed, instead of a real answer
    private static final Object QUIT = new Object();

    // the answer from the Continue button, see showMainMenu
    private static final Object CONTINUE = new Object();

    // readInt gets one button per number up to this many, more than that gets a spinner
    private static final int MAX_NUMBER_BUTTONS = 10;

    // filled on the EDT by button clicks, emptied by the engine thread
    private final BlockingQueue<Object> answers = new LinkedBlockingQueue<>();

    // Only used on the engine thread. True when text has been printed since the
    // player last answered something, so they might not have read it yet
    private boolean unreadText = false;

    // the window, only ever touched on the EDT
    private JFrame frame;
    private CardLayout cards;
    private JPanel screens;
    private TextPanel text;

    // Only touched on the EDT. True while the engine is waiting on the main menu, so
    // a menu click only counts when the engine is actually asking for one
    private boolean menuWaiting = false;

    // Only touched on the EDT. Whether there is a saved run for Load Game to resume,
    // set each time the menu is shown
    private boolean canResume = false;

    // builds the window before returning, so the engine can print to it straight away
    public SwingView() {
        try {
            SwingUtilities.invokeAndWait(this::buildWindow);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while opening the game window", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Could not open the game window", e.getCause());
        }
    }

    // ---- GameView, called on the engine thread ----

    @Override
    public void print(String line) {
        unreadText = true;
        SwingUtilities.invokeLater(() -> {
            text.append(line);
            cards.show(screens, TEXT_SCREEN);
        });
    }

    @Override
    public int readInt(String prompt, int min, int max) throws QuitGameException {
        checkNotEdt();
        if (max - min < MAX_NUMBER_BUTTONS) {
            List<String> labels = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (int n = min; n <= max; n++) {
                labels.add(String.valueOf(n));
                values.add(n);
            }
            // Back does the same as typing B in the console
            labels.add("Back");
            values.add(GameView.BACK);
            showOnTextScreen(() -> text.showButtons(prompt, labels, values));
        } else {
            showOnTextScreen(() -> text.showSpinner(prompt, min, max, GameView.BACK));
        }
        return (Integer) waitForAnswer();
    }

    @Override
    public String readChoice(String prompt, List<String> options) throws QuitGameException {
        checkNotEdt();
        showOnTextScreen(() -> text.showButtons(prompt, options, options));
        return (String) waitForAnswer();
    }

    @Override
    public boolean readYesNo(String prompt) throws QuitGameException {
        checkNotEdt();
        showOnTextScreen(() -> text.showButtons(prompt, List.of("Yes", "No"), List.of(true, false)));
        return (Boolean) waitForAnswer();
    }

    @Override
    public MenuChoice showMainMenu(boolean canResume) throws QuitGameException {
        checkNotEdt();

        // How to Play and the ending of a run both print their text and then go straight
        // back to the menu. In the console the text stays on screen above the menu, but
        // here the menu would cover it before anyone could read it. So if there is
        // unread text, wait for a Continue click first
        if (unreadText) {
            showOnTextScreen(() -> text.showButtons("", List.of("Continue"), List.of(CONTINUE)));
            waitForAnswer();
        }

        SwingUtilities.invokeLater(() -> {
            // a fresh screen for whatever comes after the menu
            text.clear();
            cards.show(screens, MENU_SCREEN);
            this.canResume = canResume;
            menuWaiting = true;
        });
        return (MenuChoice) waitForAnswer();
    }

    @Override
    public void close() {
        SwingUtilities.invokeLater(() -> frame.dispose());
    }

    // ---- helpers for the engine thread ----

    // runs a change to the text screen on the EDT and makes sure it is the one showing
    private void showOnTextScreen(Runnable change) {
        SwingUtilities.invokeLater(() -> {
            change.run();
            cards.show(screens, TEXT_SCREEN);
        });
    }

    // Waits until the player clicks something. If the window was closed, this throws
    // QuitGameException, the same as typing Q in the console
    private Object waitForAnswer() throws QuitGameException {
        Object answer;
        try {
            answer = answers.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QuitGameException();
        }
        if (answer == QUIT) {
            throw new QuitGameException();
        }
        // they answered, so they have had the chance to read what was printed before
        unreadText = false;
        return answer;
    }

    // Waiting on the EDT would freeze the window forever, because the button that
    // would answer can only be clicked on the EDT. So fail straight away instead
    private void checkNotEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("The game engine must not run on the Event Dispatch Thread");
        }
    }

    // ---- building the window and handling clicks, EDT only ----

    private void buildWindow() {
        frame = new JFrame("Expedition");

        // closing the window is the same as choosing Quit, so Main still gets to shut
        // the database down. The window goes straight away rather than waiting for the engine
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                answers.offer(QUIT);
                frame.dispose();
            }
        });

        text = new TextPanel(answers::offer);
        cards = new CardLayout();
        screens = new JPanel(cards);
        screens.setPreferredSize(new Dimension(Assets.WIDTH, Assets.HEIGHT));
        screens.add(new MainMenuPanel(this::menuClicked), MENU_SCREEN);
        screens.add(text, TEXT_SCREEN);

        // the text screen shows first, so anything printed before the menu (like a
        // startup error) is visible
        cards.show(screens, TEXT_SCREEN);

        frame.add(screens);
        frame.pack();
        // Nadia's art is drawn for exactly 1280x720
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Turns a click on the menu screen into the MenuChoice the engine is waiting for.
    // The menu has more buttons than the game has features so far
    private void menuClicked(MainMenuPanel.Choice choice) {
        if (!menuWaiting) {
            return;
        }
        switch (choice) {
            case NEW_GAME: answerMenu(MenuChoice.NEW_GAME); break;
            // the ? button and How to Play open the same instructions
            case HOW_TO_PLAY:
            case HELP: answerMenu(MenuChoice.HOW_TO_PLAY); break;
            case QUIT: answerMenu(MenuChoice.QUIT); break;
            case LOAD_GAME: loadGameClicked(); break;
            case SETTINGS: comingSoon("Settings"); break;
            case HIGH_SCORES: comingSoon("High Scores"); break;
        }
    }

    // one answer per menu visit, so a double click can't start two things
    private void answerMenu(MenuChoice choice) {
        menuWaiting = false;
        answers.offer(choice);
    }

    // The game autosaves at every planet, so Load Game just resumes that save.
    // With no save there is nothing to load, so the player stays on the menu
    private void loadGameClicked() {
        if (canResume) {
            answerMenu(MenuChoice.RESUME);
        } else {
            JOptionPane.showMessageDialog(frame, "There is no saved run yet. The game saves itself each time the ship reaches a planet.",
                    "Load Game", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // the engine isn't asked anything, the player just stays on the menu
    private void comingSoon(String feature) {
        JOptionPane.showMessageDialog(frame, feature + " isn't in the game yet.",
                "Coming soon", JOptionPane.INFORMATION_MESSAGE);
    }
}
