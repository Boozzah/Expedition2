/*
 * GameView
 *
 * Everything the game needs from a user interface, show a line of text,
 * and ask the player for an input. GameEngine, ExpeditionSelector, DrillSession and RunLogWriter
 * only ever talk to this interface.
*/
package expedition.ui;

import expedition.exception.QuitGameException;
import java.util.List;

public interface GameView {
    // returned by readInt when the player asks to go back a step
    int BACK = Integer.MIN_VALUE;

    void print(String line);
    int readInt(String prompt, int min, int max) throws QuitGameException;
    String readChoice(String prompt, List<String> options) throws QuitGameException;
    boolean readYesNo(String prompt) throws QuitGameException;
}

