/*
 * DrillScreen
 *
 * Drilling, from Nadia's drill sketches and her cave, drill and barrel art:
 * - top left: the fuel gauge and the level, "Level: 3/9" if a Geologist can see the bottom
 * - top: the risk of the next level, the next jump's cost and the chance of an item
 * - left: an ID card for each of the team, with their health, stamina and composure
 * - middle: the drill, click it to go down a level. Its hose runs into the oil barrel,
 *   which fills up as the haul does. ITEMS under the cards opens the inventory
 * - right: the team, then OTHER SITE back to the site map, ASCEND to fly back to the ship
 * After a level the oil and points float up over the drill, and anything found shows
 * in a bubble, which goes into the inventory when clicked.
 */
package expedition.ui.swing;

import expedition.ui.screen.Choices;
import expedition.ui.screen.CrewCard;
import expedition.ui.screen.DrillStatus;
import expedition.ui.screen.ItemInfo;
import expedition.ui.screen.LevelResult;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Timer;

class DrillScreen extends GameScreen {

    // the cave is darkened a little so the white text stands out
    private static final float BACKGROUND_DIM = 0.2f;

    private static final int LEVEL_TOP = 120;
    private static final int INFO_X = 300;
    private static final int INFO_TOP = 22;
    private static final int INFO_STEP = 34;

    private static final int CARD_X = 24;
    private static final int CARD_TOP = 170;
    private static final int CARD_W = 262;
    private static final int CARD_H = 96;
    private static final int CARD_STEP = 106;
    private static final Color CARD_FILL = new Color(0x5D6D7B);
    private static final Color CARD_EDGE = new Color(0x3E4A56);

    // Nadia's drill keeps its shape at this size, and the end of its hose (its bottom left
    // corner) meets the barrel's right side
    private static final Rectangle DRILL = new Rectangle(400, 210, 416, 430);
    private static final Rectangle BARREL = new Rectangle(300, 490, 100, 150);
    private static final int CREW_BOTTOM = 550;
    private static final int CREW_HEIGHT = 190;
    private static final int[] CREW_X = {875, 975, 1075};

    // the oil and points float up from here and fade
    private static final int FLOAT_X = 640;
    private static final int FLOAT_TOP = 100;
    private static final int FLOAT_MS = 2600;
    private static final int FLOAT_STEP_MS = 40;
    // where found items' bubbles sit, beside the drill
    private static final Point[] BUBBLES = {new Point(900, 250), new Point(990, 190), new Point(1080, 250)};
    private static final int BUBBLE = 76;

    private static final Color HEALTH = new Color(0x4CC24C);
    private static final Color STAMINA = new Color(0x00E5FF);
    private static final Color COMPOSURE = new Color(0x8FB8FF);
    private static final Color BAR_BACK = new Color(0x2E3A46);
    // a lighter red than Draw.DANGER, so it reads on the dark box
    private static final Color SPILL = new Color(0xFF6B5E);

    private final Font levelFont = Assets.pixelta(42f);
    private final Font infoFont = Assets.pixelta(28f);
    private final Font cardNameFont = Assets.pixelta(26f);
    private final Font cardStatFont = Assets.pixelta(16f);
    private final Font barrelFont = Assets.pixelta(26f);
    private final Font floatFont = Assets.pixelta(40f);
    private final Font hintFont = Assets.pixelta(26f);
    private final FuelGauge gauge = new FuelGauge(130, Color.WHITE);
    private final ImageButton items;
    private final Timer floatTimer;

    private final BufferedImage background = Assets.scaled(Assets.cached("backgrounds/cave.png"), Assets.WIDTH, Assets.HEIGHT);
    private DrillStatus status;
    private LevelResult result;
    private int floatMs = FLOAT_MS;
    private final List<String> bubbles = new ArrayList<>();

    // openInventory is how SwingView puts the inventory over this screen
    DrillScreen(Consumer<Object> answers, Runnable openInventory) {
        super(answers);
        gauge.setLocation(24, 14);
        add(gauge);

        ImageButton drill = new ImageButton(Assets.scaled(Assets.cached("drill/drill.png"), DRILL.width, DRILL.height), "Click to drill down a level");
        drill.setLocation(DRILL.x, DRILL.y);
        drill.addActionListener(e -> answer(Choices.Drill.of(Choices.DrillAction.DRILL)));
        add(drill);

        items = Draw.longButton("ITEMS", 170, 62, 38f);
        items.setLocation(CARD_X + (CARD_W - 170) / 2, BARREL.y + BARREL.height - 62);
        items.addActionListener(e -> {
            if (isWaiting()) {
                openInventory.run();
            }
        });
        add(items);

        ImageButton otherSite = Draw.longButton("OTHER SITE", 230, 62, 36f);
        otherSite.setLocation(1026, 560);
        otherSite.addActionListener(e -> answer(Choices.Drill.of(Choices.DrillAction.OTHER_SITE)));
        add(otherSite);

        ImageButton ascend = Draw.longButton("ASCEND", 230, 70, 46f);
        ascend.setLocation(1026, 632);
        ascend.addActionListener(e -> answer(Choices.Drill.of(Choices.DrillAction.ASCEND)));
        add(ascend);

        floatTimer = new Timer(FLOAT_STEP_MS, e -> {
            floatMs += FLOAT_STEP_MS;
            if (floatMs >= FLOAT_MS) {
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        // a found item's bubble goes into the inventory when clicked
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < bubbles.size() && i < BUBBLES.length; i++) {
                    if (BUBBLES[i].distance(e.getPoint()) <= BUBBLE / 2.0) {
                        bubbles.remove(i);
                        repaint();
                        return;
                    }
                }
            }
        });
    }

    // EDT only. The level just drilled, shown when the screen next appears
    void levelDrilled(LevelResult result) {
        this.result = result;
        bubbles.addAll(result.itemsFound());
        floatMs = 0;
    }

    // EDT only
    void show(DrillStatus status) {
        // a new site starts with no oil yet, the bubbles from the last one have gone
        if (this.status != null && !this.status.siteName().equals(status.siteName())) {
            bubbles.clear();
        }
        this.status = status;
        gauge.setFuel(status.fuel(), status.tankCapacity());
        items.setLabelText("ITEMS (" + itemCount() + ")");
        if (floatMs < FLOAT_MS) {
            floatTimer.restart();
        }
        repaint();
    }

    DrillStatus status() {
        return status;
    }

    private int itemCount() {
        int count = 0;
        for (ItemInfo item : status.items()) {
            count += item.count();
        }
        return count;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (status == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        paintBackground(g2, background);
        g2.setColor(new Color(0, 0, 0, (int) (255 * BACKGROUND_DIM)));
        g2.fillRect(0, 0, Assets.WIDTH, Assets.HEIGHT);

        String level = "Level: " + status.nextLevel();
        if (status.levelsLeft() != DrillStatus.UNKNOWN) {
            // the Geologist can see how deep the vein goes
            level += "/" + (status.nextLevel() - 1 + status.levelsLeft());
        }
        Draw.left(g2, level, levelFont, Color.WHITE, 24, LEVEL_TOP);

        Draw.left(g2, "CURRENT RISK: " + status.hazardPercent() + "%", infoFont, Color.WHITE, INFO_X, INFO_TOP);
        Draw.left(g2, "NEXT JUMP COST: " + status.nextJumpCost() + " fuel", infoFont, Color.WHITE, INFO_X, INFO_TOP + INFO_STEP);
        Draw.left(g2, "ITEM CHANCE: " + status.itemPercent() + "%", infoFont, Color.WHITE, INFO_X, INFO_TOP + 2 * INFO_STEP);
        Draw.left(g2, status.siteName(), infoFont, Draw.LIGHT_BLUE, INFO_X, INFO_TOP + 3 * INFO_STEP);

        List<CrewCard> team = status.team();
        for (int i = 0; i < team.size(); i++) {
            paintCard(g2, team.get(i), CARD_TOP + i * CARD_STEP);
            paintCrewMember(g2, team.get(i), i);
        }

        paintBarrel(g2);
        Draw.centred(g2, "Click to drill", hintFont, Color.WHITE, DRILL.x + DRILL.width / 2, DRILL.y - 30);
        paintBubbles(g2);
        paintFloatingResult(g2);
        g2.dispose();
    }

    private void paintCard(Graphics2D g2, CrewCard c, int y) {
        g2.setColor(CARD_EDGE);
        g2.fillRect(CARD_X - 4, y - 4, CARD_W + 8, CARD_H + 8);
        g2.setColor(CARD_FILL);
        g2.fillRect(CARD_X, y, CARD_W, CARD_H);
        int pic = CARD_H - 16;
        g2.drawImage(Assets.scaled(Assets.crew(c.name(), Assets.Pose.PROFILE), pic, pic * 540 / 495), CARD_X + 6, y + 4, null);

        int x = CARD_X + pic + 16;
        int w = CARD_W - pic - 26;
        Draw.left(g2, c.alive() ? c.name() : c.name() + " (dead)", cardNameFont, c.alive() ? Color.WHITE : Draw.DANGER, x, y + 8);
        statBar(g2, "Health", c.alive() ? c.health() : 0, HEALTH, x, y + 38, w);
        statBar(g2, "Stamina", c.stamina(), STAMINA, x, y + 57, w);
        statBar(g2, "Composure", c.composure(), COMPOSURE, x, y + 76, w);
    }

    private void statBar(Graphics2D g2, String label, int value, Color colour, int x, int y, int w) {
        int labelW = 74;
        Draw.left(g2, label, cardStatFont, Color.WHITE, x, y);
        g2.setColor(BAR_BACK);
        g2.fillRect(x + labelW, y, w - labelW, 12);
        g2.setColor(value < 30 ? Draw.DANGER : colour);
        g2.fillRect(x + labelW, y, (w - labelW) * Math.max(0, Math.min(100, value)) / 100, 12);
    }

    // the first of the team stands left of the drill, the others to the right, like the sketch
    private void paintCrewMember(Graphics2D g2, CrewCard c, int i) {
        if (i >= CREW_X.length) {
            return;
        }
        BufferedImage sprite = Assets.scaledToHeight(Assets.crew(c.name(), c.alive() ? Assets.Pose.STANDING : Assets.Pose.DEAD), CREW_HEIGHT);
        g2.drawImage(sprite, CREW_X[i] - sprite.getWidth() / 2, CREW_BOTTOM - sprite.getHeight(), null);
    }

    // AI assisted - Claude Code swapped the drawn barrel and drill for Nadia's art
    // The barrel fills up as the haul does. Full means the tank has no room for more, and
    // anything past that would spill
    private void paintBarrel(Graphics2D g2) {
        int oil = status.haul() + status.oilAboard();
        int space = Math.max(1, status.tankCapacity() - status.fuel());
        double full = Math.min(1, (double) oil / space);
        g2.drawImage(Assets.scaled(Assets.barrel(full), BARREL.width, BARREL.height), BARREL.x, BARREL.y, null);

        Draw.centred(g2, oil + " oil", barrelFont, Color.WHITE, BARREL.x + BARREL.width / 2, BARREL.y - 48);
        if (status.spill() > 0) {
            paintSpill(g2, status.spill() + " would spill");
        }
    }

    // the spill warning on a dark box, red on its own was hard to read on the grey cave
    private void paintSpill(Graphics2D g2, String text) {
        Font font = Assets.pixelta(22f);
        int w = g2.getFontMetrics(font).stringWidth(text) + 16;
        int centre = BARREL.x + BARREL.width / 2;
        int top = BARREL.y - 10;
        g2.setColor(new Color(0x0A1424));
        g2.fillRoundRect(centre - w / 2, top - 2, w, 28, 10, 10);
        g2.setColor(SPILL);
        g2.drawRoundRect(centre - w / 2, top - 2, w, 28, 10, 10);
        Draw.centred(g2, text, font, SPILL, centre, top);
    }

    private void paintBubbles(Graphics2D g2) {
        for (int i = 0; i < bubbles.size() && i < BUBBLES.length; i++) {
            Point p = BUBBLES[i];
            Graphics2D b = (Graphics2D) g2.create();
            b.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            b.setColor(new Color(255, 255, 255, 70));
            b.fillOval(p.x - BUBBLE / 2, p.y - BUBBLE / 2, BUBBLE, BUBBLE);
            b.setColor(new Color(0xF5D53A));
            b.setStroke(new BasicStroke(4));
            b.drawOval(p.x - BUBBLE / 2, p.y - BUBBLE / 2, BUBBLE, BUBBLE);
            b.drawImage(ItemIcons.icon(bubbles.get(i), 48), p.x - 24, p.y - 24, null);
            b.dispose();
        }
        if (!bubbles.isEmpty()) {
            Draw.centred(g2, "Found! Click to collect", Assets.pixelta(22f), new Color(0xF5D53A), BUBBLES[0].x, BUBBLES[0].y + BUBBLE / 2 + 6);
        }
    }

    // "+27 oil" and "+3 points" rise up over the drill and fade out
    private void paintFloatingResult(Graphics2D g2) {
        if (result == null || floatMs >= FLOAT_MS) {
            return;
        }
        float left = 1f - (float) floatMs / FLOAT_MS;
        int rise = (int) (40 * (1 - left));
        Graphics2D f = (Graphics2D) g2.create();
        f.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, left)));
        Draw.centred(f, "+" + result.oil() + " oil", floatFont, new Color(0x7CFC7C), FLOAT_X, FLOAT_TOP - rise);
        Draw.centred(f, "+" + result.points() + " points", floatFont, new Color(0xF5D53A), FLOAT_X, FLOAT_TOP + 44 - rise);
        f.dispose();
    }
}
