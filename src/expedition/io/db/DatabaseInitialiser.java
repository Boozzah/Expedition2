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
import expedition.io.GameDataSource;
import expedition.exception.DataLoadException;

public class DatabaseInitialiser {
    
    // where the starting content comes from when a table is empty
    private final GameDataSource seedSource;
    
    public DatabaseInitialiser(GameDataSource seedSource) {
        this.seedSource = seedSource;
    }
    
    // The order matters. HAZARD_OUTCOMES has a FK from the hazard table
    public void initialise() throws DatabaseException, DataLoadException {
        try {
            createIfAbsent("CREW", Schema.CREW);
            createIfAbsent("PLANETS", Schema.PLANETS);
            createIfAbsent("HAZARDS", Schema.HAZARDS);
            createIfAbsent("HAZARD_OUTCOMES", Schema.HAZARD_OUTCOMES);
            createIfAbsent("ITEMS", Schema.ITEMS);
            // SAVED_CREW has a FK to SAVED_RUN, so it has to come second. Both start empty,
            // the first autosave fills them
            createIfAbsent("SAVED_RUN", Schema.SAVED_RUN);
            createIfAbsent("SAVED_CREW", Schema.SAVED_CREW);
            // starts empty, a row is added each time a run ends
            createIfAbsent("SCORES", Schema.SCORES);
            
            
            // first run only - once the crew are in, the table is never emptied
            if (isEmpty("CREW")) {
                new CrewDao().insertAll(seedSource.loadCrew());
            }
            
            if (isEmpty("PLANETS")) {
                new PlanetDao().insertAll(seedSource.loadPlanets());
            }
            if (isEmpty("PLANETS")) {
                new PlanetDao().insertAll(seedSource.loadPlanets());
            }

            if (isEmpty("HAZARDS")) {
                new HazardDao().insertAll(seedSource.loadHazards());
            }
            
            // added after the other tables, so a database made before items existed
            // gets the table and its rows on the next launch without being deleted
            if (isEmpty("ITEMS")) {
                new ItemDao().insertAll(seedSource.loadItems());
            }
            
        } catch (SQLException e) {
            throw new DatabaseException("Could not create the game table", e);
        }
    }
    
    // COUNT(*) always returns one row holding the number of rows in the table
    private boolean isEmpty(String tableName) throws SQLException {
        try (Statement st = DBManager.get().createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getInt(1) == 0;
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
