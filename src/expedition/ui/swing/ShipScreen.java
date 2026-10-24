/*
 * ShipScreen
 *
 * The ship's health, from Nadia's example: the ship in the middle, a caution box for each
 * of the five parts round the edge, REPAIR at the top and OK at the bottom.
 *
 * A damaged part's box goes red, and that part of the ship gets Nadia's red outline, as
 * she asked instead of a separate repair screen. REPAIR asks which part to fix, then clicking a
 * box fixes it with a Repair Kit. Clicking a box straight away does the same.
 * If there are no Repair Kits the engine says so.
 */
package expedition.ui.swing;

import expedition.ship.ShipPart;
import expedition.ui.screen.Choices;
import expedition.ui.screen.ItemInfo;
import expedition.ui.screen.ShipStatus;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

class ShipScreen extends GameScreen {

    // the name of the item that repairs, so the screen can say how many there are
    private static final String REPAIR_KIT = "Repair Kit";

    private static final int BOX_SIZE = 190;
    private static final Rectangle SHIP = new Rectangle(215, 150, 755, 435);
    private static final Rectangle REPAIR = new Rectangle(555, 48, 190, 100);
    private static final Rectangle OK = new Rectangle(578, 598, 140, 80);
    private static final int HINT_TOP = 160;

    // where each part's box is, round the edge like Nadia's example
    private static final Map<ShipPart, Rectangle> BOXES = new EnumMap<>(ShipPart.class);
    static {
        BOXES.put(ShipPart.HULL, new Rectangle(45, 38, BOX_SIZE, BOX_SIZE));
        BOXES.put(ShipPart.ENGINE, new Rectangle(45, 490, BOX_SIZE, BOX_SIZE));
        BOXES.put(ShipPart.EXHAUST, new Rectangle(1046, 38, BOX_SIZE, BOX_SIZE));
        BOXES.put(ShipPart.PRESSURE, new Rectangle(1046, 265, BOX_SIZE, BOX_SIZE));
        BOXES.put(ShipPart.WINDOWS, new Rectangle(1046, 490, BOX_SIZE, BOX_SIZE));
    }

    private final BufferedImage shipArt = Assets.scaled(Assets.cached("ship/spaceship.png"), SHIP.width, SHIP.height);
    // Nadia's red outline for each part, the same size as the ship so it sits on top of it
    private final Map<ShipPart, BufferedImage> outlines = new EnumMap<>(ShipPart.class);
    private final BufferedImage boxArt = Assets.scaled(Assets.cached("ui/repair-box.png"), BOX_SIZE, BOX_SIZE);
    private final Font labelFont = Assets.pixelta(46f);
    private final Font smallLabelFont = Assets.pixelta(38f);
    private final Font hintFont = Assets.pixelta(28f);

    private ShipStatus ship;
    // true after REPAIR, until a part is picked
    private boolean picking = false;

    ShipScreen(Consumer<Object> answers) {
        super(answers);

        ImageButton repair = new ImageButton(repairButtonArt(), "Repair a part with a Repair Kit");
        repair.setLocation(REPAIR.x, REPAIR.y);
        repair.addActionListener(e -> {
            picking = !picking;
            repaint();
        });
        add(repair);

        ImageButton ok = Draw.button("ui/small-button.png", OK.width, OK.height, "OK", 54f, Color.WHITE);
        ok.setLocation(OK.x, OK.y);
        ok.addActionListener(e -> answer(Choices.ShipScreen.DONE));
        add(ok);

        setCursor(Cursor.getDefaultCursor());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Map.Entry<ShipPart, Rectangle> box : BOXES.entrySet()) {
                    if (box.getValue().contains(e.getPoint())) {
                        picking = false;
                        answer(new Choices.ShipScreen(box.getKey()));
                    }
                }
            }
        });
    }

    // EDT only
    void show(ShipStatus ship) {
        this.ship = ship;
        picking = false;
        repaint();
    }

    // "REPAIR" with the wrench leaning on its corner, like Nadia's example
    private BufferedImage repairButtonArt() {
        BufferedImage art = new BufferedImage(REPAIR.width, REPAIR.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = art.createGraphics();
        g.drawImage(Assets.scaled(Assets.cached("ui/small-button.png"), REPAIR.width - 20, REPAIR.height - 12), 0, 12, null);
        Draw.centred(g, "REPAIR", Assets.pixelta(48f), Color.WHITE, (REPAIR.width - 20) / 2, 38);
        BufferedImage wrench = Assets.scaledToHeight(Assets.cached("ui/wrench.png"), 64);
        g.drawImage(wrench, REPAIR.width - wrench.getWidth() - 2, 0, null);
        g.dispose();
        return art;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (ship == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(shipArt, SHIP.x, SHIP.y, null);

        for (ShipPart part : ShipPart.values()) {
            int health = ship.parts().get(part);
            if (health < 100) {
                g2.drawImage(outline(part), SHIP.x, SHIP.y, null);
            }
            paintBox(g2, part, BOXES.get(part), health);
        }

        int kits = 0;
        for (ItemInfo item : ship.items()) {
            if (item.name().equals(REPAIR_KIT)) {
                kits = item.count();
            }
        }
        String hint = picking ? "Click the part to repair" : "Repair Kits: " + kits;
        Draw.centred(g2, hint, hintFont, picking ? Color.WHITE : Draw.LIGHT_BLUE, Assets.WIDTH / 2, HINT_TOP);
        g2.dispose();
    }

    private BufferedImage outline(ShipPart part) {
        return outlines.computeIfAbsent(part, p -> Assets.scaled(Assets.damaged(p), SHIP.width, SHIP.height));
    }

    private void paintBox(Graphics2D g2, ShipPart part, Rectangle box, int health) {
        g2.drawImage(boxArt, box.x, box.y, null);
        if (picking) {
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRect(box.x + 25, box.y + 25, box.width - 50, box.height - 50);
        }
        Color text = health >= 100 ? Color.WHITE : Draw.DANGER;
        String label = part.label().toUpperCase();
        // the longer names are a size down so they fit inside the stripes, like Nadia's
        Font font = label.length() > 6 ? smallLabelFont : labelFont;
        Draw.centred(g2, label, font, text, box.x + box.width / 2, box.y + 52);
        Draw.centred(g2, health + "%", labelFont, text, box.x + box.width / 2, box.y + 108);
    }
}
