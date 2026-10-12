/*
 * DatabaseInitialiserTest
 * Checks that the game's tables get built on the first run, and that running the
 * initialiser again on an already existing database does nothing rather than throwing an error
 * that is what makes the game safe to launch over and over without any database setup
*/

package expedition.io.db;

import java.sql.SQLException;
import expedition.exception.DatabaseException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import org.junit.Test;
import static org.junit.Assert.*;
import expedition.io.TextFileDataSource;
import expedition.exception.DataLoadException;

public class DatabaseInitialiserTest {
    
    @Test
    public void initialiseCreatesAllTheTables() throws DatabaseException, DataLoadException, SQLException {
        
        new DatabaseInitialiser(new TextFileDataSource()).initialise();
        
        assertTrue("CREW table should exist", tableExists("CREW"));
        assertTrue("PLANETS table should exist", tableExists("PLANETS"));
        assertTrue("HAZARDS table should exist", tableExists("HAZARDS"));
        assertTrue("HAZARD_OUTCOMES table should exist", tableExists("HAZARD_OUTCOMES"));
        assertTrue("ITEMS table should exist", tableExists("ITEMS"));
        
    }
    
    //  No assertions needed, the the second call throws, the test will fail automatically
    //  this is the case that matters, if every launch after the first one runs
    // against a database where the tables are already there.
    @Test
    public void initialiseIsSafeToRunTwice() throws DatabaseException, DataLoadException {
        DatabaseInitialiser init = new DatabaseInitialiser(new TextFileDataSource());
        init.initialise();
        init.initialise();   // must not throw "table already exists"
    }

    // Asks the database directly whether a table is there, rather than
    // trusting the initialiser to report on itself.
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = DBManager.get().getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
