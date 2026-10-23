/*
 * HighScoresPanel
 *
 * The high-score screen, laid out from Nadia's sketch (Ideas/23-highscores.jpg): a title,
 * the best runs in a table down the middle, best first, and a button in the bottom right
 * that goes back to the main menu. It sits on the same planet background as the menu.
 *
 * Like MainMenuPanel it doesn't decide anything itself. The Back button just calls
 * whatever SwingView handed it, which tells the waiting engine to go back to the menu.
 */
package expedition.ui.swing;

import expedition.engine.HighScore;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;

class HighScoresPanel extends JPanel {

    private static final String TITLE = "HIGH SCORES";
    private static final float TITLE_SIZE = 110f;
    private static final int TITLE_TOP = 50;            // top of the letters

    // the dark box behind the table, so the rows are readable over the planet art
    private static final Color BOX = new Color(0, 0, 0, 180);
    private static final int BOX_LEFT = 140;
    private static final int BOX_TOP = 180;
    private static final int BOX_WIDTH = 1000;
    private static final int BOX_HEIGHT = 380;
    private static final int BOX_ROUNDING = 24;

    private static final float ROW_TEXT_SIZE = 34f;
    private static final Color HEADER_TEXT = new Color(0xA6CEE6);   // same blue as the menu buttons
    private static final int HEADER_BASELINE = 225;
    private static final int FIRST_ROW_BASELINE = 275;
    private static final int ROW_STEP = 36;

    // left edge of each column. SCORE is lined up on its right edge instead, so the numbers line up
    private static final int COL_RANK = 170;
    private static final int COL_DATE = 230;
    // ENDING has room for STRANDED ON PLANET, the longest one, before PLANETS starts
    private static final int COL_ENDING = 480;
    private static final int COL_PLANETS = 830;
    private static final int COL_SCORE_RIGHT = 1100;

    private static final int BUTTON_MARGIN = 22;        // from the right and bottom edges

    private final BufferedImage background = Assets.load("menu-background.png");
    private final Font titleFont = Assets.pixelta(TITLE_SIZE);
    private final Font rowFont = Assets.pixelta(ROW_TEXT_SIZE);

    // only touched on the EDT, replaced each time the screen is shown
    private List<HighScore> scores = List.of();

    HighScoresPanel(Runnable onBack) {
        setLayout(null);   // fixed 1280x720 art, everything is placed by hand
        setPreferredSize(new Dimension(Assets.WIDTH, Assets.HEIGHT));

        // the same small BACK as the other menu screens
        ImageButton back = Draw.smallButton("BACK");
        back.setLocation(Assets.WIDTH - BUTTON_MARGIN - back.getWidth(),
                Assets.HEIGHT - BUTTON_MARGIN - back.getHeight());
        back.addActionListener(e -> onBack.run());
        add(back);
    }

    // EDT only. Called by SwingView just before the screen is shown
    void showScores(List<HighScore> scores) {
        this.scores = scores;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(background, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintTitle(g2);

        g2.setColor(BOX);
        g2.fillRoundRect(BOX_LEFT, BOX_TOP, BOX_WIDTH, BOX_HEIGHT, BOX_ROUNDING, BOX_ROUNDING);

        g2.setFont(rowFont);
        if (scores.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.drawString("No finished runs yet. Play one to get on the board!",
                    COL_DATE, FIRST_ROW_BASELINE);
        } else {
            g2.setColor(HEADER_TEXT);
            paintRow(g2, HEADER_BASELINE, "#", "DATE", "ENDING", "PLANETS", "SCORE");
            g2.setColor(Color.WHITE);
            for (int i = 0; i < scores.size(); i++) {
                HighScore s = scores.get(i);
                paintRow(g2, FIRST_ROW_BASELINE + i * ROW_STEP, String.valueOf(i + 1),
                        s.when(), s.ending(), String.valueOf(s.planetsReached()),
                        String.valueOf(s.score()));
            }
        }
        g2.dispose();
    }

    // centred along the top, the same way MainMenuPanel draws its title
    private void paintTitle(Graphics2D g2) {
        g2.setFont(titleFont);
        g2.setColor(Color.WHITE);
        Rectangle2D ink = titleFont.createGlyphVector(g2.getFontRenderContext(), TITLE)
                .getVisualBounds();
        double x = (Assets.WIDTH - ink.getWidth()) / 2 - ink.getX();
        double y = TITLE_TOP - ink.getY();
        g2.drawString(TITLE, (float) x, (float) y);
    }

    private void paintRow(Graphics2D g2, int baseline, String rank, String date,
                          String ending, String planets, String score) {
        g2.drawString(rank, COL_RANK, baseline);
        g2.drawString(date, COL_DATE, baseline);
        g2.drawString(ending, COL_ENDING, baseline);
        g2.drawString(planets, COL_PLANETS, baseline);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(score, COL_SCORE_RIGHT - fm.stringWidth(score), baseline);
    }
}
