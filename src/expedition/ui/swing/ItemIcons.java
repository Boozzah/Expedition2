/*
 * ItemIcons
 *
 * Small pictures for the items in the hold. The Repair Kit (scrap metal), Caffeine, First
 * Aid Kit and White Monster use Nadia's art. The Sensory Toy and Condensed Oil don't have
 * art yet, so they are drawn here in blocky shapes to sit with the pixel art. Any other
 * item gets its first letter.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

final class ItemIcons {

    private ItemIcons() {
    }

    static BufferedImage icon(String itemName, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // everything is drawn on a 16x16 grid, then each square is size/16 pixels
        int p = Math.max(1, size / 16);
        String name = itemName.toLowerCase();

        String art = artFor(name);
        if (art != null) {
            drawArt(g, Assets.cached(art), size, p);
        } else if (name.contains("sensory") || name.contains("toy")) {
            box(g, p, 7, 2, 2, 12, new Color(0xF5D53A));
            box(g, p, 2, 7, 12, 2, new Color(0xF5D53A));
            box(g, p, 5, 5, 6, 6, new Color(0xF5D53A));
        } else if (name.contains("oil") || name.contains("fuel")) {
            box(g, p, 4, 2, 8, 12, new Color(0x2F2F2F));
            box(g, p, 4, 5, 8, 1, new Color(0x6B6B6B));
            box(g, p, 4, 10, 8, 1, new Color(0x6B6B6B));
            box(g, p, 6, 1, 2, 1, new Color(0x6B6B6B));
        } else {
            Font font = Assets.pixelta(size * 0.7f);
            Draw.centred(g, itemName.substring(0, 1).toUpperCase(), font, Color.WHITE, size / 2, size / 6);
        }
        g.dispose();
        return img;
    }

    // Nadia's picture for an item, or null if she hasn't drawn it
    private static String artFor(String name) {
        if (name.contains("repair")) {
            return "items/repair-kit.png";
        }
        if (name.contains("first aid")) {
            return "items/first-aid-kit.png";
        }
        if (name.contains("monster")) {
            return "items/white-monster.png";
        }
        if (name.contains("caffeine")) {
            return "items/caffeine.png";
        }
        return null;
    }

    // fits the art inside the square with a small margin, keeping its shape, centred
    private static void drawArt(Graphics2D g, BufferedImage art, int size, int margin) {
        int room = size - 2 * margin;
        BufferedImage fitted = art.getWidth() >= art.getHeight()
                ? Assets.scaledToWidth(art, room) : Assets.scaledToHeight(art, room);
        g.drawImage(fitted, (size - fitted.getWidth()) / 2, (size - fitted.getHeight()) / 2, null);
    }

    private static void box(Graphics2D g, int p, int x, int y, int w, int h, Color colour) {
        g.setColor(colour);
        g.fillRect(x * p, y * p, w * p, h * p);
    }
}
