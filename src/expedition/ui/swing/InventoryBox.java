/*
 * InventoryBox
 *
 * The hold, from Nadia's inventory sketch: a grid of slots with the items in them, and
 * the faces of the crew down the side. Click an item to see what it does, then click
 * who to give it to, or USE for an item that fixes the ship. Used from the drill screen,
 * where the team down there can be given things, and from the cockpit before a jump.
 */
package expedition.ui.swing;

import expedition.ui.screen.Choices;
import expedition.ui.screen.CrewCard;
import expedition.ui.screen.ItemInfo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;

class InventoryBox extends JComponent {

    private static final int WIDTH = 820;
    private static final int HEIGHT = 520;
    private static final int COLUMNS = 4;
    private static final int SLOT = 110;
    private static final int GRID_X = 50;
    private static final int GRID_Y = 90;
    private static final int FACE = 96;
    private static final int FACES_X = 560;
    private static final int FACE_STEP = 112;

    private static final Color FILL = new Color(0xE9ECEF);
    private static final Color LINE = new Color(0x2E3A46);
    private static final Color PICKED = new Color(0xE8732C);
    private static final Color DIM_TEXT = new Color(0x4A5866);

    private final List<ItemInfo> items;
    private final List<CrewCard> crew;
    private final int capacity;
    private final Font titleFont = Assets.pixelta(48f);
    private final Font countFont = Assets.pixelta(24f);
    private final Font infoFont = Assets.pixelta(24f);
    private final Font nameFont = Assets.pixelta(20f);
    private final ImageButton use;
    private int picked = -1;
    private boolean done = false;
    // the faces get smaller when the whole crew is listed, so they all fit down the side
    private final int faceStep;
    private final int faceSize;

    // onUse gets the item and who to give it to, onClose is called when the player closes it
    InventoryBox(List<ItemInfo> items, List<CrewCard> crew, int capacity, Consumer<Choices.ItemUse> onUse, Runnable onClose) {
        this.items = items;
        this.crew = crew;
        this.capacity = capacity;
        faceStep = Math.min(FACE_STEP, (HEIGHT - GRID_Y - 110) / Math.max(1, crew.size()));
        faceSize = Math.min(FACE, faceStep - 8);
        setLayout(null);
        setSize(WIDTH, HEIGHT);

        use = Draw.smallButton("USE");
        use.setLocation(WIDTH - 2 * 150, HEIGHT - 95);
        use.setVisible(false);
        use.addActionListener(e -> finish(() -> onUse.accept(new Choices.ItemUse(items.get(picked).name(), null))));
        add(use);

        ImageButton close = Draw.button("ui/small-button.png", 150, 78, "CLOSE", 46f, Color.WHITE);
        close.setLocation(WIDTH - 165, HEIGHT - 95);
        close.addActionListener(e -> finish(onClose));
        add(close);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < items.size(); i++) {
                    if (slot(i).contains(e.getPoint())) {
                        picked = i;
                        use.setVisible(!items.get(i).needsCrewTarget());
                        repaint();
                        return;
                    }
                }
                if (picked >= 0 && items.get(picked).needsCrewTarget()) {
                    for (int c = 0; c < crew.size(); c++) {
                        if (face(c).contains(e.getPoint()) && crew.get(c).alive()) {
                            String who = crew.get(c).name();
                            finish(() -> onUse.accept(new Choices.ItemUse(items.get(picked).name(), who)));
                            return;
                        }
                    }
                }
            }
        });
    }

    // only the first button press counts, the box is gone after it
    private void finish(Runnable action) {
        if (!done) {
            done = true;
            action.run();
        }
    }

    private Rectangle slot(int i) {
        return new Rectangle(GRID_X + (i % COLUMNS) * SLOT, GRID_Y + (i / COLUMNS) * SLOT, SLOT, SLOT);
    }

    private Rectangle face(int i) {
        return new Rectangle(FACES_X + 40, GRID_Y + i * faceStep, faceSize, faceSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Draw.NAVY);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setColor(Draw.LIGHT_BLUE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(1, 1, WIDTH - 3, HEIGHT - 3);
        Draw.centred(g2, "Inventory", titleFont, Color.WHITE, GRID_X + COLUMNS * SLOT / 2, 26);

        // the slots, filled in order, with how many of each in the corner
        int rows = (capacity + COLUMNS - 1) / COLUMNS;
        for (int i = 0; i < rows * COLUMNS; i++) {
            Rectangle r = slot(i);
            g2.setColor(i < capacity ? FILL : new Color(0x7D8A96));
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor(LINE);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(r.x, r.y, r.width, r.height);
            if (i < items.size()) {
                BufferedImage icon = ItemIcons.icon(items.get(i).name(), 72);
                g2.drawImage(icon, r.x + (r.width - 72) / 2, r.y + 12, null);
                if (items.get(i).count() > 1) {
                    Draw.left(g2, "x" + items.get(i).count(), countFont, LINE, r.x + r.width - 34, r.y + r.height - 26);
                }
            }
            if (i == picked) {
                g2.setColor(PICKED);
                g2.setStroke(new BasicStroke(5));
                g2.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
            }
        }

        // who can be given something
        for (int c = 0; c < crew.size(); c++) {
            Rectangle f = face(c);
            BufferedImage pic = Assets.scaled(Assets.crew(crew.get(c).name(), Assets.Pose.PROFILE), f.width, f.width * 540 / 495);
            g2.drawImage(pic, f.x, f.y, null);
            Draw.left(g2, crew.get(c).name(), nameFont, crew.get(c).alive() ? Color.WHITE : DIM_TEXT, f.x + f.width + 8, f.y + f.height / 2 - 18);
            Draw.left(g2, "HP " + crew.get(c).health(), nameFont, Draw.LIGHT_BLUE, f.x + f.width + 8, f.y + f.height / 2 + 6);
        }

        // what the picked item does, and what to do next
        String info;
        if (items.isEmpty()) {
            info = "The hold is empty. Items are found while drilling.";
        } else if (picked < 0) {
            info = "Click an item to see what it does.";
        } else {
            ItemInfo item = items.get(picked);
            info = item.name() + ": " + item.description()
                    + (item.needsCrewTarget() ? " Click who to give it to." : " Press USE.");
        }
        Draw.paragraph(g2, info, infoFont, Draw.LIGHT_BLUE,
                new Rectangle(GRID_X, GRID_Y + rows * SLOT + 8, WIDTH - GRID_X - 330, HEIGHT - (GRID_Y + rows * SLOT + 8) - 14));
        g2.dispose();
    }
}
