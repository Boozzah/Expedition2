/*
 * Assets
 *
 * Loads Nadia's art and the Pixelta font. They live in src/expedition/ui/swing/assets
 * and are loaded from the classpath, so they get bundled into the jar with the classes.
 *
 * Nadia drew every screen on a 4000x2250 canvas. The window is 1280x720, so her
 * assets are 1280/4000 = 0.32 of their canvas size. The menu art is exported at
 * window size, which load() reads as it is.
 *
 * The game screen art (crew, planets, ship, bars, buttons) is kept at the size it was
 * drawn and cropped to its drawn pixels, so each screen asks for it at the size it
 * needs with scaled(). The crew, planet and bar helpers turn game values into the
 * right file, so the screens don't need to know the file names.
 */
package expedition.ui.swing;

import expedition.world.PlanetArchetype;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

final class Assets {

    // the ways a crew member is drawn. STANDING is the plain sprite, SELECTED has the
    // orange outline for whoever's stats are showing, ABOARD the yellow one for anyone
    // already in the shuttle, WINDOW and PROFILE are the round head shots
    enum Pose { STANDING, SELECTED, ABOARD, DEAD, WINDOW, PROFILE }

    // the stat bar art has 10 segments
    private static final int BAR_SEGMENTS = 10;

    // the health line changes at these percentages
    private static final int HEALTH_HIGH = 60;
    private static final int HEALTH_MEDIUM = 30;

    // the bigger art is read once and kept, every screen that shows a crew member
    // or a planet would otherwise read the same file again
    private static final Map<String, BufferedImage> loaded = new HashMap<>();

    // window size, the size of Nadia's background
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;

    // Nadia's canvas was 4000 wide
    static final double SCALE = WIDTH / 4000.0;

    private static Font pixelta;

    private Assets() {
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

    // Loads an image once and hands back the same one after that. Only for art that is
    // never drawn on, since every caller shares it
    static BufferedImage cached(String name) {
        return loaded.computeIfAbsent(name, Assets::load);
    }

    // Resizes art to an exact size in window pixels. Nearest neighbour keeps the pixel
    // art's hard edges, smoothing would blur them
    static BufferedImage scaled(BufferedImage original, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(original, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    // Resizes art to a height, keeping its shape
    static BufferedImage scaledToHeight(BufferedImage original, int h) {
        int w = Math.max(1, original.getWidth() * h / original.getHeight());
        return scaled(original, w, h);
    }

    // Resizes art to a width, keeping its shape
    static BufferedImage scaledToWidth(BufferedImage original, int w) {
        int h = Math.max(1, original.getHeight() * w / original.getWidth());
        return scaled(original, w, h);
    }

    // a crew member's sprite, by their name in crew.txt. e.g. Alexander, ABOARD is
    // crew/alexander-aboard.png
    static BufferedImage crew(String name, Pose pose) {
        return cached("crew/" + name.toLowerCase() + "-" + pose.name().toLowerCase() + ".png");
    }

    // the corner view of a planet. There is one per archetype, so both frozen
    // planets share the same view
    static BufferedImage planet(PlanetArchetype archetype) {
        return cached("planets/" + archetype.name().toLowerCase() + ".png");
    }

    // the cyan composure/fatigue bar with as many segments filled as the stat, rounded
    // to the nearest 10%
    static BufferedImage statBar(int percent) {
        int filled = Math.round(Math.max(0, Math.min(100, percent)) * BAR_SEGMENTS / 100f);
        return cached("bars/stat-" + filled + ".png");
    }

    // the heartbeat line, which flattens out as health drops
    static BufferedImage healthBar(int health, boolean alive) {
        if (!alive) {
            return cached("bars/health-dead.png");
        }
        if (health >= HEALTH_HIGH) {
            return cached("bars/health-high.png");
        }
        if (health >= HEALTH_MEDIUM) {
            return cached("bars/health-medium.png");
        }
        return cached("bars/health-low.png");
    }
}
