/*
 * BookPanel
 *
 * Nadia's open book for How to Play. Two pages show at a time, and the arrow in the
 * bottom corner turns to the next two. Used on its own screen from the main menu, and
 * in a window over the game when the player picks Help in the in-game menu.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;

class BookPanel extends JPanel {

    // the book's size in the window, from Nadia's example
    static final int WIDTH = 790;
    static final int HEIGHT = 625;

    private static final String TITLE = "How to play";
    // the white of each page, inside the book
    private static final Rectangle LEFT_PAGE = new Rectangle(62, 40, 310, 520);
    private static final Rectangle RIGHT_PAGE = new Rectangle(418, 40, 310, 520);
    private static final int TITLE_HEIGHT = 70;
    private static final Color TITLE_TEXT = new Color(0x3A4654);
    // a grey that is still easy to read on the white page
    private static final Color PAGE_TEXT = new Color(0x7D838B);

    private static final int ARROW_W = 48;
    private static final int ARROW_H = 34;
    private static final int ARROW_MARGIN = 30;

    private final BufferedImage book = Assets.scaled(Assets.cached("ui/book.png"), WIDTH, HEIGHT);
    private final Font titleFont = Assets.pixelta(46f);
    private final Font pageFont = Assets.pixelta(28f);
    private final ImageButton next;
    private final ImageButton back;

    private List<String> pages = List.of();
    // the left page of the two showing
    private int spread = 0;

    BookPanel() {
        setLayout(null);
        setOpaque(false);
        setSize(WIDTH, HEIGHT);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        BufferedImage arrow = arrow();
        next = new ImageButton(arrow, "Next page");
        next.setLocation(RIGHT_PAGE.x + RIGHT_PAGE.width - ARROW_W - ARROW_MARGIN / 2,
                RIGHT_PAGE.y + RIGHT_PAGE.height - ARROW_H - ARROW_MARGIN / 2);
        next.addActionListener(e -> turn(2));
        add(next);

        back = new ImageButton(mirrored(arrow), "Previous page");
        back.setLocation(LEFT_PAGE.x + ARROW_MARGIN / 2, LEFT_PAGE.y + LEFT_PAGE.height - ARROW_H - ARROW_MARGIN / 2);
        back.addActionListener(e -> turn(-2));
        add(back);
    }

    // EDT only. Opens the book at the first page
    void show(List<String> pages) {
        this.pages = List.copyOf(pages);
        spread = 0;
        update();
    }

    private void turn(int by) {
        spread = Math.max(0, Math.min(spread + by, pages.size() - 1));
        update();
    }

    private void update() {
        next.setVisible(spread + 2 < pages.size());
        back.setVisible(spread > 0);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(book, 0, 0, null);

        Rectangle left = LEFT_PAGE;
        // the very first page has the title at the top, so its text starts lower down
        if (spread == 0) {
            Draw.centred(g2, TITLE, titleFont, TITLE_TEXT, left.x + left.width / 2, left.y + 30);
            left = new Rectangle(left.x, left.y + TITLE_HEIGHT, left.width, left.height - TITLE_HEIGHT);
        }
        if (spread < pages.size()) {
            Draw.paragraph(g2, pages.get(spread), pageFont, PAGE_TEXT, inset(left));
        }
        if (spread + 1 < pages.size()) {
            Draw.paragraph(g2, pages.get(spread + 1), pageFont, PAGE_TEXT, inset(RIGHT_PAGE));
        }
        g2.dispose();
    }

    // leaves room round the edge of the page, and at the bottom for the arrow
    private Rectangle inset(Rectangle page) {
        return new Rectangle(page.x + 14, page.y + 10, page.width - 28, page.height - ARROW_H - 30);
    }

    // a dark blocky arrow pointing right, like the one in Nadia's example
    private static BufferedImage arrow() {
        BufferedImage img = new BufferedImage(ARROW_W, ARROW_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x3A4654));
        int shaft = ARROW_H / 3;
        g.fillRect(0, (ARROW_H - shaft) / 2, ARROW_W - ARROW_H / 2, shaft);
        int[] xs = {ARROW_W - ARROW_H / 2 - 4, ARROW_W, ARROW_W - ARROW_H / 2 - 4};
        int[] ys = {0, ARROW_H / 2, ARROW_H};
        g.fillPolygon(xs, ys, 3);
        g.dispose();
        return img;
    }

    private static BufferedImage mirrored(BufferedImage img) {
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        // a negative width draws it back to front
        g.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
        g.dispose();
        return flipped;
    }
}
