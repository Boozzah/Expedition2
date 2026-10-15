/*
 * DerbyScoreStore
 * The database version of ScoreStore. Hands each request to ScoreDao and turns any
 * DatabaseException into a SaveException, the same way DerbySaveStore does
 */
package expedition.io.db;

import expedition.engine.HighScore;
import expedition.exception.DatabaseException;
import expedition.exception.SaveException;
import expedition.io.ScoreStore;
import java.util.List;

public class DerbyScoreStore implements ScoreStore {

    private final ScoreDao dao = new ScoreDao();

    @Override
    public void add(HighScore score) throws SaveException {
        try {
            dao.insert(score);
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public List<HighScore> top(int limit) throws SaveException {
        try {
            return dao.findTop(limit);
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }
}
