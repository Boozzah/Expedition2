/*
 * InGameMenu
 *
 * The pause menu that the ☰ button opens during a run. It sits over the game
 * and has three of the long menu buttons: Main Menu, Quit Game and Resume.
 * Esc closes it, the same as Resume.
 *
 * Like MainMenuPanel it doesn't decide what the buttons do, it hands the choice
 * back to SwingView. It is modal, so nothing behind it can be clicked while it's open.
 *
 * There is no art for the ☰ button yet, so icon() draws one. Swap it for
 * Nadia's image once there is one.
 */
package expedition.ui.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

// AI assisted - Claude suggested making the pause menu a modal JDialog and drawing the ☰ icon in code until there is art for it
class InGameMenu extends JDialog {

    enum Choice { MAIN_MENU, QUIT, RESUME }

    // the same navy and light blue as the text screen and the menu button text
    private static final Color BACKGROUND = new Color(0x030F1F);
    private static final Color TEXT = new Color(0xA6CEE6);

    private static final int WIDTH = 560;
    private static final int HEIGHT = 610;
    private static final int TITLE_TOP = 30;
    private static final int NOTE_TOP = 115;
    private static final int NOTE_HEIGHT = 105;
    private static final int BUTTONS_TOP = 228;
    private static final int BUTTON_STEP = 120;
    private static final int SIDE_MARGIN = 40;

    private static final float TITLE_SIZE = 80f;
    private static final float NOTE_SIZE = 26f;
    private static final float BUTTON_TEXT_SIZE = 60f;

    // the ☰ icon, a square the same size as the help and settings buttons
    private static final int ICON_SIZE = 64;
    private static final int ICON_BAR_INSET = 16;
    private static final int ICON_BAR_THICKNESS = 6;

    // Telling the player what leaving costs them: the save is made when the ship
    // reaches a planet, so anything since then (like oil being hauled) is lost
    private static final String NOTE = "<html><div style='text-align:center'>The game saves when the ship reaches a planet. "
            + "Leaving now goes back to that save.</div></html>";

    InGameMenu(JFrame owner, Consumer<Choice> onChoice) {
        super(owner, true);
        setUndecorated(true);

        JPanel panel = new JPanel(null);   // placed by hand, like the main menu
        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createLineBorder(TEXT, 3));

        JLabel title = new JLabel("PAUSED", SwingConstants.CENTER);
        title.setFont(Assets.pixelta(TITLE_SIZE));
        title.setForeground(Color.WHITE);
        title.setBounds(0, TITLE_TOP, WIDTH, (int) TITLE_SIZE);
        panel.add(title);

        JLabel note = new JLabel(NOTE, SwingConstants.CENTER);
        note.setFont(Assets.pixelta(NOTE_SIZE));
        note.setForeground(TEXT);
        note.setBounds(SIDE_MARGIN, NOTE_TOP, WIDTH - 2 * SIDE_MARGIN, NOTE_HEIGHT);
        panel.add(note);

        BufferedImage buttonImage = Assets.load("menu-button.png");
        Font buttonFont = Assets.pixelta(BUTTON_TEXT_SIZE);
        String[] labels = {"MAIN MENU", "QUIT GAME", "RESUME"};
        Choice[] choices = {Choice.MAIN_MENU, Choice.QUIT, Choice.RESUME};
        int x = (WIDTH - buttonImage.getWidth()) / 2;
        for (int i = 0; i < labels.length; i++) {
            Choice choice = choices[i];
            ImageButton b = new ImageButton(buttonImage, labels[i], buttonFont, TEXT);
            b.setLocation(x, BUTTONS_TOP + i * BUTTON_STEP);
            // the menu closes first, so the choice runs with the game showing again
            b.addActionListener(e -> {
                dispose();
                onChoice.accept(choice);
            });
            panel.add(b);
        }

        // Esc is the same as Resume
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "resume");
        panel.getActionMap().put("resume", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setContentPane(panel);
        pack();
        setLocationRelativeTo(owner);
    }

    // Three light bars on a navy square with a light border, drawn with square
    // corners and no smoothing so it sits with the pixel art
    static BufferedImage icon() {
        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
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
