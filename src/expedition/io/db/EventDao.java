/*
 * EventDao
 * Reads and writes the EVENTS table. The only class that knows the SQL for jump events,
 * so the rest of the program just deals with JumpEvent objects
*/
package expedition.io.db;

import expedition.event.JumpEvent;
import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.hazard.effect.EffectParser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

class EventDao {

    // ORDER BY ID so they come back in the same order they were in events.txt
    private static final String SELECT_ALL = "SELECT ID, TEXT, EFFECTS, POINTS, WEIGHT FROM EVENTS ORDER BY ID";

    // ID is left out, the database numbers the rows itself
    private static final String INSERT = "INSERT INTO EVENTS (TEXT, EFFECTS, POINTS, WEIGHT) VALUES (?, ?, ?, ?)";

    void insertAll(List<JumpEvent> events) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(INSERT)) {
            for (JumpEvent e : events) {
                ps.setString(1, e.text());
                ps.setString(2, e.effectCodes());
                ps.setInt(3, e.points());
                ps.setInt(4, e.weight());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the jump events", e);
        }
    }

    List<JumpEvent> findAll() throws DatabaseException {
        List<JumpEvent> events = new ArrayList<>();

        try (Statement st = DBManager.get().createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {

            while (rs.next()) {
                // the codes are parsed back into Effects the same way HazardDao does it,
                // with the table name standing in for the file name in error messages
                String codes = rs.getString("EFFECTS");
                events.add(new JumpEvent(
                        rs.getString("TEXT"),
                        EffectParser.parse(codes, "EVENTS", rs.getInt("ID")),
                        codes,
                        rs.getInt("POINTS"),
                        rs.getInt("WEIGHT")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the jump events", e);
        } catch (DataLoadException e) {
            throw new DatabaseException("A jump event in the database has bad effect codes", e);
        }
        return events;
    }
}
