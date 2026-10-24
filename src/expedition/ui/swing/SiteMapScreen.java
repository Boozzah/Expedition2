/*
 * SiteMapScreen
 *
 * Choosing where to drill, from Nadia's drill site art: the planet seen from above with
 * its three sites marked, and her box on the right with the details of the site clicked.
 * - The box's top band has the site's name, the middle its risk, oil and hazard chance,
 *   then the ship's fuel and the next jump's cost, so the player can see whether moving
 *   here still leaves enough fuel to jump
 * - The bottom left slot says what moving here costs, the bottom right slot is DRILL
 * - The marker of the site showing gets cyan brackets, and a closed site's are red
 * LEAVE PLANET in the bottom left takes the team back up. Moving to another site after
 * the first costs fuel.
 */
package expedition.ui.swing;

import expedition.ui.screen.ShipStatus;
import expedition.ui.screen.SiteInfo;
import expedition.world.PlanetArchetype;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class SiteMapScreen extends GameScreen {

    // answered when the player leaves the planet
    static final Object LEAVE = new Object();

    // where Nadia drew the three site markers on each planet, in window pixels
    private static final Map<PlanetArchetype, Point[]> MARKERS = new EnumMap<>(PlanetArchetype.class);
    static {
        MARKERS.put(PlanetArchetype.BARREN, new Point[]{new Point(365, 135), new Point(200, 320), new Point(570, 520)});
        MARKERS.put(PlanetArchetype.DESERT, new Point[]{new Point(165, 230), new Point(450, 395), new Point(250, 590)});
        MARKERS.put(PlanetArchetype.FROZEN, new Point[]{new Point(225, 170), new Point(510, 445), new Point(215, 550)});
        MARKERS.put(PlanetArchetype.STORM, new Point[]{new Point(380, 40), new Point(200, 355), new Point(465, 535)});
        MARKERS.put(PlanetArchetype.TOXIC, new Point[]{new Point(145, 170), new Point(360, 340), new Point(440, 540)});
        MARKERS.put(PlanetArchetype.VOLCANIC, new Point[]{new Point(505, 195), new Point(195, 320), new Point(360, 595)});
    }
    // how far from a marker's centre a click still counts, and the size of the brackets
    private static final int MARKER = 40;

    // the parts of Nadia's box, measured from her art
    private static final Rectangle HEADER = new Rectangle(820, 80, 410, 65);
    private static final int DETAILS_TOP = 165;
    private static final int LINE = 44;
    private static final int BOX_CENTRE = 1025;
    private static final Rectangle COST_SLOT = new Rectangle(835, 600, 250, 40);
    private static final Rectangle DRILL_SLOT = new Rectangle(1100, 600, 115, 40);
    private static final Color SLOT_FILL = new Color(0x162235);

    private static final int TITLE_TOP = 16;
    private static final int TITLE_CENTRE = 990;

    private static final Color MARKER_COLOUR = new Color(0x5CE1E6);
    private static final Color WARNING = new Color(0xFFB347);

    private final BufferedImage box = Assets.scaled(Assets.cached("sites/box.png"), Assets.WIDTH, Assets.HEIGHT);
    private final Font titleFont = Assets.pixelta(40f);
    private final Font nameFont = Assets.pixelta(40f);
    private final Font riskFont = Assets.pixelta(36f);
    private final Font detailFont = Assets.pixelta(28f);
    private final Font slotFont = Assets.pixelta(26f);
    private final ImageButton drill;

    private BufferedImage planet;
    private Point[] markers = new Point[0];
    private List<SiteInfo> sites = List.of();
    private int moveCost;
    private ShipStatus ship;
    private int picked = -1;

    SiteMapScreen(Consumer<Object> answers) {
        super(answers);

        // DRILL fills Nadia's bottom right slot, in the slot's own colour
        BufferedImage slot = new BufferedImage(DRILL_SLOT.width, DRILL_SLOT.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = slot.createGraphics();
        g.setColor(SLOT_FILL);
        g.fillRect(0, 0, slot.getWidth(), slot.getHeight());
        g.dispose();
        drill = new ImageButton(slot, "DRILL", Assets.pixelta(34f), Draw.GOOD);
        drill.setToolTipText("Send the team to this site");
        drill.setLocation(DRILL_SLOT.x, DRILL_SLOT.y);
        drill.addActionListener(e -> answer(picked));
        add(drill);

        ImageButton leave = Draw.longButton("LEAVE PLANET", 300, 64, 38f);
        leave.setLocation(20, Assets.HEIGHT - 64 - 16);
        leave.addActionListener(e -> answer(LEAVE));
        add(leave);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (int i = 0; i < sites.size() && i < markers.length; i++) {
                    if (markers[i].distance(e.getPoint()) <= MARKER) {
                        picked = i;
                        update();
                    }
                }
            }
        });
    }

    // EDT only. Starts with the first open site's details showing
    void show(PlanetArchetype archetype, List<SiteInfo> sites, int moveCost, ShipStatus ship) {
        planet = Assets.scaled(Assets.siteMap(archetype), Assets.WIDTH, Assets.HEIGHT);
        markers = MARKERS.get(archetype);
        this.sites = sites;
        this.moveCost = moveCost;
        this.ship = ship;
        picked = -1;
        for (int i = 0; i < sites.size(); i++) {
            if (sites.get(i).open()) {
                picked = i;
                break;
            }
        }
        update();
    }

    private void update() {
        drill.setEnabled(picked >= 0 && sites.get(picked).open());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sites.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        paintBackground(g2, planet);
        g2.drawImage(box, 0, 0, null);
        Draw.centred(g2, "Choose your Drill Site", titleFont, Color.WHITE, TITLE_CENTRE, TITLE_TOP);

        // Nadia drew the markers, so only the picked and closed ones get brackets on top
        for (int i = 0; i < sites.size() && i < markers.length; i++) {
            if (!sites.get(i).open()) {
                paintBrackets(g2, markers[i], Draw.DANGER, 5);
            } else if (i == picked) {
                paintBrackets(g2, markers[i], MARKER_COLOUR, 6);
            }
        }

        paintDetails(g2);
        g2.dispose();
    }

    // square brackets round a marker, like the ones in Nadia's art but bigger
    private void paintBrackets(Graphics2D g2, Point at, Color colour, int thickness) {
        int half = MARKER / 2 + 6;
        int arm = MARKER / 3;
        Graphics2D b = (Graphics2D) g2.create();
        b.setColor(colour);
        b.setStroke(new BasicStroke(thickness));
        b.drawPolyline(new int[]{at.x - half + arm, at.x - half, at.x - half, at.x - half + arm},
                new int[]{at.y - half, at.y - half, at.y + half, at.y + half}, 4);
        b.drawPolyline(new int[]{at.x + half - arm, at.x + half, at.x + half, at.x + half - arm},
                new int[]{at.y - half, at.y - half, at.y + half, at.y + half}, 4);
        b.dispose();
    }

    private void paintDetails(Graphics2D g2) {
        if (picked < 0) {
            Draw.centred(g2, "All closed", nameFont, Draw.DANGER, BOX_CENTRE, HEADER.y + 12);
            Draw.paragraph(g2, "Every site here is closed. Leave the planet.", detailFont, Color.WHITE,
                    new Rectangle(HEADER.x + 20, DETAILS_TOP, HEADER.width - 40, 200));
            return;
        }

        SiteInfo site = sites.get(picked);
        Draw.centred(g2, site.name(), nameFont, Color.WHITE, BOX_CENTRE, HEADER.y + 12);
        int y = DETAILS_TOP;
        if (!site.open()) {
            Draw.centred(g2, "SITE IS CLOSED", riskFont, Draw.DANGER, BOX_CENTRE, y);
        } else {
            Draw.centred(g2, site.tier() + " RISK", riskFont, riskColour(site.tier()), BOX_CENTRE, y);
            y += LINE + 6;
            Draw.centred(g2, "Est. oil " + site.estimatedOil().replace("oil -", "").replace("~", "").trim(), detailFont, Color.WHITE, BOX_CENTRE, y);
            y += LINE;
            Draw.centred(g2, "Hazard chance " + site.riskStart() + "% - " + site.riskDeep() + "%", detailFont, Color.WHITE, BOX_CENTRE, y);
        }
        paintFuel(g2);

        String cost = moveCost > 0 ? "COSTS " + moveCost + " FUEL" : "FIRST SITE FREE";
        Draw.centred(g2, cost, slotFont, Draw.LIGHT_BLUE, COST_SLOT.x + COST_SLOT.width / 2, COST_SLOT.y + 6);
    }

    // The fuel the ship has and what the next jump costs, at the bottom of the box. If moving
    // here would leave less than the jump needs, that line goes orange to warn the player
    private void paintFuel(Graphics2D g2) {
        if (ship == null) {
            return;
        }
        int y = DETAILS_TOP + 4 * LINE;
        g2.setColor(new Color(0x2E4460));
        g2.fillRect(HEADER.x + 30, y - 14, HEADER.width - 60, 3);
        Draw.centred(g2, "Fuel: " + ship.fuel() + "/" + ship.tankCapacity(), detailFont, Color.WHITE, BOX_CENTRE, y);
        y += LINE;
        Draw.centred(g2, "Next jump costs " + ship.nextJumpCost(), detailFont, Color.WHITE, BOX_CENTRE, y);
        if (moveCost > 0) {
            y += LINE;
            int left = ship.fuel() - moveCost;
            boolean tooShort = left < ship.nextJumpCost();
            Draw.centred(g2, "After moving: " + left + " fuel", detailFont, tooShort ? WARNING : Draw.LIGHT_BLUE, BOX_CENTRE, y);
            if (tooShort) {
                y += LINE - 8;
                Draw.centred(g2, "Not enough to jump unless you find oil", Assets.pixelta(22f), WARNING, BOX_CENTRE, y);
            }
        }
    }

    private static Color riskColour(String tier) {
        switch (tier) {
            case "LOW": return Draw.GOOD;
            case "MEDIUM": return new Color(0xFFB347);
            default: return Draw.DANGER;
        }
    }
}
