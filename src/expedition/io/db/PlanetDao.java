/*
 * PlanetDao
 * Reads and write the PLANETS table. The only class that knows the SQL for planets,
 * so the rest of the program just deals with Planet objects
*/

package expedition.io.db;

import expedition.exception.DatabaseException;
import expedition.world.Planet;
import expedition.world.PlanetArchetype;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// AI assisted - Claude suggested the join/split for storing site names in one column

public class PlanetDao {
    
    // the site names are stored as one string joined by ';', same as planets.txt
    private static final String SITE_SEPERATOR = ";";
    
    
    // ORDER BY ID so the planets return in the order they were saved - same order as planets.txt
    
    private static final String SELECT_ALL = "SELECT NAME, ARCHETYPE, FLAVOUR, SITES FROM PLANETS ORDER BY ID";
    
    // ID is left out because Derby numbers it automaticaly, so the columns have to be names
    private static final String INSERT = "INSERT INTO PLANETS (NAME, ARCHETYPE, FLAVOUR, SITES) VALUES (?, ?, ?, ?)";
    
    // the flavour text has apostrophes in it so the ? placeholders is what keeps it from breaking
    void insertAll(List<Planet> planets) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(INSERT)) {
            for (Planet p : planets) {
                ps.setString(1, p.name());
                ps.setString(2, p.archetype().name());
                ps.setString(3, p.introFlavour());
                // ["North Shelf", "The Silo", ...] becomes "North Shelf;The Silo;..."
                ps.setString(4, String.join(SITE_SEPERATOR, p.siteNamePool()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the planets", e);
        }
    }
    
    // reads every row and turns each one back into a Planet
    List<Planet> findAll() throws DatabaseException {
        List<Planet> planets = new ArrayList<>();
        
        try (Statement st = DBManager.get().createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {
            
            while (rs.next()) {
                PlanetArchetype archetype = PlanetArchetype.valueOf(rs.getString("ARCHETYPE"));
                // "Noth Shelf;The Silo;..." is split back into a list of names
                // new ArrayList so the list can be changed later, Arrays.asList is fixed size
                List<String> sites = new ArrayList<>(Arrays.asList(rs.getString("SITES").split(SITE_SEPERATOR)));
                
                planets.add(new Planet(rs.getString("NAME"), archetype, rs.getString("FLAVOUR"), sites));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the planets", e);
        }
        return planets;
    }
}
