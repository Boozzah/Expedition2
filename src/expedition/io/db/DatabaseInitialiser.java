/*
 * DatabaseInitialiser
 * Builds the game's tables the first time the program runs. Called once from main
 * before the engine starts, and safe to call on every launch after that.
 * Tables that already exist are left alone. This is what lets the player's game run with
 * no database setup, nothing to install or configure.
*/

package expedition.io.db;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import expedition.exception.DatabaseException;

public class DatabaseInitialiser {
    
    // The order matters. HAZARD_OUTCOMES has a FK from the hazard table
    public void initialise() throws DatabaseException {
        try {
            createIfAbsent("CREW", Schema.CREW);
            createIfAbsent("PLANETS", Schema.PLANETS);
            createIfAbsent("HAZARDS", Schema.HAZARDS);
            createIfAbsent("HAZARD_OUTCOMES", Schema.HAZARD_OUTCOMES);
        } catch (SQLException e) {
            throw new DatabaseException("Could not create the game table", e);
        }
    }
    
    // Creates one table, unless it is already there.
    private void createIfAbsent(String tableName, String ddl) throws SQLException {
        
        // Derby stores unquoted table names in capitals, so the name passed in has to be capitalised
        DatabaseMetaData meta = DBManager.get().getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            if (rs.next()) return;
        }
        
        // Statement is declared in the brackets so it is closed automatically,
        // even if the statement fails
        try (Statement st = DBManager.get().createStatement()) {
            st.execute(ddl);
        }
    }
}
