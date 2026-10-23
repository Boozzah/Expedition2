/*
 * GameView
 *
 * Everything the game needs from a user interface, show a line of text,
 * and ask the player for an input. GameEngine, ExpeditionSelector, DrillSession and RunLogWriter
 * only ever talk to this interface.
*/
package expedition.ui;

import expedition.engine.HighScore;
import expedition.exception.QuitGameException;
import expedition.io.SaveSlot;
import java.util.List;

public interface GameView {
    // returned by readInt when the player asks to go back a step
    int BACK = Integer.MIN_VALUE;

    void print(String line);
    int readInt(String prompt, int min, int max) throws QuitGameException;
    String readChoice(String prompt, List<String> options) throws QuitGameException;
    boolean readYesNo(String prompt) throws QuitGameException;

    // Shows the main menu and waits for the player to pick something. The console
    // prints the text menu, the window shows menu screen. canResume is false when
    // there is no saved run, so Resume can't be picked
    MenuChoice showMainMenu(boolean canResume) throws QuitGameException;

    // Shows the best finished runs, best first. The window waits for Back, the console
    // just prints the table and carries on to the menu
    void showHighScores(List<HighScore> scores) throws QuitGameException;

    // Called once when the program is finished, so a window can close itself
    void close();

    // Shows the saved runs, the most recently played first, and returns the one the player
    // picks, or null to go back to the menu. A numbered list unless a view draws its own
    default SaveSlot chooseSave(List<SaveSlot> slots) throws QuitGameException {
        print("");
        print("--- SAVED RUNS ---");
        for (int i = 0; i < slots.size(); i++) {
            SaveSlot slot = slots.get(i);
            print(String.format("  %d) %-20s planet %d   last played %s",
                    i + 1, slot.name(), slot.planetsReached(), slot.lastPlayedText()));
        }
        int pick = readInt("Choose a save: ", 1, slots.size());
        return (pick == BACK) ? null : slots.get(pick - 1);
    }
}

