package expedition.ui;
import expedition.exception.QuitGameException;
import java.util.List;

/*
 * InputValidator
 *
 * Wraps ConsoleUI to add validations on top of the raw input:
 * menu choices, yes/no answers. It does no printing or reading input,
 * everything goes through the ConsoleUI instance passed in the constructor
 * 
 * Quit/Back requirement, used by every method here:
 * - typing "Q" at ANY prompt throws QuitGameException, which unwinds
 *   straight up through the call stack to Main (caught there, program
 *   exits cleanly) without needing to be handled at every level in between.
 * - typing "B" (readInt only, for now) returns GameView.BACK instead of
 *   a real value, so the caller knows to treat it as "go back a step or menu level"
 *   rather than a valid answer
 * 
 * None of these methods ever let a raw exception (like NumberFormatException)
 * escape to the player console - bad input is caught, a friendly message is shown,
 * and the prompt loops over
 */

public class InputValidator {
    
    private final ConsoleUI ui;
    
    public InputValidator(ConsoleUI ui) {
        this.ui = ui;
    }
    
    // Reads an integer between min and max (inclusive). Re prompts on anything invalid:
    // not a number, or out of range
    public int readInt(String prompt, int min, int max) throws QuitGameException {
        while (true) {
            String line = ui.input(prompt);
            if (line.equalsIgnoreCase("Q")) throw new QuitGameException();  // quit game
            if (line.equalsIgnoreCase("B")) return GameView.BACK;
            
            int parsed;
            try {
                parsed = Integer.parseInt(line); 
            } catch (NumberFormatException e) {
                ui.print("'" + line + "' is not a number. Try again.");
                continue;
            }
            if (parsed < min || parsed > max) {
                ui.print("Enter a number between " + min + " and " + max + ".");
                continue;
            }
            return parsed;
        }
    }
    
    // Prints the prompt plus a list of valid options, then reads a line and matches it (non case-sensitive)
    // against those options. Re-prompts intil the player enters a valid option.
    public String readChoice (String prompt, List<String> validOptions) throws QuitGameException {
        while (true) {
            ui.print(prompt);
            for (String option : validOptions) {
                ui.print(option);     
            }
            String line = ui.input("> ");
            if (line.equalsIgnoreCase("Q")) throw new QuitGameException();
            
            String matched = null;  
            for (String option : validOptions) {
                if (option.equalsIgnoreCase(line)) {
                    matched = option;
                    break;
                }
            }
            if (matched != null) {
                return matched;
            }
            
            if (line.length() ==1) {
                String hit = null;
                int matches = 0;
                for (String option : validOptions) {
                    if (option.toLowerCase().startsWith(line.toLowerCase())) {
                        hit = option;
                        matches++;
                    }
                }
                if (matches == 1) {
                    return hit;
                }
            }
            ui.print("Please choose one of: " + validOptions);
        }
    }
    
    // Reads a yes/no answer. Accepts Y or N (non case-sensitive), re prompts on
    // anything else.
    public boolean readYesNo(String prompt) throws QuitGameException {
        while (true) {
            String line = ui.input(prompt + " (Y/N): ");
            if (line.equalsIgnoreCase("Q")) throw new QuitGameException();
            if (line.equalsIgnoreCase("Y")) return true;
            if (line.equalsIgnoreCase("N")) return false;
            ui.print("Please enter Y or N.");
        }
    }
}
