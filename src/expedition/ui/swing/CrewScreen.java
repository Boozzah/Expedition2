/*
 * CrewScreen
 *
 * Choosing who goes down, from Nadia's crew selection examples. The six crew stand in a
 * row with the three seat shuttle under them and a stat card on the right.
 * - Hovering over someone shows their card and gives them the orange outline
 * - Clicking them puts them in the next empty window of the shuttle, and they keep the
 *   yellow outline while they're aboard
 * - Clicking them again (or their window) takes them back out, back to only the hover outline
 * - Anyone dead is drawn dead, and anyone who can't go says why on their card
 * GO sends the team down once there are 2 or 3 aboard.
 */
package expedition.ui.swing;

import expedition.ui.screen.CrewCard;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class CrewScreen extends GameScreen {

    // answered when the player backs out and nobody goes down
    static final Object STAND_DOWN = new Object();

    private static final int TITLE_TOP = 52;

    // the row of crew
    private static final int CREW_LEFT = 22;
    private static final int CREW_STEP = 133;
    private static final int CREW_BOTTOM = 385;
    private static final int CREW_HEIGHT = 225;

    // the shuttle and its three windows, centres measured from Nadia's example
    private static final Rectangle SHUTTLE = new Rectangle(45, 372, 755, 333);
    private static final Point[] WINDOWS = {new Point(233, 528), new Point(357, 528), new Point(482, 528)};
    private static final int WINDOW_RADIUS = 56;

    // the stat card
    private static final Rectangle CARD = new Rectangle(872, 146, 348, 365);
    private static final int NAME_TOP = 92;
    private static final int ICON = 70;
    private static final int ROW_STEP = 82;
    private static final Color CARD_FILL = new Color(0x5D6D7B);
    private static final Color CARD_EDGE = new Color(0x3E4A56);
    private static final Color CARD_TEXT = new Color(0xC9D3DB);
    private static final Color WARNING = new Color(0xFFB347);

    private static final Rectangle GO = new Rectangle(968, 540, 160, 160);

    private static final float UNFIT_ALPHA = 0.55f;

    private final BufferedImage shuttle = Assets.scaled(Assets.cached("ship/shuttle.png"), SHUTTLE.width, SHUTTLE.height);
    private final BufferedImage composure = Assets.scaled(Assets.cached("ui/composure-icon.png"), ICON, ICON);
    private final BufferedImage fatigue = Assets.scaled(Assets.cached("ui/fatigue-icon.png"), ICON, ICON);
    private final Font titleFont = Assets.pixelta(46f);
    private final Font nameFont = Assets.pixelta(52f);
    private final Font statFont = Assets.pixelta(20f);
    private final Font abilityTitleFont = Assets.pixelta(24f);
    private final Font abilityFont = Assets.pixelta(22f);
    private final ImageButton go;

    private List<CrewCard> crew = List.of();
    private int min;
    private int max;
    // positions in the crew list: who is in each window (-1 for empty), whose card shows,
    // and who the mouse is over (-1 for nobody). The card stays on the last person hovered
    private final int[] seats = {-1, -1, -1};
    private int viewing = 0;
    private int hovered = -1;

    CrewScreen(Consumer<Object> answers) {
        super(answers);

        go = Draw.button("ui/go-button.png", GO.width, GO.height, "GO", 72f, Color.WHITE);
        go.setLocation(GO.x, GO.y);
        go.addActionListener(e -> answer(aboard()));
        add(go);

        ImageButton back = Draw.smallButton("BACK");
        back.setToolTipText("Nobody goes down");
        back.setLocation(22, 22);
        back.addActionListener(e -> answer(STAND_DOWN));
        add(back);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicked(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                hover(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover(new Point(-1, -1));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // EDT only. Starts with an empty shuttle and the first crew member's card showing
    void show(List<CrewCard> crew, int min, int max) {
        this.crew = crew;
        this.min = min;
        this.max = Math.min(max, seats.length);
        for (int i = 0; i < seats.length; i++) {
            seats[i] = -1;
        }
        viewing = 0;
        hovered = -1;
        update();
    }

    // who is aboard, in window order
    private List<Integer> aboard() {
        List<Integer> chosen = new ArrayList<>();
        for (int seat : seats) {
            if (seat >= 0) {
                chosen.add(seat);
            }
        }
        return chosen;
    }

    // the crew member at p, or -1
    private int crewAt(Point p) {
        for (int i = 0; i < crew.size(); i++) {
            if (crewArea(i).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    // the mouse moved: whoever it is over gets the outline and their card shows
    private void hover(Point p) {
        int over = crewAt(p);
        setCursor(Cursor.getPredefinedCursor(over >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        if (over == hovered) {
            return;
        }
        hovered = over;
        if (over >= 0) {
            viewing = over;
        }
        repaint();
    }

    private void clicked(Point p) {
        // a crew member: in if they're out, out if they're in
        int i = crewAt(p);
        if (i >= 0) {
            viewing = i;
            toggle(i);
            return;
        }
        // a window: takes whoever is in it back out
        for (int w = 0; w < WINDOWS.length && w < max; w++) {
            if (WINDOWS[w].distance(p) <= WINDOW_RADIUS && seats[w] >= 0) {
                seats[w] = -1;
                update();
                return;
            }
        }
    }

    // Puts a crew member in the first empty window, or takes them out if they're already in.
    // Nobody unfit can go in, their card already says why, and a full shuttle takes nobody else
    private void toggle(int i) {
        for (int w = 0; w < seats.length; w++) {
            if (seats[w] == i) {
                seats[w] = -1;
                update();
                return;
            }
        }
        if (!crew.get(i).canGoDown()) {
            return;
        }
        for (int w = 0; w < max; w++) {
            if (seats[w] < 0) {
                seats[w] = i;
                update();
                return;
            }
        }
    }

    private void update() {
        int count = aboard().size();
        go.setEnabled(count >= min && count <= max);
        repaint();
    }

    private Rectangle crewArea(int i) {
        return new Rectangle(CREW_LEFT + i * CREW_STEP, CREW_BOTTOM - CREW_HEIGHT, CREW_STEP - 8, CREW_HEIGHT);
    }

    // e.g. "CHOOSE 3 or 2 CREW MEMBERS", then "CHOOSE 2 or 1 MORE CREW MEMBERS" once one is aboard
    private String title() {
        int count = aboard().size();
        if (count >= max) {
            return "PRESS GO";
        }
        int fewest = Math.max(0, min - count);
        int most = max - count;
        String more = count == 0 ? "" : "MORE ";
        if (fewest == 0) {
            return "CHOOSE " + most + " " + more + "OR PRESS GO";
        }
        return "CHOOSE " + most + " or " + fewest + " " + more + "CREW MEMBERS";
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (crew.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Draw.centred(g2, title(), titleFont, Color.WHITE, Assets.WIDTH / 2, TITLE_TOP);

        List<Integer> chosen = aboard();
        for (int i = 0; i < crew.size(); i++) {
            paintCrewMember(g2, i, chosen.contains(i));
        }

        g2.drawImage(shuttle, SHUTTLE.x, SHUTTLE.y, null);
        for (int w = 0; w < WINDOWS.length; w++) {
            if (seats[w] >= 0) {
                BufferedImage face = Assets.scaled(Assets.crew(crew.get(seats[w]).name(), Assets.Pose.WINDOW),
                        2 * WINDOW_RADIUS, (int) (2 * WINDOW_RADIUS * 540 / 495.0));
                g2.drawImage(face, WINDOWS[w].x - WINDOW_RADIUS, WINDOWS[w].y - face.getHeight() / 2, null);
            }
        }

        paintCard(g2, crew.get(viewing));
        g2.dispose();
    }

    private void paintCrewMember(Graphics2D g2, int i, boolean isAboard) {
        CrewCard c = crew.get(i);
        Assets.Pose pose;
        if (!c.alive()) {
            pose = Assets.Pose.DEAD;
        } else if (isAboard) {
            pose = Assets.Pose.ABOARD;
        } else if (i == hovered) {
            pose = Assets.Pose.SELECTED;
        } else {
            pose = Assets.Pose.STANDING;
        }
        BufferedImage sprite = Assets.scaledToHeight(Assets.crew(c.name(), pose), CREW_HEIGHT);
        Rectangle area = crewArea(i);
        int x = area.x + (area.width - sprite.getWidth()) / 2;
        Graphics2D sg = (Graphics2D) g2.create();
        // alive but not fit to go: faded, the card says why
        if (c.alive() && !c.canGoDown()) {
            sg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, UNFIT_ALPHA));
        }
        sg.drawImage(sprite, x, CREW_BOTTOM - sprite.getHeight(), null);
        sg.dispose();
    }

    // The heartbeat line, centred in the box. Every line is drawn at the same scale, the one
    // that fits the tallest (full health) in the box. Stretching each one to fill the box
    // turned the thin flatline into a solid green block
    private void paintHealthLine(Graphics2D g2, CrewCard c, int x, int y, int w, int h) {
        BufferedImage line = Assets.healthBar(c.health(), c.alive());
        BufferedImage tallest = Assets.healthBar(100, true);
        double scale = Math.min((double) w / tallest.getWidth(), (double) h / tallest.getHeight());
        BufferedImage sized = Assets.scaled(line, Math.max(1, (int) (line.getWidth() * scale)), Math.max(1, (int) (line.getHeight() * scale)));
        g2.drawImage(sized, x + (w - sized.getWidth()) / 2, y + (h - sized.getHeight()) / 2, null);
    }

    private void paintCard(Graphics2D g2, CrewCard c) {
        Draw.centred(g2, c.name(), nameFont, Color.WHITE, CARD.x + CARD.width / 2, NAME_TOP);
        g2.setColor(CARD_EDGE);
        g2.fillRect(CARD.x - 6, CARD.y - 6, CARD.width + 12, CARD.height + 12);
        g2.setColor(CARD_FILL);
        g2.fillRect(CARD.x, CARD.y, CARD.width, CARD.height);

        int iconX = CARD.x + 22;
        int barX = iconX + ICON + 12;
        int barW = CARD.x + CARD.width - 18 - barX;
        int y = CARD.y + 18;

        // health: the heartbeat line, which flattens out as health drops
        BufferedImage profile = Assets.scaled(Assets.crew(c.name(), Assets.Pose.PROFILE), ICON, (int) (ICON * 540 / 495.0));
        g2.drawImage(profile, iconX, y - 4, null);
        paintHealthLine(g2, c, barX, y + 4, barW, 46);
        Draw.centred(g2, (c.alive() ? c.health() : 0) + "% Health", statFont, CARD_TEXT, barX + barW / 2, y + 56);
        y += ROW_STEP;

        g2.drawImage(composure, iconX, y, null);
        g2.drawImage(Assets.scaled(Assets.statBar(c.composure()), barW, 30), barX, y + 14, null);
        Draw.centred(g2, c.composure() + "% Composure", statFont, CARD_TEXT, barX + barW / 2, y + 52);
        y += ROW_STEP;

        g2.drawImage(fatigue, iconX, y, null);
        g2.drawImage(Assets.scaled(Assets.statBar(c.fatigue()), barW, 30), barX, y + 14, null);
        Draw.centred(g2, c.fatigue() + "% Fatigue", statFont, CARD_TEXT, barX + barW / 2, y + 52);
        y += ROW_STEP;

        Draw.centred(g2, c.role().title() + " Ability:", abilityTitleFont, Color.WHITE, CARD.x + CARD.width / 2, y);
        String detail = c.role().ability();
        Color detailColour = Color.WHITE;
        if (!c.canGoDown()) {
            detail = c.alive() ? "Can't go down: " + c.ineligibleReason() : "Lost on an earlier expedition";
            detailColour = Draw.DANGER;
        } else if (c.fatiguedAfterTrip()) {
            detail = "Too tired to use their ability on this trip";
            detailColour = WARNING;
        }
        Draw.paragraph(g2, detail, abilityFont, detailColour, new Rectangle(CARD.x + 14, y + 28, CARD.width - 28, 50));
    }
}
