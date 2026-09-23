package expedition.ui;
import java.util.Scanner;
import java.io.PrintStream;
import java.io.InputStream;
import expedition.exception.QuitGameException;

/*
 * ConsoleUI
 * 
 * This is the ONLY class in the whole program allowed to touch System.in
 * or System.out directly. Every other class that needs to print something
 * or read input goes through an instance of this class instead.
 *
 * Why: if two different classes each open their own Scanner on System.in,
 * they can steal/duplicate each other's input depending on buffering timing.
 * Centralising it here means there is only ever ONE Scanner for the whole
 * program, so that bug can't happen.
 *
 * The constructor takes InputStream/PrintStream (not System.in/System.out
 * directly) so this class isn't hardwired to the console - it could be
 * pointed at a fake stream for testing later if needed.
 */

public class ConsoleUI {
    private final Scanner in;
    private final PrintStream out;
    
    public ConsoleUI(InputStream in, PrintStream out) {
        this.in = new Scanner(in);
        this.out = out;
    }
    
    // plain output, no input involved. Adds a newline
    // use this for game tesxt, status messages, error messages, etc.
    public void print(String s) {
        out.println(s);
    }
    
    // Prints a prompt on the SAME line (no newline), then blocks and reads
    // one line of input, trimmed of leading/trailing white space
    // this is the only place in the program that ever reads input from the player.
    public String input(String prompt) {
        out.print(prompt);
        
        // end of input - the player pressed Ctrl+D, or the game is being
        // fed from a file that has run out. There is no way to ask for anything else
        // so treat it like typing Q
        if (!in.hasNextLine()) {
            out.println();
            throw new QuitGameException();
        }
        return in.nextLine().trim();
    }
}
