/* DBManager
 * Where the derby connection lives. Embedded db, so the database is just a folder inside the project
 * Adaped from the PDC lab DBManager, converted to embedded mode
*/

package expedition.io.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// AI assisted - Claude reccomended what changes to make to the source code provided in 
// lab 8 PDC for this implementation
public final class DBManager {
    
    // data/expedition-db is a folder inside the project
    // create=true builds it on the first run
    private static final String URL = "jdbc:derby:data/expedition-db;create=true";
    
    // shutting derby down throws on success, this state means it worked
    private static final String SHUTDOWN_URL = "jdbc:derby:;shutdown=true";
    private static final String SHUTDOWN_OK = "XJ015";
    
    private static Connection conn;
    
    // never instantiated, it is static
    private DBManager () {
    }
    
    // synchronized so the game and the window cna never open two connections at once
    public static synchronized Connection get() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(URL);
        }
        return conn;
    }
    
    // Called once as the game exists. Skipping it leaves the database intact but
    // makes Derby log a recovery on the next boot, just shutdowns cleanly
    public static synchronized void shutdown() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
            DriverManager.getConnection(SHUTDOWN_URL);
        } catch (SQLException e) {
            if (!SHUTDOWN_OK.equals(e.getSQLState())) {
                System.err.print("Derby did not shut down cleanly: " + e.getMessage());
            }
            // finally fires regarldless of the outcome of the try block
        } finally {
            conn = null;
        }
    }
}
