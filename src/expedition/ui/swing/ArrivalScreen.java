/*
 * ArrivalScreen
 *
 * Reaching a planet, from Nadia's "Welcome to" examples: the planet's corner view, the
 * fuel gauge in the top left, the planet's name and story in the text box, Y and N on
 * the right to send a team down or not, and SHIP in the bottom left to check the ship.
 */
package expedition.ui.swing;

import expedition.ui.screen.Choices;
import expedition.ui.screen.PlanetInfo;
import expedition.ui.screen.ShipStatus;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

class ArrivalScreen extends GameScreen {

    private static final int FUEL_LABEL_X = 35;
    private static final int FUEL_LABEL_TOP = 58;
    private static final int GAUGE_X = 148;
    private static final int GAUGE_Y = 28;
    private static final int GAUGE_WIDTH = 140;
    private static final int NEXT_JUMP_TOP = 120;

    private static final int TITLE_TOP = 135;
    private static final Rectangle BOX = new Rectangle(278, 188, 734, 372);
    private static final Rectangle TEXT = new Rectangle(318, 214, 654, 320);

    private static final Rectangle YES_NO = new Rectangle(1040, 275, 135, 205);
    private static final int YES_NO_BUTTON = 76;
    // a darker shade of each button's colour, like Nadia's Y and N
    private static final Color YES_TEXT = new Color(0x2F5A1C);
    private static final Color NO_TEXT = new Color(0x6E1A12);

    private static final Rectangle SHIP = new Rectangle(33, 573, 207, 116);

    private final Font fuelFont = Assets.pixelta(56f);
    private final Font titleFont = Assets.pixelta(50f);
    private final Font textFont = Assets.pixelta(36f);
    private final Font smallFont = Assets.pixelta(22f);
    private final BufferedImage box = Assets.scaled(Assets.cached("ui/text-box.png"), BOX.width, BOX.height);
    private final BufferedImage yesNo = Assets.scaled(Assets.cached("ui/yes-no-panel.png"), YES_NO.width, YES_NO.height);
    private final FuelGauge gauge = new FuelGauge(GAUGE_WIDTH, Color.WHITE);

    private BufferedImage background;
    private PlanetInfo planet;
    private ShipStatus ship;

    ArrivalScreen(Consumer<Object> answers) {
        super(answers);
        gauge.setLocation(GAUGE_X, GAUGE_Y);
        add(gauge);

        int buttonX = YES_NO.x + (YES_NO.width - YES_NO_BUTTON) / 2;
        // the letters are drawn on, the button art is blank
        ImageButton yes = new ImageButton(Assets.scaled(Assets.cached("ui/yes-button.png"), YES_NO_BUTTON, YES_NO_BUTTON),
                "Y", Assets.pixelta(58f), YES_TEXT);
        yes.setToolTipText("Yes, send a team down");
        yes.setLocation(buttonX, YES_NO.y + 20);
        yes.addActionListener(e -> answer(Choices.Arrival.DRILL));
        add(yes);

        ImageButton no = new ImageButton(Assets.scaled(Assets.cached("ui/no-button.png"), YES_NO_BUTTON, YES_NO_BUTTON),
                "N", Assets.pixelta(58f), NO_TEXT);
        no.setToolTipText("No, stay aboard");
        no.setLocation(buttonX, YES_NO.y + YES_NO.height - YES_NO_BUTTON - 20);
        no.addActionListener(e -> answer(Choices.Arrival.STAY_ABOARD));
        add(no);

        ImageButton shipButton = new ImageButton(shipButtonArt(), "Check and repair the ship");
        shipButton.setLocation(SHIP.x, SHIP.y);
        shipButton.addActionListener(e -> answer(Choices.Arrival.SHIP));
        add(shipButton);
    }

    // EDT only
    void show(PlanetInfo planet, ShipStatus ship) {
        this.planet = planet;
        this.ship = ship;
        background = Assets.planet(planet.archetype());
        gauge.setFuel(ship.fuel(), ship.tankCapacity());
        repaint();
    }

    // "SHIP" and the wrench on the small grey button, like Nadia's example
    private BufferedImage shipButtonArt() {
        BufferedImage art = Assets.scaled(Assets.cached("ui/small-button.png"), SHIP.width, SHIP.height);
        Graphics2D g = art.createGraphics();
        Draw.left(g, "SHIP", Assets.pixelta(54f), Color.WHITE, 24, 34);
        BufferedImage wrench = Assets.scaledToHeight(Assets.cached("ui/wrench.png"), 74);
        g.drawImage(wrench, SHIP.width - wrench.getWidth() - 26, (SHIP.height - wrench.getHeight()) / 2 - 4, null);
        g.dispose();
        return art;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (planet == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        paintBackground(g2, background);

        Draw.left(g2, "Fuel:", fuelFont, Color.WHITE, FUEL_LABEL_X, FUEL_LABEL_TOP);
        // the cost of leaving, so the player knows how much oil they need before deciding
        String nextJump = "Next jump: " + ship.nextJumpCost();
        if (!ship.canAffordJump()) {
            nextJump += " (" + (ship.nextJumpCost() - ship.fuel()) + " short)";
        }
        Draw.left(g2, nextJump, smallFont, ship.canAffordJump() ? Draw.LIGHT_BLUE : Draw.DANGER, FUEL_LABEL_X, NEXT_JUMP_TOP);

        Draw.centred(g2, planet.name() + " [" + titleCase(planet.archetype().name()) + "]", titleFont, Color.WHITE,
                BOX.x + BOX.width / 2, TITLE_TOP);
        g2.drawImage(box, BOX.x, BOX.y, null);
        Draw.paragraph(g2, planet.flavour() + " Would you like to take an expedition down?", textFont, Color.WHITE, TEXT);
        g2.drawImage(yesNo, YES_NO.x, YES_NO.y, null);
        g2.dispose();
    }

    // FROZEN -> Frozen
    private static String titleCase(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
