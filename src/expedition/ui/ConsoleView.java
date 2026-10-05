/*
 * The console implementation of GameView. It doesn't do any work iteslef, it just builds
 * a ConsoleUI and an InputValidator and forwards each GameView call to whichever one handles it
 * e.g print goes to ConsoleUI, the read methods go to the Input Validator
*/
package expedition.ui;

import expedition.exception.QuitGameException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

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
}

