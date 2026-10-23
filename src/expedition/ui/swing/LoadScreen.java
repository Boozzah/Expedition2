/*
 * LoadScreen
 *
 * Load Game, from Nadia's example: the saved runs in a scrolling grey list over the
 * planet, newest at the top, each with when it was last played and a load button.
 * Clicking a row loads it, BACK goes back to the main menu.
 */
package expedition.ui.swing;

import expedition.io.SaveSlot;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;

class LoadScreen extends GameScreen {

    static final Object BACK = new Object();

    // the list art, and the rows' area inside it
    private static final int LIST_X = 350;
    private static final int LIST_Y = 70;
    private static final int LIST_W = 568;
    private static final int LIST_H = 578;
    private static final int ROWS_X = 372;
    private static final int ROWS_Y = 86;
    private static final int ROWS_W = 520;
    private static final int ROWS_H = 540;

    private static final int ROW_W = 492;
    private static final int ROW_H = 76;
    private static final int ROW_GAP = 12;
    private static final Color ROW = new Color(0x6F7C88);
    private static final Color ROW_EDGE = new Color(0x4F5B67);
    private static final Color ROW_HOVER = new Color(0x7D8A96);
    private static final Color DATE_TEXT = new Color(0x3E4A56);

    private static final int BACK_X = 1117;
    private static final int BACK_Y = 622;
    private static final int GEAR_X = 1104;
    private static final int GEAR_Y = 33;

    private final BufferedImage background = Assets.cached("menu-background.png");
    private final BufferedImage list = Assets.scaled(Assets.cached("ui/load-list.png"), LIST_W, LIST_H);
    private final BufferedImage loadIcon = Assets.scaled(Assets.cached("ui/load-button.png"), 62, 58);
    private final Font nameFont = Assets.pixelta(40f);
    private final Font dateFont = Assets.pixelta(22f);
    private final JPanel rows = new JPanel(null);

    LoadScreen(Consumer<Object> answers, Runnable openSettings) {
        super(answers);

        rows.setOpaque(false);
        JScrollPane scroll = new JScrollPane(rows, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBounds(ROWS_X, ROWS_Y, ROWS_W, ROWS_H);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_H / 2);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBar());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(14, 0));
        add(scroll);

        ImageButton back = Draw.smallButton("BACK");
        back.setLocation(BACK_X, BACK_Y);
        back.addActionListener(e -> answer(BACK));
        add(back);

        ImageButton gear = new ImageButton(Assets.load("settings-button.png"), "Settings");
        gear.setLocation(GEAR_X, GEAR_Y);
        gear.addActionListener(e -> openSettings.run());
        add(gear);
    }

    // EDT only. One row per save, the newest first as they come from the database
    void show(List<SaveSlot> slots) {
        rows.removeAll();
        for (int i = 0; i < slots.size(); i++) {
            Row row = new Row(slots.get(i));
            row.setBounds(0, i * (ROW_H + ROW_GAP), ROW_W, ROW_H);
            rows.add(row);
        }
        rows.setPreferredSize(new Dimension(ROW_W, Math.max(0, slots.size() * (ROW_H + ROW_GAP) - ROW_GAP)));
        rows.revalidate();
        rows.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        paintBackground(g2, background);
        g2.drawImage(list, LIST_X, LIST_Y, null);
        g2.dispose();
    }

    // one save in the list. The whole row is the button, the load icon just shows it
    private class Row extends JComponent {
        private final SaveSlot slot;
        private boolean hover = false;

        Row(SaveSlot slot) {
            this.slot = slot;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Load " + slot.name() + ", planet " + slot.planetsReached());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    answer(slot);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(hover ? ROW_HOVER : ROW);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(ROW_EDGE);
            g2.fillRect(0, getHeight() - 6, getWidth(), 6);

            int iconX = getWidth() - loadIcon.getWidth() - 10;
            g2.drawImage(loadIcon, iconX, (getHeight() - loadIcon.getHeight()) / 2, null);

            String when = "Last Played: " + slot.lastPlayedText();
            Draw.smoothText(g2);
            g2.setFont(dateFont);
            FontMetrics dateMetrics = g2.getFontMetrics();
            int dateX = iconX - 16 - dateMetrics.stringWidth(when);
            g2.setColor(DATE_TEXT);
            g2.drawString(when, dateX, getHeight() / 2 + dateMetrics.getAscent() / 2 - 2);

            // a long name is cut short so it never runs into the date
            g2.setFont(nameFont);
            FontMetrics nameMetrics = g2.getFontMetrics();
            String name = slot.name();
            while (nameMetrics.stringWidth(name) > dateX - 30 && name.length() > 1) {
                name = name.substring(0, name.length() - 1);
            }
            if (!name.equals(slot.name())) {
                name = name.substring(0, Math.max(1, name.length() - 2)) + "..";
            }
            g2.setColor(Color.WHITE);
            Draw.string(g2, name, 14, getHeight() / 2 + nameMetrics.getAscent() / 2 - 6);
            g2.dispose();
        }
    }

    // the narrow dark scroll bar down the right of Nadia's list
    private static class ThinScrollBar extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(0x4F5B67);
            trackColor = new Color(0, 0, 0, 0);
        }

        // no arrows at the ends, just the bar
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return noButton();
        }

        private JButton noButton() {
            JButton none = new JButton();
            none.setPreferredSize(new Dimension(0, 0));
            return none;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        }
    }
}
