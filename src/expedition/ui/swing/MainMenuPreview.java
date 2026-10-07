/*
 * MainMenuPreview
 *
 * Opens the main menu on its own, without the engine, to check it against Nadia's
 * example. Clicking a button prints the choice; QUIT GAME closes the window.
 *
 * Run: java -cp build/classes expedition.ui.swing.MainMenuPreview
 */
package expedition.ui.swing;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class MainMenuPreview {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Expedition");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new MainMenuPanel(choice -> {
                System.out.println("Clicked: " + choice);
                if (choice == MainMenuPanel.Choice.QUIT) {
                    frame.dispose();
                }
            }));
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
