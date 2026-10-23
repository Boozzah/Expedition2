/*
 * SavedRunDao
 * Reads and writes the SAVED_RUN and SAVED_CREW tables. The only class that knows the
 * SQL for the saves, so the rest of the program just deals with SavedRun objects.
 * Each save is a slot, the autosave is the one slot with AUTOSAVE set
*/
package expedition.io.db;

import expedition.crew.Role;
import expedition.engine.SavedRun;
import expedition.engine.ScoreSheet;
import expedition.exception.DatabaseException;
import expedition.io.SaveSlot;
import expedition.ship.ShipPart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

class SavedRunDao {

    // route and item names are stored as one string, split on this
    private static final String SEPARATOR = ";";
    // between a score line's reason and its points
    private static final String EQUALS = "=";

    private static final String SELECT_RUN = "SELECT SEED, RNG_SEED, PLANET_INDEX, TOTAL_OIL, FUEL, HULL, ENGINE, EXHAUST, PRESSURE, WINDOWS, ROUTE, ITEMS, SCORE, OIL_DRILLED FROM SAVED_RUN WHERE SLOT = ?";
    private static final String SELECT_CREW = "SELECT NAME, ROLE, HEALTH, STAMINA, COMPOSURE, ALIVE FROM SAVED_CREW WHERE SLOT = ? ORDER BY POSITION";
    private static final String INSERT_RUN = "INSERT INTO SAVED_RUN (SLOT, NAME, LAST_PLAYED, AUTOSAVE, SEED, RNG_SEED, PLANET_INDEX, TOTAL_OIL, FUEL, "
            + "HULL, ENGINE, EXHAUST, PRESSURE, WINDOWS, ROUTE, ITEMS, SCORE, OIL_DRILLED) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // the Load Game list, the most recently played first
    private static final String SELECT_SLOTS = "SELECT SLOT, NAME, LAST_PLAYED, AUTOSAVE, PLANET_INDEX FROM SAVED_RUN ORDER BY LAST_PLAYED DESC";
    private static final String SELECT_AUTOSAVE = "SELECT SLOT FROM SAVED_RUN WHERE AUTOSAVE = TRUE";
    private static final String SELECT_NAMED = "SELECT SLOT FROM SAVED_RUN WHERE NAME = ? AND AUTOSAVE = FALSE";
    // COALESCE gives 0 when there are no saves yet, so the first slot is 1
    private static final String SELECT_LAST_SLOT = "SELECT COALESCE(MAX(SLOT), 0) FROM SAVED_RUN";
    private static final String INSERT_CREW = "INSERT INTO SAVED_CREW VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    // crew first, a SAVED_RUN row can't be deleted while SAVED_CREW rows still point at it
    private static final String DELETE_CREW = "DELETE FROM SAVED_CREW WHERE SLOT = ?";
    private static final String DELETE_RUN = "DELETE FROM SAVED_RUN WHERE SLOT = ?";

    // Replaces whatever was saved in the slot before. Done as one transaction, so if the game is
    // closed or something fails half way through, the old save is still there instead of half a new one
    void save(int slot, String name, boolean autosave, SavedRun run) throws DatabaseException {
        inTransaction("Could not save the run", conn -> {
            deleteRows(conn, slot);
            insertRun(conn, slot, name, autosave, run);
            insertCrew(conn, slot, run.crew());
        });
    }

    // returns null when the slot is empty
    SavedRun load(int slot) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_RUN)) {
            ps.setInt(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new SavedRun(
                        rs.getLong("SEED"),
                        rs.getLong("RNG_SEED"),
                        rs.getInt("PLANET_INDEX"),
                        rs.getInt("TOTAL_OIL"),
                        rs.getInt("FUEL"),
                        readParts(rs),
                        split(rs.getString("ROUTE")),
                        split(rs.getString("ITEMS")),
                        loadCrew(slot),
                        readScore(rs.getString("SCORE")),
                        rs.getInt("OIL_DRILLED"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the saved run", e);
        }
    }

    void delete(int slot) throws DatabaseException {
        inTransaction("Could not delete the saved run", conn -> deleteRows(conn, slot));
    }

    List<SaveSlot> list() throws DatabaseException {
        List<SaveSlot> slots = new ArrayList<>();
        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_SLOTS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                slots.add(new SaveSlot(
                        rs.getInt("SLOT"),
                        rs.getString("NAME"),
                        rs.getTimestamp("LAST_PLAYED").toLocalDateTime(),
                        rs.getBoolean("AUTOSAVE"),
                        // the index starts at 0, the first planet is planet 1
                        rs.getInt("PLANET_INDEX") + 1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the saved runs", e);
        }
        return slots;
    }

    // the autosave's slot, or null if there isn't one yet
    Integer autosaveSlot() throws DatabaseException {
        return findSlot(SELECT_AUTOSAVE, null);
    }

    // the slot of the named save with this name, or null if there isn't one
    Integer slotNamed(String name) throws DatabaseException {
        return findSlot(SELECT_NAMED, name);
    }

    // one past the highest slot in use
    int nextSlot() throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_LAST_SLOT);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) + 1;
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the saved runs", e);
        }
    }

    private Integer findSlot(String sql, String name) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(sql)) {
            if (name != null) {
                ps.setString(1, name);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the saved runs", e);
        }
    }

    // the SQL steps for one transaction, written as a lambda in save and delete
    private interface SqlWork {
        void run(Connection conn) throws SQLException;
    }

    // Runs the work as one transaction: all of it is saved, or if any step fails none of it is
    private void inTransaction(String failMessage, SqlWork work) throws DatabaseException {
        try {
            Connection conn = DBManager.get();
            conn.setAutoCommit(false);
            try {
                work.run(conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                // every other DAO expects each statement to save straight away
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException(failMessage, e);
        }
    }

    private List<SavedRun.SavedCrew> loadCrew(int slot) throws SQLException {
        List<SavedRun.SavedCrew> crew = new ArrayList<>();
        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_CREW)) {
            ps.setInt(1, slot);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    crew.add(new SavedRun.SavedCrew(
                            rs.getString("NAME"),
                            Role.valueOf(rs.getString("ROLE")),
                            rs.getInt("HEALTH"),
                            rs.getInt("STAMINA"),
                            rs.getInt("COMPOSURE"),
                            rs.getBoolean("ALIVE")));
                }
            }
        }
        return crew;
    }

    private void deleteRows(Connection conn, int slot) throws SQLException {
        try (PreparedStatement crew = conn.prepareStatement(DELETE_CREW);
             PreparedStatement run = conn.prepareStatement(DELETE_RUN)) {
            crew.setInt(1, slot);
            crew.executeUpdate();
            run.setInt(1, slot);
            run.executeUpdate();
        }
    }

    private void insertRun(Connection conn, int slot, String name, boolean autosave, SavedRun run) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_RUN)) {
            ps.setInt(1, slot);
            ps.setString(2, name);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBoolean(4, autosave);
            ps.setLong(5, run.seed());
            ps.setLong(6, run.rngSeed());
            ps.setInt(7, run.planetIndex());
            ps.setInt(8, run.totalOilCollected());
            ps.setInt(9, run.fuel());
            // the part columns are in the same order as ShipPart
            int column = 10;
            for (ShipPart part : ShipPart.values()) {
                ps.setInt(column++, run.parts().get(part));
            }
            ps.setString(column++, String.join(SEPARATOR, run.route()));
            ps.setString(column++, String.join(SEPARATOR, run.items()));
            ps.setString(column++, writeScore(run.score()));
            ps.setInt(column, run.oilDrilled());
            ps.executeUpdate();
        }
    }

    private void insertCrew(Connection conn, int slot, List<SavedRun.SavedCrew> crew) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CREW)) {
            for (int i = 0; i < crew.size(); i++) {
                SavedRun.SavedCrew c = crew.get(i);
                ps.setInt(1, slot);
                ps.setInt(2, i);
                ps.setString(3, c.name());
                ps.setString(4, c.role().name());
                ps.setInt(5, c.health());
                ps.setInt(6, c.stamina());
                ps.setInt(7, c.composure());
                ps.setBoolean(8, c.alive());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // each part has a column named after it, e.g. ENGINE
    private Map<ShipPart, Integer> readParts(ResultSet rs) throws SQLException {
        Map<ShipPart, Integer> parts = new EnumMap<>(ShipPart.class);
        for (ShipPart part : ShipPart.values()) {
            parts.put(part, rs.getInt(part.name()));
        }
        return parts;
    }

    // OIL=12;PLANETS=300
    private String writeScore(Map<ScoreSheet.Reason, Integer> score) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<ScoreSheet.Reason, Integer> e : score.entrySet()) {
            lines.add(e.getKey().name() + EQUALS + e.getValue());
        }
        return String.join(SEPARATOR, lines);
    }

    private Map<ScoreSheet.Reason, Integer> readScore(String joined) {
        Map<ScoreSheet.Reason, Integer> score = new EnumMap<>(ScoreSheet.Reason.class);
        for (String line : split(joined)) {
            String[] parts = line.split(EQUALS);
            score.put(ScoreSheet.Reason.valueOf(parts[0]), Integer.parseInt(parts[1]));
        }
        return score;
    }

    // an empty inventory is saved as "", which split would turn into one blank name
    private List<String> split(String joined) {
        if (joined.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(joined.split(SEPARATOR));
    }
}
