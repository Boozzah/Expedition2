/*
 * ScoreStore
 * Where finished runs are kept for the high-score screen. GameEngine only talks to
 * this, the same as SaveStore, so it never knows the scores are in a database.
 */
package expedition.io;

import expedition.engine.HighScore;
import expedition.exception.SaveException;
import java.util.List;

public interface ScoreStore {
    void add(HighScore score) throws SaveException;

    // best first, at most this many
    List<HighScore> top(int limit) throws SaveException;
}
