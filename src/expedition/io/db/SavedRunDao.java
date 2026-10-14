/*
 * SavedRunDao
 * Reads and writes the SAVED_RUN and SAVED_CREW tables. The only class that knows the
 * SQL for the save, so the rest of the program just deals with SavedRun objects
*/
package expedition.io.db;

import expedition.crew.Role;
import expedition.engine.SavedRun;
import expedition.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class SavedRunDao {

    // there is only one save, see Schema.SAVED_RUN
    private static final int SLOT = 1;

    // route and item names are stored as one string, split on this
    private static final String SEPARATOR = ";";

    private static final String SELECT_RUN = "SELECT SEED, RNG_SEED, PLANET_INDEX, TOTAL_OIL, FUEL, HULL, ROUTE, ITEMS FROM SAVED_RUN WHERE SLOT = ?";
    private static final String SELECT_CREW = "SELECT NAME, ROLE, HEALTH, STAMINA, COMPOSURE, ALIVE FROM SAVED_CREW WHERE SLOT = ? ORDER BY POSITION";
    private static final String INSERT_RUN = "INSERT INTO SAVED_RUN VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_CREW = "INSERT INTO SAVED_CREW VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    // crew first, a SAVED_RUN row can't be deleted while SAVED_CREW rows still point at it
    private static final String DELETE_CREW = "DELETE FROM SAVED_CREW WHERE SLOT = ?";
    private static final String DELETE_RUN = "DELETE FROM SAVED_RUN WHERE SLOT = ?";

    // Replaces whatever was saved before. Done as one transaction, so if the game is closed
    // or something fails half way through, the old save is still there instead of half a new one
    void save(SavedRun run) throws DatabaseException {
        inTransaction("Could not save the run", conn -> {
            deleteRows(conn);
            insertRun(conn, run);
            insertCrew(conn, run.crew());
        });
    }

    // returns null when there is no save, which is normal before the first run
    SavedRun load() throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_RUN)) {
            ps.setInt(1, SLOT);
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
                        rs.getInt("HULL"),
                        split(rs.getString("ROUTE")),
                        split(rs.getString("ITEMS")),
                        loadCrew());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the saved run", e);
        }
    }

    // a finished run deletes its save so Resume can't bring back a run that is over
    void delete() throws DatabaseException {
        inTransaction("Could not delete the saved run", this::deleteRows);
    }

    boolean exists() throws DatabaseException {
        return load() != null;
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

    private List<SavedRun.SavedCrew> loadCrew() throws SQLException {
        List<SavedRun.SavedCrew> crew = new ArrayList<>();
        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_CREW)) {
            ps.setInt(1, SLOT);
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

    private void deleteRows(Connection conn) throws SQLException {
        try (PreparedStatement crew = conn.prepareStatement(DELETE_CREW);
             PreparedStatement run = conn.prepareStatement(DELETE_RUN)) {
            crew.setInt(1, SLOT);
            crew.executeUpdate();
            run.setInt(1, SLOT);
            run.executeUpdate();
        }
    }

    private void insertRun(Connection conn, SavedRun run) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_RUN)) {
            ps.setInt(1, SLOT);
            ps.setLong(2, run.seed());
            ps.setLong(3, run.rngSeed());
            ps.setInt(4, run.planetIndex());
            ps.setInt(5, run.totalOilCollected());
            ps.setInt(6, run.fuel());
            ps.setInt(7, run.hullPercent());
            ps.setString(8, String.join(SEPARATOR, run.route()));
            ps.setString(9, String.join(SEPARATOR, run.items()));
            ps.executeUpdate();
        }
    }

    private void insertCrew(Connection conn, List<SavedRun.SavedCrew> crew) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CREW)) {
            for (int i = 0; i < crew.size(); i++) {
                SavedRun.SavedCrew c = crew.get(i);
                ps.setInt(1, SLOT);
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

    // an empty inventory is saved as "", which split would turn into one blank name
    private List<String> split(String joined) {
        if (joined.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(joined.split(SEPARATOR));
    }
}
