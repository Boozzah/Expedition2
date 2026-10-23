/*
 * EndingScreen
 *
 * The end of a run, from Nadia's "you won" sketch: the story of how it ended in the text
 * box with FINISH GAME under it. Next to it is where the score came from, line by line,
 * which is the only place the run's score is shown in full.
 */
package expedition.ui.swing;

import expedition.engine.EndingReason;
import expedition.ui.screen.EndingInfo;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;

class EndingScreen extends GameScreen {

    static final Object FINISH = new Object();

    private static final int TITLE_TOP = 34;
    private static final Rectangle STORY_BOX = new Rectangle(40, 120, 640, 470);
    private static final Rectangle STORY = new Rectangle(85, 150, 550, 410);
    private static final Rectangle SCORE = new Rectangle(720, 120, 520, 470);
    // the most room a line of the breakdown gets, they close up when there are a lot of them
    private static final int LINE_STEP = 34;
    // the score panel's title, the line of facts and the total take this much of its height
    private static final int PANEL_EXTRAS = 210;
    private static final Color PANEL = new Color(3, 15, 31, 220);

    private final BufferedImage storyBox = Assets.scaled(Assets.cached("ui/text-box.png"), STORY_BOX.width, STORY_BOX.height);
    private final Font titleFont = Assets.pixelta(64f);
    private final Font storyFont = Assets.pixelta(26f);
    private final Font headFont = Assets.pixelta(44f);
    private final Font lineFont = Assets.pixelta(28f);
    private final Font totalFont = Assets.pixelta(44f);

    private EndingInfo ending;
    private BufferedImage background;

    EndingScreen(Consumer<Object> answers) {
        super(answers);
        ImageButton finish = Draw.longButton("FINISH GAME", 320, 72, 46f);
        finish.setLocation((Assets.WIDTH - 320) / 2, 620);
        finish.addActionListener(e -> answer(FINISH));
        add(finish);
    }

    // EDT only. Home is the planet from the menu, every other ending is lost in space
    void show(EndingInfo ending) {
        this.ending = ending;
        background = Assets.scaled(Assets.cached(ending.reason() == EndingReason.HOME
                ? "menu-background.png" : "backgrounds/space-planet.png"), Assets.WIDTH, Assets.HEIGHT);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (ending == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(background, 0, 0, null);
        String title = ending.reason() == EndingReason.HOME ? "You made it home!" : "Game over";
        Draw.centred(g2, title, titleFont, Color.WHITE, Assets.WIDTH / 2, TITLE_TOP);

        g2.drawImage(storyBox, STORY_BOX.x, STORY_BOX.y, null);
        Draw.paragraph(g2, ending.story(), storyFont, Color.WHITE, STORY);

        paintScore(g2);
        g2.dispose();
    }

    // the facts of the run, then a line for each way points were won or lost, then the total
    private void paintScore(Graphics2D g2) {
        g2.setColor(PANEL);
        g2.fillRect(SCORE.x, SCORE.y, SCORE.width, SCORE.height);
        g2.setColor(Draw.LIGHT_BLUE);
        g2.drawRect(SCORE.x, SCORE.y, SCORE.width, SCORE.height);

        int left = SCORE.x + 24;
        int right = SCORE.x + SCORE.width - 24;
        int y = SCORE.y + 18;
        Draw.centred(g2, "Score", headFont, Color.WHITE, SCORE.x + SCORE.width / 2, y);
        y += 60;
        Draw.left(g2, "Planets reached: " + ending.planetsReached() + "    Oil: " + ending.oilCollected(),
                lineFont, Draw.LIGHT_BLUE, left, y);
        y += LINE_STEP + 10;

        int lines = Math.max(1, ending.breakdown().size());
        int step = Math.min(LINE_STEP, (SCORE.height - PANEL_EXTRAS) / lines);
        Font font = lineFont.deriveFont(Math.min(lineFont.getSize2D(), step * 0.82f));
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        for (Map.Entry<String, Integer> line : ending.breakdown().entrySet()) {
            int points = line.getValue();
            String value = (points > 0 ? "+" : "") + points;
            Draw.left(g2, line.getKey(), font, Color.WHITE, left, y);
            Draw.left(g2, value, font, points < 0 ? Draw.DANGER : Draw.GOOD, right - fm.stringWidth(value), y);
            y += step;
        }

        g2.setColor(Draw.LIGHT_BLUE);
        g2.drawLine(left, SCORE.y + SCORE.height - 70, right, SCORE.y + SCORE.height - 70);
        String total = String.valueOf(ending.score());
        g2.setFont(totalFont);
        Draw.left(g2, "Total", totalFont, Color.WHITE, left, SCORE.y + SCORE.height - 56);
        Draw.left(g2, total, totalFont, Color.WHITE, right - g2.getFontMetrics().stringWidth(total), SCORE.y + SCORE.height - 56);
    }
}
