/*
 * Assets
 *
 * Loads Nadia's art and the Pixelta font. They live in src/expedition/ui/swing/assets
 * and are loaded from the classpath, so they get bundled into the jar with the classes.
 *
 * Nadia drew every screen on a 4000x2250 canvas. The window is 1280x720, so her
 * assets are 1280/4000 = 0.32 of their canvas size. She now exports them at window
 * size, which load() reads as they are. image() shrinks any that are still full size.
 */
package expedition.ui.swing;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

final class Assets {

    // window size, the size of Nadia's background
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;

    // Nadia's canvas was 4000 wide
    static final double SCALE = WIDTH / 4000.0;

    private static Font pixelta;

    private Assets() {
    }

    // Loads an image and shrinks it to window scale.
    // pixelArt = true keeps hard pixel edges (nearest neighbour). false averages
    // the pixels, for art with smooth edges like the rounded icon buttons
    static BufferedImage image(String name, boolean pixelArt) {
        BufferedImage original = load(name);
        int w = (int) Math.round(original.getWidth() * SCALE);
        int h = (int) Math.round(original.getHeight() * SCALE);

        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        if (pixelArt) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(original, 0, 0, w, h, null);
        } else {
            g.drawImage(original.getScaledInstance(w, h, Image.SCALE_SMOOTH), 0, 0, null);
        }
        g.dispose();
        return scaled;
    }

    // Loads an image at its file size, for art that is already window sized (backgrounds)
    static BufferedImage load(String name) {
        URL url = Assets.class.getResource("assets/" + name);
        if (url == null) {
            throw new IllegalStateException("Missing asset: " + name);
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read asset: " + name, e);
        }
    }

    // Pixelta at the given size. Sizes are in window pixels (1pt = 1px in Swing)
    static Font pixelta(float size) {
        if (pixelta == null) {
            try (InputStream in = Assets.class.getResourceAsStream("assets/pixelta-regular.ttf")) {
                if (in == null) {
                    throw new IllegalStateException("Missing asset: pixelta-regular.ttf");
                }
                pixelta = Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (IOException | FontFormatException e) {
                throw new IllegalStateException("Could not read the Pixelta font", e);
            }
        }
        return pixelta.deriveFont(size);
    }
}
