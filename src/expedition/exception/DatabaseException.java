/*
 * DatabaseException
 * Thrown when a database operation fails. Wraps the SQLException underneath so the rest
 * of the program never has to import anything from java.sql only the io.db package
*/
package expedition.exception;

public class DatabaseException extends Exception {
    
    // Takes the cause as well as the messgae so the original database error is kept for debugging
    // while the player just sees the plain english message
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
