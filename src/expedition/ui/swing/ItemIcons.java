/*
 * ItemIcons
 *
 * Small pictures for the items in the hold. There is no item art yet, so each one is
 * drawn here in blocky shapes to sit with the pixel art: a wrench for the Repair Kit,
 * a cross for the First Aid Kit, a can for the White Monster and so on. Any item without
 * its own picture gets its first letter. Swap these for Nadia's art once it is drawn.
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

        if (name.contains("repair")) {
            BufferedImage wrench = Assets.scaledToHeight(Assets.cached("ui/wrench.png"), size - 2 * p);
            g.drawImage(wrench, (size - wrench.getWidth()) / 2, p, null);
        } else if (name.contains("first aid")) {
            box(g, p, 2, 3, 12, 10, new Color(0xF2F2F2));
            box(g, p, 7, 4, 2, 8, new Color(0xD93A32));
            box(g, p, 4, 7, 8, 2, new Color(0xD93A32));
        } else if (name.contains("monster")) {
            box(g, p, 5, 1, 6, 14, new Color(0xEDEDED));
            box(g, p, 5, 1, 6, 1, new Color(0x9A9A9A));
            box(g, p, 6, 5, 1, 6, new Color(0x4CC24C));
            box(g, p, 9, 5, 1, 6, new Color(0x4CC24C));
            box(g, p, 7, 6, 2, 2, new Color(0x4CC24C));
        } else if (name.contains("caffeine")) {
            box(g, p, 3, 6, 5, 4, new Color(0xF2F2F2));
            box(g, p, 8, 6, 5, 4, new Color(0x8B5A2B));
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

    private static void box(Graphics2D g, int p, int x, int y, int w, int h, Color colour) {
        g.setColor(colour);
        g.fillRect(x * p, y * p, w * p, h * p);
    }
}
