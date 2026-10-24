/*
 * HazardDao
 * Reads and writes the Hazards and Hazard outcomes tables. One hazard is one row
 * in HAZARDS plux siz rows in Hazard_Outcomes, one per severity, joined by the hazard's ID
 */
package expedition.io.db;

import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.hazard.Hazard;
import expedition.hazard.HazardOutcome;
import expedition.hazard.HazardSet;
import expedition.hazard.effect.EffectParser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// AI assisted - Claude Code explained how the DAO classes should work and gave a pseudocode outline, we typed it in ourselves.
// It also traced the compile errors from our typing slips when we first copied this in
public class HazardDao {

    private static final String INSERT_HAZARD = "INSERT INTO HAZARDS (NAME, HAZARD_SET) VALUES (?, ?)";
    private static final String INSERT_OUTCOME = "INSERT INTO HAZARD_OUTCOMES (HAZARD_ID, SEVERITY, TEXT, EFFECTS) VALUES (?, ?, ?, ?)";

    // JOIN puts each outcome row next to the hazard it belongs to, so both tables are read in one go
    // ORDER BY Hazard ID keeps the hazards in the same order as hazards.txt
    private static final String SELECT_ALL
            = "SELECT H.ID, H.NAME, H.HAZARD_SET, O.SEVERITY, O.TEXT, O.EFFECTS "
            + "FROM HAZARDS H JOIN HAZARD_OUTCOMES O ON O.HAZARD_ID = H.ID "
            + "ORDER BY H.ID, O.SEVERITY";

    void insertAll(List<Hazard> hazards) throws DatabaseException {
        // RETURN_GENERATED_KEYS asks Derby to hand back the ID it creates for each hazard
        try (PreparedStatement hazardPs = DBManager.get().prepareStatement(INSERT_HAZARD,
                Statement.RETURN_GENERATED_KEYS); PreparedStatement outcomePs = DBManager.get().prepareStatement(INSERT_OUTCOME)) {

            for (Hazard h : hazards) {
                hazardPs.setString(1, h.name());
                hazardPs.setString(2, h.set().name());
                hazardPs.executeUpdate();

                // the outcomes can't be saved until the hazard's ID is known,
                // since every outcome row has to point at it
                int hazardId;
                try (ResultSet keys = hazardPs.getGeneratedKeys()) {
                    keys.next();
                    hazardId = keys.getInt(1);
                }

                for (HazardOutcome o : h.outcomes().values()) {
                    outcomePs.setInt(1, hazardId);
                    outcomePs.setInt(2, o.severity());
                    outcomePs.setString(3, o.text());
                    outcomePs.setString(4, o.effectCodes());
                    outcomePs.addBatch();
                }
            }
            outcomePs.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the hazards", e);
        }
    }

    // holds one hazard's detaisl while its siz rows are read
    private record HazardRows(String name, HazardSet set, Map<Integer, HazardOutcome> outcomes) {

    }
    // works like TextFileDataSource.loadHazards, six rows make one hazard, so the rows are collected first
    // and the Hazard objects built once they have all been read

    List<Hazard> findAll() throws DatabaseException {
        // hazard ID -> its rows so far. LinkedHashMap keeps them in ID order
        Map<Integer, HazardRows> byId = new LinkedHashMap<>();

        try (Statement st = DBManager.get().createStatement(); ResultSet rs = st.executeQuery(SELECT_ALL)) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                HazardSet set = HazardSet.valueOf(rs.getString("HAZARD_SET"));
                int severity = rs.getInt("SEVERITY");
                String codes = rs.getString("EFFECTS");
                // the codes are stored as text, so they are parsed back into Effects the same
                // way the text file does it. The table name stands in for the file name in error messages 
                HazardOutcome outcome = new HazardOutcome(severity, rs.getString("TEXT"),
                        EffectParser.parse(codes, "HAZARD_OUTCOMES", id), codes);

                byId.computeIfAbsent(id, k -> new HazardRows(name, set, new LinkedHashMap<>()))
                        .outcomes()
                        .put(severity, outcome);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the hazards", e);
        } catch (DataLoadException e) {
            throw new DatabaseException("A hazard in the database has bad effect codes", e);
        }

        List<Hazard> hazards = new ArrayList<>();
        for (HazardRows rows : byId.values()) {
            hazards.add(new Hazard(rows.name(), rows.set(), rows.outcomes()));
        }
        return hazards;
    }
}
