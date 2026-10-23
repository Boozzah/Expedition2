/*
 * Overlay
 *
 * Darkens the whole window and holds a box in the middle of it, for the hazard and jump
 * event popups and anything else that has to be read before carrying on. It sits in the
 * frame's layered pane above the screens, and soaks up every click so nothing behind it
 * can be pressed while it's open.
 */
package expedition.ui.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import javax.swing.JComponent;

class Overlay extends JComponent {

    private static final Color DIM = new Color(0, 0, 0, 150);

    private JComponent box;

    Overlay() {
        setBounds(0, 0, Assets.WIDTH, Assets.HEIGHT);
        setLayout(null);
        setVisible(false);
        // an empty listener is enough to stop clicks reaching the screen underneath
        addMouseListener(new MouseAdapter() { });
    }

    // EDT only. Shows the box centred in the window, replacing any box already showing
    void show(JComponent content) {
        if (box != null) {
            remove(box);
        }
        box = content;
        box.setLocation((Assets.WIDTH - box.getWidth()) / 2, (Assets.HEIGHT - box.getHeight()) / 2);
        add(box);
        setVisible(true);
        revalidate();
        repaint();
    }

    void close() {
        if (box != null) {
            remove(box);
            box = null;
        }
        setVisible(false);
    }

    boolean isShowing(JComponent content) {
        return isVisible() && box == content;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(DIM);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
