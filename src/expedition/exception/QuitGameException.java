/*
 * QuitGameException
 * Thrown when the player quits from any input prompt. Used
 * deliberately as control flow so "quit at any step" doesn't need a
 * BACK/QUIT sentinel threaded through every method.
 */
package expedition.exception;

public class QuitGameException extends RuntimeException {
    
    // Constructor for a quit request with no message. Handlers gives the default
    public QuitGameException() {
        this(null);
    }
    
    // Constructor for a quit request, carrying text for the handler to display
    public QuitGameException(String message){
        super(message, null, false, false);
    }
}
