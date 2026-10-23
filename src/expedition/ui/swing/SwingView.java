/*
 * SwingView
 *
 * The window version of GameView. The window has one screen for each of Nadia's
 * drawings and swaps between them with a CardLayout:
 * - the main menu, the high scores, Load Game, How to Play and the story at the start
 * - the game screens, which come in over the next few files
 * - the text screen, TextPanel, for any question that has no screen of its own
 * On top of the screens, in the frame's layered pane, are the ☰ button, the notices that
 * print() shows, and the overlay the popups sit on.
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
 *
 * Leaving a run from the in-game menu (the ☰ button, or Esc) works the same way with a
 * MENU marker. The question the engine is on throws ReturnToMenuException, which
 * GameEngine catches and goes back to the main menu.
 *
 * Each screen only answers once per question (see GameScreen), and any answer still in the
 * queue from an earlier question is dropped before a new one is asked, so a fast double
 * click can never answer the next question by accident.
 */
package expedition.ui.swing;

import expedition.engine.HighScore;
import expedition.exception.QuitGameException;
import expedition.exception.ReturnToMenuException;
import expedition.io.SaveSlot;
import expedition.ui.GameView;
import expedition.ui.MenuChoice;
import expedition.ui.RunControls;
import expedition.ui.screen.Popup;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class SwingView implements GameView {

    // names of the screens in the CardLayout
    private static final String MENU_SCREEN = "menu";
    private static final String SCORES_SCREEN = "scores";
    private static final String TEXT_SCREEN = "text";
    private static final String LORE_SCREEN = "lore";
    private static final String HOW_TO_PLAY_SCREEN = "howToPlay";
    private static final String LOAD_SCREEN = "load";

    // the screens outside a run, which have no ☰ button
    private static final Set<String> MENU_SCREENS = Set.of(MENU_SCREEN, SCORES_SCREEN, HOW_TO_PLAY_SCREEN, LOAD_SCREEN);

    // put in the queue when the window is closed, instead of a real answer
    private static final Object QUIT = new Object();

    // put in the queue when the player picks Main Menu in the in-game menu
    private static final Object MENU = new Object();

    // where the ☰ button sits, in from the top right corner. Far enough left to
    // clear the text screen's scroll bar
    private static final int MENU_BUTTON_MARGIN = 24;

    // the answer from the Continue button, see showMainMenu
    private static final Object CONTINUE = new Object();

    // the answer from the Back button on the high-score screen
    private static final Object BACK_TO_MENU = new Object();

    // the answer from a popup's DONE button
    private static final Object POPUP_DONE = new Object();

    // the in-game menu's Help window: the book plus a Close button under it
    private static final int HELP_CLOSE_GAP = 10;

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
    private HighScoresPanel highScores;
    private LoreScreen lore;
    private HowToPlayScreen howToPlay;
    private LoadScreen load;
    private ImageButton menuButton;
    private NoticeBar notices;
    private Overlay overlay;

    // Only touched on the EDT. The screen showing, so print() knows whether to show a notice
    private String currentScreen = TEXT_SCREEN;

    // The run being played, for the in-game menu's score and Save Game. Set by the engine
    // thread and read on the EDT, so volatile. null outside a run
    private volatile RunControls controls;

    // How to Play's pages, kept for the in-game menu's Help. Set by the engine thread
    private volatile List<String> helpPages = List.of();

    // Only touched on the EDT. True while the engine is waiting on the main menu, so
    // a menu click only counts when the engine is actually asking for one
    private boolean menuWaiting = false;

    // Only touched on the EDT. Same idea for the Back button on the high-score screen,
    // so a double click can't leave a second Back in the queue for the menu to read
    private boolean scoresWaiting = false;

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

    // Everything printed goes in the text screen's log. On any other screen it also shows
    // as a notice near the bottom, rather than switching away from the screen
    @Override
    public void print(String line) {
        SwingUtilities.invokeLater(() -> {
            text.append(line);
            if (TEXT_SCREEN.equals(currentScreen)) {
                unreadText = true;
            } else {
                notices.add(line);
            }
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
        return (Integer) waitForGameAnswer();
    }

    @Override
    public String readChoice(String prompt, List<String> options) throws QuitGameException {
        checkNotEdt();
        showOnTextScreen(() -> text.showButtons(prompt, options, options));
        return (String) waitForGameAnswer();
    }

    @Override
    public boolean readYesNo(String prompt) throws QuitGameException {
        checkNotEdt();
        showOnTextScreen(() -> text.showButtons(prompt, List.of("Yes", "No"), List.of(true, false)));
        return (Boolean) waitForGameAnswer();
    }

    @Override
    public MenuChoice showMainMenu(boolean canResume) throws QuitGameException {
        checkNotEdt();

        // How to Play and the ending of a run both print their text and then go straight
        // back to the menu. In the console the text stays on screen above the menu, but
        // here the menu would cover it before anyone could read it. So if there is
        // unread text, wait for a Continue click first. Main Menu from the ☰ menu counts as Continue
        if (unreadText) {
            unreadText = false;
            showOnTextScreen(() -> text.showButtons("", List.of("Continue"), List.of(CONTINUE)));
            waitForAnswer();
        }

        SwingUtilities.invokeLater(() -> {
            // a fresh screen for whatever comes after the menu
            text.clear();
            showScreen(MENU_SCREEN);
            this.canResume = canResume;
            menuWaiting = true;
        });
        return (MenuChoice) waitIgnoringMenu();
    }

    // shows the scores screen and waits until the player clicks Back
    @Override
    public void showHighScores(List<HighScore> scores) throws QuitGameException {
        checkNotEdt();
        SwingUtilities.invokeLater(() -> {
            highScores.showScores(scores);
            showScreen(SCORES_SCREEN);
            scoresWaiting = true;
        });
        waitIgnoringMenu();
    }

    @Override
    public void close() {
        SwingUtilities.invokeLater(() -> frame.dispose());
    }

    @Override
    public void runStarted(RunControls controls) {
        this.controls = controls;
    }

    @Override
    public void runEnded() {
        this.controls = null;
        SwingUtilities.invokeLater(() -> menuButton.setVisible(false));
    }

    @Override
    public void showLore(String title, String story) throws QuitGameException {
        ask(lore, LORE_SCREEN, () -> lore.show(title, story), true);
    }

    @Override
    public void showHowToPlay(List<String> pages) throws QuitGameException {
        helpPages = pages;
        ask(howToPlay, HOW_TO_PLAY_SCREEN, () -> howToPlay.show(pages), false);
    }

    @Override
    public SaveSlot chooseSave(List<SaveSlot> slots) throws QuitGameException {
        Object answer = ask(load, LOAD_SCREEN, () -> load.show(slots), false);
        return (answer instanceof SaveSlot slot) ? slot : null;
    }

    // the popup goes over whatever screen is showing, and the engine waits for DONE
    @Override
    public void showPopup(Popup popup) throws QuitGameException {
        checkNotEdt();
        dropOldAnswers();
        SwingUtilities.invokeLater(() -> {
            PopupBox box = new PopupBox(popup, () -> {
                overlay.close();
                answers.offer(POPUP_DONE);
            });
            overlay.show(box);
            box.shakeIfHazard();
        });
        waitForGameAnswer();
    }

    // ---- helpers for the engine thread ----

    // Shows a screen for a question and waits for its answer. fill runs on the EDT first and
    // puts this question's details on the screen. inRun is false for the screens outside a
    // run, where Main Menu from the ☰ menu has nothing to leave
    private Object ask(GameScreen screen, String name, Runnable fill, boolean inRun) throws QuitGameException {
        checkNotEdt();
        dropOldAnswers();
        SwingUtilities.invokeLater(() -> {
            fill.run();
            screen.startWaiting();
            showScreen(name);
        });
        return inRun ? waitForGameAnswer() : waitIgnoringMenu();
    }

    // A click left in the queue from an earlier question can't answer this one. QUIT and
    // MENU stay, they mean the player wants out whatever is being asked
    private void dropOldAnswers() {
        answers.removeIf(a -> a != QUIT && a != MENU);
    }

    // runs a change to the text screen on the EDT and makes sure it is the one showing
    private void showOnTextScreen(Runnable change) {
        SwingUtilities.invokeLater(() -> {
            change.run();
            showScreen(TEXT_SCREEN);
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

    // AI assisted - Claude suggested a MENU marker in the answer queue, the same way QUIT works
    // waitForAnswer for a question asked during a run, where Main Menu in the
    // in-game menu leaves the run
    private Object waitForGameAnswer() throws QuitGameException {
        Object answer = waitForAnswer();
        if (answer == MENU) {
            throw new ReturnToMenuException();
        }
        return answer;
    }

    // For the menu and score screens, which are already out of the run. A MENU left in
    // the queue from a double click would have nothing to leave, so it's skipped
    private Object waitIgnoringMenu() throws QuitGameException {
        Object answer;
        do {
            answer = waitForAnswer();
        } while (answer == MENU);
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
                quit();
            }
        });

        text = new TextPanel(answers::offer);
        cards = new CardLayout();
        screens = new JPanel(cards);
        screens.setPreferredSize(new Dimension(Assets.WIDTH, Assets.HEIGHT));
        screens.add(new MainMenuPanel(this::menuClicked), MENU_SCREEN);
        screens.add(text, TEXT_SCREEN);
        highScores = new HighScoresPanel(this::backFromScores);
        screens.add(highScores, SCORES_SCREEN);
        lore = new LoreScreen(answers::offer);
        screens.add(lore, LORE_SCREEN);
        howToPlay = new HowToPlayScreen(answers::offer, this::openSettings);
        screens.add(howToPlay, HOW_TO_PLAY_SCREEN);
        load = new LoadScreen(answers::offer, this::openSettings);
        screens.add(load, LOAD_SCREEN);

        frame.add(screens);
        addMenuButton();

        notices = new NoticeBar();
        frame.getLayeredPane().add(notices, JLayeredPane.PALETTE_LAYER);
        overlay = new Overlay();
        frame.getLayeredPane().add(overlay, JLayeredPane.MODAL_LAYER);

        // the text screen shows first, so anything printed before the menu (like a
        // startup error) is visible
        showScreen(TEXT_SCREEN);

        frame.pack();
        // Nadia's art is drawn for exactly 1280x720
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // AI assisted - Claude suggested putting the ☰ button in the layered pane and binding Esc on the root pane
    // The ☰ button floats over the top right of every in-game screen, in the frame's
    // layered pane so a screen doesn't need to make room for it. Esc opens it too
    private void addMenuButton() {
        menuButton = new ImageButton(InGameMenu.icon(), "Menu (Esc)");
        menuButton.setLocation(Assets.WIDTH - MENU_BUTTON_MARGIN - menuButton.getWidth(), MENU_BUTTON_MARGIN);
        menuButton.addActionListener(e -> openInGameMenu());
        frame.getLayeredPane().add(menuButton, JLayeredPane.PALETTE_LAYER);

        JComponent root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "inGameMenu");
        root.getActionMap().put("inGameMenu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openInGameMenu();
            }
        });
    }

    // Swaps to a screen. The ☰ button only shows during a run, the menu screens are
    // already out of it. Notices from the screen before are cleared
    private void showScreen(String name) {
        if (!name.equals(currentScreen)) {
            notices.clear();
        }
        currentScreen = name;
        cards.show(screens, name);
        menuButton.setVisible(controls != null && !MENU_SCREENS.contains(name));
    }

    private void openInGameMenu() {
        RunControls run = controls;
        if (!menuButton.isVisible() || run == null) {
            return;
        }
        InGameMenu.inGame(frame, run.score(), new InGameMenu.Actions() {
            @Override
            public void help() {
                openHelp();
            }

            @Override
            public String save(String name) {
                return run.save(name);
            }

            @Override
            public String saveAndQuit() {
                return run.saveAndQuit();
            }

            @Override
            public void quitToMenu() {
                answers.offer(MENU);
            }

            @Override
            public void quitToDesktop() {
                quit();
            }
        }).setVisible(true);
    }

    // How to Play in a window over the game, from the in-game menu's Help
    private void openHelp() {
        JDialog help = new JDialog(frame, true);
        help.setUndecorated(true);
        JPanel panel = new JPanel(null);
        panel.setBackground(Draw.NAVY);
        BookPanel book = new BookPanel();
        book.setLocation(0, 0);
        book.show(helpPages);
        panel.add(book);
        ImageButton close = Draw.smallButton("CLOSE");
        close.setLocation((BookPanel.WIDTH - close.getWidth()) / 2, BookPanel.HEIGHT + HELP_CLOSE_GAP);
        close.addActionListener(e -> help.dispose());
        panel.add(close);
        panel.setPreferredSize(new Dimension(BookPanel.WIDTH, BookPanel.HEIGHT + 2 * HELP_CLOSE_GAP + close.getHeight()));
        help.setContentPane(panel);
        help.pack();
        help.setLocationRelativeTo(frame);
        help.setVisible(true);
    }

    // the gear on the menu screens
    private void openSettings() {
        InGameMenu.settings(frame, this::quit).setVisible(true);
    }

    // a message over the screen that the engine isn't waiting on, like Load Game with nothing saved
    private void showMessage(String message) {
        overlay.show(new PopupBox(Popup.info(message), overlay::close));
    }

    // the window goes straight away rather than waiting for the engine
    private void quit() {
        answers.offer(QUIT);
        frame.dispose();
    }

    // Turns a click on the menu screen into the MenuChoice the engine is waiting for
    private void menuClicked(MainMenuPanel.Choice choice) {
        // the settings don't need the engine, so they open even while it is busy
        if (choice == MainMenuPanel.Choice.SETTINGS) {
            openSettings();
            return;
        }
        if (!menuWaiting) {
            return;
        }
        switch (choice) {
            case NEW_GAME: answerMenu(MenuChoice.NEW_GAME); break;
            case HOW_TO_PLAY: answerMenu(MenuChoice.HOW_TO_PLAY); break;
            case QUIT: answerMenu(MenuChoice.QUIT); break;
            case LOAD_GAME: loadGameClicked(); break;
            case HIGH_SCORES: answerMenu(MenuChoice.HIGH_SCORES); break;
            default: break;
        }
    }

    // one answer per menu visit, so a double click can't start two things
    // EDT only. The first Back click answers, any more are ignored until the screen shows again
    private void backFromScores() {
        if (!scoresWaiting) {
            return;
        }
        scoresWaiting = false;
        answers.offer(BACK_TO_MENU);
    }

    private void answerMenu(MenuChoice choice) {
        menuWaiting = false;
        answers.offer(choice);
    }

    // Load Game lists the saves to pick from. With no save there is nothing to load,
    // so the player stays on the menu
    private void loadGameClicked() {
        if (canResume) {
            answerMenu(MenuChoice.RESUME);
        } else {
            showMessage("There is no saved run yet. The game saves itself each time the ship reaches a planet, "
                    + "and you can save from the menu during a run.");
        }
    }
}
