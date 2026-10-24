/*
 * CockpitScreen
 *
 * The jump, from Nadia's jump sequence: inside the ship with the next jump's cost on the
 * dashboard and the fuel gauge in the corner. Press JUMP, then pull the lever to go. The
 * lights go from red to green once JUMP is pressed, so the player knows the lever is ready.
 * ITEMS opens the inventory first, for Condensed Oil when the fuel is short.
 *
 * Pulling the lever swaps to Nadia's picture of the lever pulled down, and the jump goes
 * a moment later.
 */
package expedition.ui.swing;

import expedition.ui.screen.Choices;
import expedition.ui.screen.ShipStatus;
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
import java.util.function.Consumer;
import javax.swing.Timer;

class CockpitScreen extends GameScreen {

    // measured from Nadia's example
    private static final Rectangle COST_BOX = new Rectangle(137, 487, 138, 90);
    private static final Point JUMP = new Point(642, 605);
    private static final int JUMP_RADIUS = 100;
    private static final Rectangle LEVER = new Rectangle(993, 428, 50, 182);
    private static final Point[] LIGHTS = {new Point(137, 653), new Point(207, 653), new Point(280, 653)};
    private static final int LIGHT_RADIUS = 18;
    private static final int GAUGE_X = 1088;
    private static final int GAUGE_Y = 585;
    private static final int HINT_TOP = 40;

    private static final Color JUMP_TEXT = new Color(0xB8322F);
    // how long the pulled lever shows before the jump goes
    private static final int PULLED_MS = 500;

    private final BufferedImage background = Assets.scaled(Assets.cached("backgrounds/inside-ship.png"), Assets.WIDTH, Assets.HEIGHT);
    private final BufferedImage pulledBackground = Assets.scaled(Assets.cached("backgrounds/inside-ship-pulled.png"), Assets.WIDTH, Assets.HEIGHT);
    private final Font costFont = Assets.pixelta(26f);
    private final Font jumpFont = Assets.pixelta(76f);
    private final Font pullFont = Assets.pixelta(30f);
    private final Font hintFont = Assets.pixelta(32f);
    private final FuelGauge gauge = new FuelGauge(170, Color.BLACK);
    private final ImageButton items;

    private ShipStatus ship;
    private boolean armed = false;
    // true once the lever is pulled, while the jump is about to go
    private boolean pulled = false;

    // openInventory is how SwingView puts the inventory over this screen
    CockpitScreen(Consumer<Object> answers, Runnable openInventory) {
        super(answers);
        gauge.setLocation(GAUGE_X, GAUGE_Y);
        add(gauge);

        items = Draw.longButton("ITEMS", 170, 58, 36f);
        items.setLocation(122, 412);
        items.addActionListener(e -> {
            if (isWaiting() && !pulled) {
                openInventory.run();
            }
        });
        add(items);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isWaiting() || pulled) {
                    return;
                }
                if (JUMP.distance(e.getPoint()) <= JUMP_RADIUS) {
                    armed = true;
                    repaint();
                } else if (armed && LEVER.contains(e.getPoint())) {
                    pullLever();
                }
            }
        });
    }

    // EDT only
    void show(ShipStatus ship) {
        this.ship = ship;
        armed = false;
        pulled = false;
        gauge.setFuel(ship.fuel(), ship.tankCapacity());
        items.setEnabled(!ship.items().isEmpty());
        repaint();
    }

    ShipStatus ship() {
        return ship;
    }

    // AI assisted - Claude Code
    // the lever shows pulled down, then the jump goes
    private void pullLever() {
        pulled = true;
        repaint();
        Timer jump = new Timer(PULLED_MS, e -> answer(Choices.Jump.JUMP));
        jump.setRepeats(false);
        jump.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (ship == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(pulled ? pulledBackground : background, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String hint = armed ? "Now pull the lever" : "Press JUMP, then pull the lever";
        Draw.centred(g2, hint, hintFont, Color.WHITE, Assets.WIDTH / 2, HINT_TOP);

        Color costColour = ship.canAffordJump() ? Color.WHITE : Draw.DANGER;
        Draw.centred(g2, "Next Jump", costFont, costColour, COST_BOX.x + COST_BOX.width / 2, COST_BOX.y + 20);
        Draw.centred(g2, "Costs: " + ship.nextJumpCost(), costFont, costColour, COST_BOX.x + COST_BOX.width / 2, COST_BOX.y + 52);

        Draw.centred(g2, "JUMP", jumpFont, JUMP_TEXT, JUMP.x, JUMP.y - 34);
        if (armed) {
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(6));
            g2.drawOval(JUMP.x - JUMP_RADIUS, JUMP.y - JUMP_RADIUS, 2 * JUMP_RADIUS, 2 * JUMP_RADIUS);
        }

        // red lights until JUMP is pressed, then green
        for (int i = 0; i < LIGHTS.length; i++) {
            Color c = armed ? Draw.GOOD : (i == LIGHTS.length - 1 ? Draw.DANGER : new Color(0x555555));
            g2.setColor(c);
            g2.fillOval(LIGHTS[i].x - LIGHT_RADIUS, LIGHTS[i].y - LIGHT_RADIUS, 2 * LIGHT_RADIUS, 2 * LIGHT_RADIUS);
        }

        paintLever(g2);
        g2.dispose();
    }

    // PULL down the lever in Nadia's art, and a white outline once JUMP has armed it
    private void paintLever(Graphics2D g2) {
        if (pulled) {
            return;
        }
        if (armed) {
            g2.setColor(new Color(255, 255, 255, 140));
            g2.setStroke(new BasicStroke(4));
            g2.drawRoundRect(LEVER.x - 6, LEVER.y - 6, LEVER.width + 12, LEVER.height + 12, 50, 64);
        }
        String word = "PULL";
        for (int i = 0; i < word.length(); i++) {
            Draw.centred(g2, String.valueOf(word.charAt(i)), pullFont, JUMP_TEXT, LEVER.x + LEVER.width / 2, LEVER.y + 30 + i * 32);
        }
    }
}
