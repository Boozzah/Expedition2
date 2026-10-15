/*
 * ScoreDao
 * Reads and writes the SCORES table. The only class that knows the SQL for scores,
 * so the rest of the program just deals with HighScore objects
*/
package expedition.io.db;

import expedition.engine.EndingReason;
import expedition.engine.HighScore;
import expedition.exception.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

class ScoreDao {

    // best first. on a tie the older run stays above, it got there first
    private static final String SELECT_TOP = "SELECT PLAYED_AT, ENDING, PLANETS, OIL, SURVIVORS, LOST, SCORE FROM SCORES ORDER BY SCORE DESC, PLAYED_AT ASC";

    // ID is left out, the database numbers the rows itself
    private static final String INSERT = "INSERT INTO SCORES (PLAYED_AT, ENDING, PLANETS, OIL, SURVIVORS, LOST, SCORE) VALUES (?, ?, ?, ?, ?, ?, ?)";

    void insert(HighScore s) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(INSERT)) {
            // Timestamp is the java.sql version of a date and time
            ps.setTimestamp(1, Timestamp.valueOf(s.playedAt()));
            ps.setString(2, s.reason().name());
            ps.setInt(3, s.planetsReached());
            ps.setInt(4, s.oilCollected());
            ps.setInt(5, s.survivors());
            ps.setInt(6, s.lost());
            ps.setInt(7, s.score());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the score", e);
        }
    }

    // the best few runs, for the high-score screen
    List<HighScore> findTop(int limit) throws DatabaseException {
        List<HighScore> scores = new ArrayList<>();

        try (PreparedStatement ps = DBManager.get().prepareStatement(SELECT_TOP)) {
            // stops reading after this many rows, so the table can keep growing
            ps.setMaxRows(limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    scores.add(new HighScore(
                            rs.getTimestamp("PLAYED_AT").toLocalDateTime(),
                            EndingReason.valueOf(rs.getString("ENDING")),
                            rs.getInt("PLANETS"),
                            rs.getInt("OIL"),
                            rs.getInt("SURVIVORS"),
                            rs.getInt("LOST"),
                            rs.getInt("SCORE")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the high scores", e);
        }
        return scores;
    }
}
