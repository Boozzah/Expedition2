/*
 * InGameMenu
 *
 * The menu that the ☰ button opens during a run, from Nadia's sketch. It sits over the
 * game and swaps between pages:
 * - MENU: the score so far, then Help, Save Game, Save & Quit and Close
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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
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
        void help();

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
    private static final int NOTE_HEIGHT = 80;
    private static final int BUTTON_STEP = 114;
    private static final int SIDE_MARGIN = 40;
    private static final int BOTTOM_MARGIN = 30;

    private static final float TITLE_SIZE = 80f;
    private static final float NOTE_SIZE = 28f;
    private static final float BUTTON_TEXT_SIZE = 56f;

    // the ☰ icon, a square the same size as the help and settings buttons
    private static final int ICON_SIZE = 64;
    private static final int ICON_BAR_INSET = 16;
    private static final int ICON_BAR_THICKNESS = 6;

    private final BufferedImage buttonImage = Assets.load("menu-button.png");
    private final Font buttonFont = Assets.pixelta(BUTTON_TEXT_SIZE);

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
        menu.setLocationRelativeTo(owner);
        return menu;
    }

    // the gear on the main menu
    static InGameMenu settings(JFrame owner, Runnable quitToDesktop) {
        InGameMenu menu = new InGameMenu(owner);
        menu.showPage("SETTINGS", "Sound settings are coming in a later version.", List.of(
                new Option("QUIT TO DESKTOP", quitToDesktop),
                new Option("CLOSE", menu::dispose)));
        menu.setLocationRelativeTo(owner);
        return menu;
    }

    private void showMain(int score, Actions actions) {
        // Telling the player what saving keeps: the save is made when the ship reaches a
        // planet, so anything since then (like oil being hauled) isn't in it
        showPage("MENU", "Score so far: " + score, List.of(
                new Option("HELP", actions::help),
                new Option("SAVE GAME", () -> showSaveName(score, actions)),
                new Option("SAVE & QUIT", () -> showSaveAndQuit(score, actions)),
                new Option("CLOSE", this::dispose)));
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

        JPanel panel = showPage("SAVE GAME", "Name this save:", List.of(
                new Option("SAVE", save),
                new Option("CLOSE", () -> showMain(score, actions))));
        name.setBounds(SIDE_MARGIN, NOTE_TOP + NOTE_HEIGHT - 10, WIDTH - 2 * SIDE_MARGIN, 56);
        panel.add(name);
        // pushes the buttons down to make room for the name box
        for (Component c : panel.getComponents()) {
            if (c instanceof ImageButton) {
                c.setLocation(c.getX(), c.getY() + 60);
            }
        }
        panel.setPreferredSize(new Dimension(WIDTH, panel.getPreferredSize().height + 60));
        pack();
        setLocationRelativeTo(getOwner());
        name.requestFocusInWindow();
    }

    // Swaps the window to a page with a title, a line of text and a column of buttons.
    // Returns the panel so a page can add its own parts
    private JPanel showPage(String title, String note, List<Option> options) {
        JPanel panel = new JPanel(null);   // placed by hand, like the main menu
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(TEXT, 3));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(Assets.pixelta(TITLE_SIZE));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, TITLE_TOP, WIDTH, (int) TITLE_SIZE);
        panel.add(titleLabel);

        JLabel noteLabel = new JLabel("<html><div style='text-align:center'>" + note + "</div></html>", SwingConstants.CENTER);
        noteLabel.setFont(Assets.pixelta(NOTE_SIZE));
        noteLabel.setForeground(TEXT);
        noteLabel.setBounds(SIDE_MARGIN, NOTE_TOP, WIDTH - 2 * SIDE_MARGIN, NOTE_HEIGHT);
        panel.add(noteLabel);

        int x = (WIDTH - buttonImage.getWidth()) / 2;
        int top = NOTE_TOP + NOTE_HEIGHT + 10;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            ImageButton b = new ImageButton(buttonImage, option.label(), buttonFont, TEXT);
            b.setLocation(x, top + i * BUTTON_STEP);
            b.addActionListener(e -> option.action().run());
            panel.add(b);
        }

        int height = top + options.size() * BUTTON_STEP + BOTTOM_MARGIN;
        panel.setPreferredSize(new Dimension(WIDTH, height));
        setContentPane(panel);
        pack();
        setLocationRelativeTo(getOwner());
        return panel;
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
