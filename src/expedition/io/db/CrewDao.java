/*
 * CrewDao
 * Reads and writes the CREW table. The only class that knows the SQL for crew,
 * so the rest of the program just deals with CrewMember objects
*/
package expedition.io.db;

import expedition.crew.CrewFactory;
import expedition.crew.CrewMember;
import expedition.crew.Role;
import expedition.exception.DatabaseException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
// prepared statement lets you write the SWL once with ? gaps and fill in the values seperately.
// CLAUDE RECCOMENDED WHEN I ASKED HOW TO HANDLE APOSTROPHES
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

// AI assisted - Claude suggested the DAO structure for reading and writing the CREW table
class CrewDao {
    
    // ORDER BY so the crew comes back in the same order
    private static final String SELECT_ALL = "SELECT NAME, ROLE, HEALTH, STAMINA, COMPOSURE FROM CREW ORDER BY NAME";
    
    // each ? is filled in later with setString/setInt, in column order
    private static final String INSERT = "INSERT INTO CREW VALUES (?, ?, ?, ?, ?)";
    
    // the ? placeholders let the deriver handle the quoting, so text with an apostrophe
    // in it (like planet flavour text) can't break the SQL query or statement
    void insertAll(List<CrewMember> crew) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(INSERT)) {
            for (CrewMember c : crew) {
                ps.setString(1, c.getName());
                ps.setString(2, c.getRole().name());
                ps.setInt(3, c.getHealth());
                ps.setInt(4, c.getStamina());
                ps.setInt(5, c.getComposure());
                ps.addBatch();
            }
            // sends every row to the database in one go
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Vould not save the crew", e);
        }
    }
    
    // reads every row and turns each one back into the right CrewMember subclass
    List<CrewMember> findAll() throws DatabaseException {
        List<CrewMember> crew = new ArrayList<>();
        
        // Both declared in the brackets so they close automatically
        try (Statement st = DBManager.get().createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {
            
            // rs.next() moves to the next row, when there is no next row it changes to false
            while (rs.next()) {
                // ROLE is stored as text, valueOf turns "MINER" back into Role.MINER
                Role role = Role.valueOf(rs.getString("ROLE"));
                crew.add(CrewFactory.create(role, 
                        rs.getString("NAME"),
                        rs.getInt("HEALTH"),
                        rs.getInt("STAMINA"),
                        rs.getInt("COMPOSURE")));
            }
        } catch(SQLException e) {
            throw new DatabaseException("Could not read the crew", e);
        }
        return crew;
    }
    
}
