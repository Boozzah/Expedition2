/*
 * InGameMenu
 *
 * The menu that the ☰ button opens during a run, from Nadia's sketch. It sits over the
 * game and swaps between pages:
 * - MENU: the score so far, then Help, Save Game, Save & Quit and Close
 * - HELP: a button for each topic, which opens a page explaining that part of the game
 * - SAVE & QUIT: Quit to Menu, Quit to Desktop, or Close to go back
 * - SAVE GAME: a box to type the save's name in
 * The gear on the main menu opens the same kind of window with the Settings page, which
 * for now only has Quit to Desktop, sound settings can go there later.
 *
 * Like MainMenuPanel it doesn't decide what the buttons do, it calls the Actions SwingView
 * gave it. It is modal, so nothing behind it can be clicked while it's open. Esc closes it.
 *
 * There is no art for the ☰ button yet, so icon() draws one. Swap it for
 * Nadia's image once there is one.
 */
package expedition.ui.swing;

import expedition.io.SaveStore;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

class InGameMenu extends JDialog {

    // what the in-game menu's buttons do, filled in by SwingView
    interface Actions {
        // the Help topics, title to text
        Map<String, String> helpTopics();

        // returns a message saying how the save went
        String save(String name);

        String saveAndQuit();

        void quitToMenu();

        void quitToDesktop();
    }

    // one button on a page
    private record Option(String label, Runnable action) {
    }

    // the same navy and light blue as the text screen and the menu button text
    private static final Color BACKGROUND = Draw.NAVY;
    private static final Color TEXT = Draw.LIGHT_BLUE;

    private static final int WIDTH = 560;
    private static final int TITLE_TOP = 30;
    private static final int NOTE_TOP = 120;
    private static final int NOTE_GAP = 20;
    private static final int BUTTON_STEP = 114;
    private static final int SIDE_MARGIN = 40;
    // the room the save name box takes under the text
    private static final int NAME_ROOM = 70;
    private static final int BOTTOM_MARGIN = 30;

    private static final float TITLE_SIZE = 80f;
    private static final float NOTE_SIZE = 28f;
    private static final float BUTTON_TEXT_SIZE = 56f;

    // the Help topics are smaller buttons, two to a row
    private static final int TOPIC_W = 230;
    private static final int TOPIC_H = 62;
    private static final int TOPIC_GAP = 14;
    private static final float TOPIC_TEXT_SIZE = 28f;

    // the ☰ icon, a square the same size as the help and settings buttons
    private static final int ICON_SIZE = 64;
    private static final int ICON_BAR_INSET = 16;
    private static final int ICON_BAR_THICKNESS = 6;

    private final BufferedImage buttonImage = Assets.load("menu-button.png");
    private final Font buttonFont = Assets.pixelta(BUTTON_TEXT_SIZE);

    // where the last page's text ended, for a page that adds its own parts under it
    private int noteBottom;

    private InGameMenu(JFrame owner) {
        super(owner, true);
        setUndecorated(true);

        // Esc closes the menu, the same as Close
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    // the ☰ menu during a run
    static InGameMenu inGame(JFrame owner, int score, Actions actions) {
        InGameMenu menu = new InGameMenu(owner);
        menu.showMain(score, actions);
        return menu;
    }

    // the gear on the main menu
    static InGameMenu settings(JFrame owner, Runnable quitToDesktop) {
        InGameMenu menu = new InGameMenu(owner);
        menu.showPage("SETTINGS", "Sound settings are coming in a later version.", List.of(
                new Option("QUIT TO DESKTOP", quitToDesktop),
                new Option("CLOSE", menu::dispose)));
        return menu;
    }

    private void showMain(int score, Actions actions) {
        // Telling the player what saving keeps: the save is made when the ship reaches a
        // planet, so anything since then (like oil being hauled) isn't in it
        showPage("MENU", "Score so far: " + score, List.of(
                new Option("HELP", () -> showTopics(score, actions)),
                new Option("SAVE GAME", () -> showSaveName(score, actions)),
                new Option("SAVE & QUIT", () -> showSaveAndQuit(score, actions)),
                new Option("CLOSE", this::dispose)));
    }

    // AI assisted - Claude Code made Help a page per topic
    // The Help topics two to a row, then BACK to the menu
    private void showTopics(int score, Actions actions) {
        JPanel panel = buildPage("HELP", "Pick a topic.", List.of());
        BufferedImage topicImage = Assets.scaled(buttonImage, TOPIC_W, TOPIC_H);
        Font topicFont = Assets.pixelta(TOPIC_TEXT_SIZE);
        List<String> titles = new ArrayList<>(actions.helpTopics().keySet());
        int left = (WIDTH - 2 * TOPIC_W - TOPIC_GAP) / 2;
        int top = noteBottom + NOTE_GAP;
        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);
            ImageButton b = new ImageButton(topicImage, title, topicFont, TEXT);
            b.setLocation(left + (i % 2) * (TOPIC_W + TOPIC_GAP), top + (i / 2) * (TOPIC_H + TOPIC_GAP));
            b.addActionListener(e -> showPage(title, actions.helpTopics().get(title),
                    List.of(new Option("BACK", () -> showTopics(score, actions)))));
            panel.add(b);
        }
        int rows = (titles.size() + 1) / 2;
        int backTop = top + rows * (TOPIC_H + TOPIC_GAP) + 10;
        ImageButton back = new ImageButton(buttonImage, "BACK", buttonFont, TEXT);
        back.setLocation((WIDTH - buttonImage.getWidth()) / 2, backTop);
        back.addActionListener(e -> showMain(score, actions));
        panel.add(back);
        panel.setPreferredSize(new Dimension(WIDTH, backTop + BUTTON_STEP + BOTTOM_MARGIN));
        display(panel);
    }

    private void showSaveAndQuit(int score, Actions actions) {
        showPage("SAVE & QUIT", "The game saves when the ship reaches a planet. Loading carries on from there.", List.of(
                new Option("QUIT TO MENU", () -> {
                    actions.saveAndQuit();
                    dispose();
                    actions.quitToMenu();
                }),
                new Option("QUIT TO DESKTOP", () -> {
                    actions.saveAndQuit();
                    dispose();
                    actions.quitToDesktop();
                }),
                new Option("CLOSE", () -> showMain(score, actions))));
    }

    // a box to type the name in, then SAVE shows how it went
    private void showSaveName(int score, Actions actions) {
        JTextField name = new JTextField();
        name.setFont(Assets.pixelta(36f));
        name.setForeground(Color.WHITE);
        name.setBackground(new Color(0x24384F));
        name.setCaretColor(Color.WHITE);
        name.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TEXT, 2), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        name.setHorizontalAlignment(SwingConstants.CENTER);

        Runnable save = () -> {
            String typed = name.getText().trim();
            if (typed.isEmpty() || typed.length() > SaveStore.MAX_NAME_LENGTH) {
                name.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Draw.DANGER, 2), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
                name.setToolTipText("Give the save a name of up to " + SaveStore.MAX_NAME_LENGTH + " letters");
                return;
            }
            String result = actions.save(typed);
            showPage("SAVED", result, List.of(new Option("CLOSE", () -> showMain(score, actions))));
        };
        // Enter saves too
        name.addActionListener(e -> save.run());

        JPanel panel = buildPage("SAVE GAME", "Name this save:", List.of(
                new Option("SAVE", save),
                new Option("CLOSE", () -> showMain(score, actions))));
        name.setBounds(SIDE_MARGIN, noteBottom + 10, WIDTH - 2 * SIDE_MARGIN, 56);
        panel.add(name);
        // pushes the buttons down to make room for the name box
        for (Component c : panel.getComponents()) {
            if (c instanceof ImageButton) {
                c.setLocation(c.getX(), c.getY() + NAME_ROOM);
            }
        }
        panel.setPreferredSize(new Dimension(WIDTH, panel.getPreferredSize().height + NAME_ROOM));
        display(panel);
        name.requestFocusInWindow();
    }

    // AI assisted - Claude Code fixed the text being cut off by measuring the wrapped lines
    // Swaps the window to a page with a title, some text and a column of buttons
    private void showPage(String title, String note, List<Option> options) {
        display(buildPage(title, note, options));
    }

    // A page with a title, some text and a column of buttons. The text gets as much height
    // as it needs, so a long message isn't cut off. It isn't shown yet, so a page can add
    // its own parts before display() sizes the window to it
    private JPanel buildPage(String title, String note, List<Option> options) {
        JPanel panel = new JPanel(null);   // placed by hand, like the main menu
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(TEXT, 3));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(fitted(title, TITLE_SIZE, WIDTH - 2 * SIDE_MARGIN));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, TITLE_TOP, WIDTH, (int) TITLE_SIZE);
        panel.add(titleLabel);

        // the text is wrapped to the width in Pixelta, and gets as many lines as it needs
        int noteWidth = WIDTH - 2 * SIDE_MARGIN;
        Font noteFont = Assets.pixelta(NOTE_SIZE);
        int lines = Math.max(1, Draw.wrap(note, panel.getFontMetrics(noteFont), noteWidth).size());
        int noteHeight = lines * Draw.lineHeight(noteFont);
        JComponent noteText = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Draw.paragraph((Graphics2D) g, note, noteFont, TEXT, new Rectangle(0, 0, getWidth(), getHeight()));
            }
        };
        noteText.setBounds(SIDE_MARGIN, NOTE_TOP, noteWidth, noteHeight);
        panel.add(noteText);
        noteBottom = NOTE_TOP + noteHeight;

        int x = (WIDTH - buttonImage.getWidth()) / 2;
        int top = noteBottom + NOTE_GAP;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            ImageButton b = new ImageButton(buttonImage, option.label(), buttonFont, TEXT);
            b.setLocation(x, top + i * BUTTON_STEP);
            b.addActionListener(e -> option.action().run());
            panel.add(b);
        }

        int height = top + options.size() * BUTTON_STEP + BOTTOM_MARGIN;
        panel.setPreferredSize(new Dimension(WIDTH, height));
        return panel;
    }

    // Shows a finished page and centres the window over the game once it has its final
    // size. Sizing a page and then growing it left the HELP page hanging off the bottom
    private void display(JPanel panel) {
        setContentPane(panel);
        pack();
        Rectangle game = getOwner().getBounds();
        int x = game.x + (game.width - getWidth()) / 2;
        int y = Math.max(game.y, game.y + (game.height - getHeight()) / 2);
        setLocation(x, y);
    }

    // the title font, a size down at a time until the title fits the width
    private static Font fitted(String title, float size, int width) {
        Font font = Assets.pixelta(size);
        JLabel probe = new JLabel(title);
        probe.setFont(font);
        while (probe.getPreferredSize().width > width && size > 30f) {
            size -= 4f;
            font = Assets.pixelta(size);
            probe.setFont(font);
        }
        return font;
    }

    // Three light bars on a navy square with a light border, drawn with square
    // corners and no smoothing so it sits with the pixel art
    static BufferedImage icon() {
        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
        g.setColor(TEXT);
        g.setStroke(new BasicStroke(3));
        g.drawRect(1, 1, ICON_SIZE - 3, ICON_SIZE - 3);

        int barWidth = ICON_SIZE - 2 * ICON_BAR_INSET;
        int gap = (ICON_SIZE - 2 * ICON_BAR_INSET - 3 * ICON_BAR_THICKNESS) / 2;
        for (int i = 0; i < 3; i++) {
            int y = ICON_BAR_INSET + i * (ICON_BAR_THICKNESS + gap);
            g.fillRect(ICON_BAR_INSET, y, barWidth, ICON_BAR_THICKNESS);
        }
        g.dispose();
        return image;
    }
}
