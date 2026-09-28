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
import java.util.ArrayList;
import java.util.List;

// AI assisted - Claude suggested the DAO structure for reading and writing the CREW table
class CrewDao {
    
    // ORDER BY so the crew comes back in the same order
    private static final String SELECT_ALL = "SELECT NAME, ROLE, HEALTH, STAMINA, COMPOSURE FROM CREW ORDER BY NAME";
    
    
    // each crew member is one insert statement. The names come from crew.txt
    // so they can be put straight into SQL text
    void insertAll(List<CrewMember> crew) throws DatabaseException {
        try (Statement st = DBManager.get().createStatement()) {
            for (CrewMember c : crew) {
                st.executeUpdate("INSERT INTO CREW VALUES ('"
                        + c.getName() + "', '"
                        + c.getRole().name() + "', "
                        + c.getHealth() + ", "
                        + c.getStamina() + ", "
                        + c.getComposure() + ")");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the crew", e);
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
