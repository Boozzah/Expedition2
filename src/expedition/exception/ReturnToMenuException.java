/*
 * ReturnToMenuException
 * Thrown when the player leaves a run from the in-game menu. Works the same way
 * as QuitGameException, it unwinds out of whatever prompt the game is on, but
 * GameEngine catches it and goes back to the main menu instead of closing.
 */
package expedition.exception;

// AI assisted - Claude suggested using an exception like QuitGameException to get out of a run and back to the main menu
public class ReturnToMenuException extends RuntimeException {

    public ReturnToMenuException() {
        super(null, null, false, false);
    }
}
