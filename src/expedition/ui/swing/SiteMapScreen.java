/*
 * SiteMapScreen
 *
 * Choosing where to drill, from Nadia's site map sketch: the planet with a marker for
 * each site, and a card with the details of the site clicked. DRILL goes to the site,
 * Leave Planet takes the team back up. A closed site's marker is red and can't be drilled.
 * Moving to another site after the first costs fuel, which the card says.
 */
package expedition.ui.swing;

import expedition.ui.screen.SiteInfo;
import expedition.world.PlanetArchetype;
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
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

class SiteMapScreen extends GameScreen {

    // answered when the player leaves the planet
    static final Object LEAVE = new Object();

    private static final int TITLE_TOP = 40;
    private static final Rectangle PLANET = new Rectangle(100, 130, 500, 500);
    // where the planet's corner view is cut from to make the round planet
    private static final Rectangle PLANET_ART = new Rectangle(760, 200, 520, 520);
    // the site markers on the planet, like Nadia's sketch
    private static final Point[] MARKERS = {new Point(355, 250), new Point(205, 440), new Point(470, 520)};
    private static final int MARKER = 60;

    private static final Rectangle CARD = new Rectangle(720, 140, 450, 380);
    private static final Color CARD_FILL = new Color(0x5D6D7B);
    private static final Color CARD_EDGE = new Color(0x3E4A56);
    private static final Color MARKER_COLOUR = new Color(0x5CE1E6);

    private final Font titleFont = Assets.pixelta(60f);
    private final Font nameFont = Assets.pixelta(44f);
    private final Font riskFont = Assets.pixelta(38f);
    private final Font detailFont = Assets.pixelta(30f);
    private final Font hintFont = Assets.pixelta(30f);
    private final ImageButton drill;

    private BufferedImage planet;
    private List<SiteInfo> sites = List.of();
    private int moveCost;
    private int picked = -1;

    SiteMapScreen(Consumer<Object> answers) {
        super(answers);

        drill = Draw.longButton("DRILL", 260, 70, 46f);
        drill.setLocation(CARD.x + (CARD.width - 260) / 2, CARD.y + CARD.height - 90);
        drill.addActionListener(e -> answer(picked));
        add(drill);

        ImageButton leave = Draw.longButton("LEAVE PLANET", 340, 76, 44f);
        leave.setLocation(CARD.x + (CARD.width - 340) / 2, CARD.y + CARD.height + 40);
        leave.addActionListener(e -> answer(LEAVE));
        add(leave);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < sites.size() && i < MARKERS.length; i++) {
                    if (MARKERS[i].distance(e.getPoint()) <= MARKER) {
                        picked = i;
                        update();
                    }
                }
            }
        });
    }

    // EDT only. Starts with the first open site's card showing
    void show(PlanetArchetype archetype, List<SiteInfo> sites, int moveCost) {
        planet = roundPlanet(Assets.planet(archetype));
        this.sites = sites;
        this.moveCost = moveCost;
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

    // there is no art of the whole planet, so the planet part of its corner view is cut
    // into a circle
    private BufferedImage roundPlanet(BufferedImage corner) {
        BufferedImage round = new BufferedImage(PLANET.width, PLANET.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = round.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new Ellipse2D.Double(0, 0, PLANET.width, PLANET.height));
        g.drawImage(corner, 0, 0, PLANET.width, PLANET.height,
                PLANET_ART.x, PLANET_ART.y, PLANET_ART.x + PLANET_ART.width, PLANET_ART.y + PLANET_ART.height, null);
        g.dispose();
        return round;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sites.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Draw.centred(g2, "Choose your Drill Site", titleFont, Color.WHITE, Assets.WIDTH / 2, TITLE_TOP);
        g2.drawImage(planet, PLANET.x, PLANET.y, null);

        for (int i = 0; i < sites.size() && i < MARKERS.length; i++) {
            paintMarker(g2, MARKERS[i], sites.get(i).open() ? MARKER_COLOUR : Draw.DANGER, i == picked);
        }
        Draw.centred(g2, "Click a site to see it", hintFont, Draw.LIGHT_BLUE, PLANET.x + PLANET.width / 2, PLANET.y + PLANET.height + 22);

        paintCard(g2);
        g2.dispose();
    }

    // Nadia's square brackets round a dot
    private void paintMarker(Graphics2D g2, Point at, Color colour, boolean selected) {
        int half = MARKER / 2;
        int arm = MARKER / 4;
        g2.setColor(colour);
        g2.setStroke(new BasicStroke(selected ? 7 : 5));
        g2.drawPolyline(new int[]{at.x - half + arm, at.x - half, at.x - half, at.x - half + arm},
                new int[]{at.y - half, at.y - half, at.y + half, at.y + half}, 4);
        g2.drawPolyline(new int[]{at.x + half - arm, at.x + half, at.x + half, at.x + half - arm},
                new int[]{at.y - half, at.y - half, at.y + half, at.y + half}, 4);
        g2.setColor(Color.BLACK);
        g2.fillRect(at.x - 6, at.y - 6, 12, 12);
    }

    private void paintCard(Graphics2D g2) {
        g2.setColor(CARD_EDGE);
        g2.fillRect(CARD.x - 6, CARD.y - 6, CARD.width + 12, CARD.height + 12);
        g2.setColor(CARD_FILL);
        g2.fillRect(CARD.x, CARD.y, CARD.width, CARD.height);
        if (picked < 0) {
            Draw.paragraph(g2, "Every site here is closed.", detailFont, Color.WHITE,
                    new Rectangle(CARD.x + 20, CARD.y + 20, CARD.width - 40, 200));
            return;
        }

        SiteInfo site = sites.get(picked);
        int centre = CARD.x + CARD.width / 2;
        Draw.centred(g2, site.name(), nameFont, Color.WHITE, centre, CARD.y + 22);
        if (!site.open()) {
            Draw.centred(g2, "SITE IS CLOSED", riskFont, Draw.DANGER, centre, CARD.y + 90);
            return;
        }
        Draw.centred(g2, site.tier() + " RISK", riskFont, riskColour(site.tier()), centre, CARD.y + 86);
        Draw.centred(g2, "Est. oil " + site.estimatedOil().replace("oil -", "").replace("~", "").trim(), detailFont, Color.WHITE, centre, CARD.y + 146);
        Draw.centred(g2, "Hazard chance " + site.riskStart() + "% - " + site.riskDeep() + "%", detailFont, Color.WHITE, centre, CARD.y + 192);
        String cost = moveCost > 0 ? "Costs " + moveCost + " fuel to move here" : "The first site is free";
        Draw.centred(g2, cost, detailFont, Draw.LIGHT_BLUE, centre, CARD.y + 238);
    }

    private static Color riskColour(String tier) {
        switch (tier) {
            case "LOW": return Draw.GOOD;
            case "MEDIUM": return new Color(0xFFB347);
            default: return Draw.DANGER;
        }
    }
}
