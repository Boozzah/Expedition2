/*
 * ConsoleView
 * 
 * The console implementation of GameView. It doesn't do any work itself, it just builds
 * a ConsoleUI and an InputValidator and forwards each GameView call to whichever one handles it
 * e.g print goes to ConsoleUI, the read methods go to the Input Validator
 */
package expedition.ui;

import expedition.engine.HighScore;
import expedition.exception.QuitGameException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

// AI assisted - Claude Code suggested ConsoleView as an adapter over ConsoleUI and InputValidator, we typed it in ourselves
public class ConsoleView implements GameView {

    private final ConsoleUI ui;
    private final InputValidator input;

    public ConsoleView(InputStream in, PrintStream out) {
        this.ui = new ConsoleUI(in, out);
        this.input = new InputValidator(ui);
    }
    
    @Override
    public void print(String line) {
        ui.print(line);
    }

    @Override
    public int readInt(String prompt, int min, int max) throws QuitGameException {
        return input.readInt(prompt, min, max);
    }

    @Override
    public String readChoice(String prompt, List<String> options) throws QuitGameException {
        return input.readChoice(prompt, options);
    }

    @Override
    public boolean readYesNo(String prompt) throws QuitGameException {
        return input.readYesNo(prompt);
    }

    // The same text menu the console has always had, the typed answer is just
    // turned into a MenuChoice before it goes back to the engine.
    // Resume is only listed when there is a saved run to resume
    @Override
    public MenuChoice showMainMenu(boolean canResume) throws QuitGameException {
        List<String> options = new ArrayList<>();
        options.add("New Game (n)");
        if (canResume) {
            options.add("Resume (r)");
        }
        options.add("How to Play (h)");
        // not "High Scores", it would start with h like How to Play and typing h would match both
        options.add("Scores (s)");
        options.add("Quit (q)");
        
        String choice = input.readChoice("--- MAIN MENU ---", options);
        switch (choice) {
            case "New Game (n)": return MenuChoice.NEW_GAME;
            case "Resume (r)": return MenuChoice.RESUME;
            case "How to Play (h)": return MenuChoice.HOW_TO_PLAY;
            case "Scores (s)": return MenuChoice.HIGH_SCORES;
            default: return MenuChoice.QUIT;
        }
    }

    // a plain table, %-16s pads a column out to 16 characters so the next one lines up
    @Override
    public void showHighScores(List<HighScore> scores) {
        ui.print("");
        ui.print("--- HIGH SCORES ---");
        if (scores.isEmpty()) {
            ui.print("No finished runs yet.");
            return;
        }
        ui.print(String.format(" %-3s %-16s %-19s %-8s %s", "#", "Date", "Ending", "Planets", "Score"));
        for (int i = 0; i < scores.size(); i++) {
            HighScore s = scores.get(i);
            ui.print(String.format(" %-3d %-16s %-19s %-8d %d", i + 1, s.when(), s.ending(), s.planetsReached(), s.score()));
        }
    }

    // nothing to close, the console belongs to the terminal
    @Override
    public void close() {
    }
}
