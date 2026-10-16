/*
 * SaveException
 * Thrown when a saved run can't be written, read or deleted, or when the save is
 * there but can't be turned back into a run (e.g. a planet in it no longer exists).
 * The game carries on without the save rather than stopping.
*/
package expedition.exception;

public class SaveException extends Exception {

    public SaveException(String message) {
        super(message);
    }

    // keeps the original error as the cause, the same as DatabaseException
    public SaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
