/*
 * SaveStore
 * Where the autosave lives. GameEngine only talks to this, the same way it only talks to
 * GameDataSource for the game content, so it never knows the save is in a database.
 */
package expedition.io;

import expedition.engine.SavedRun;
import expedition.exception.SaveException;

public interface SaveStore {
    boolean hasSave() throws SaveException;

    // replaces any save already there
    void save(SavedRun run) throws SaveException;

    // null when there is no save
    SavedRun load() throws SaveException;

    void delete() throws SaveException;
}
