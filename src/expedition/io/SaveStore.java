/*
 * SaveStore
 * Where the saved runs live. GameEngine only talks to this, the same way it only talks to
 * GameDataSource for the game content, so it never knows the saves are in a database.
 *
 * There is one autosave, replaced each time the ship reaches a planet, and any number of
 * named saves the player makes from the in-game menu.
 */
package expedition.io;

import expedition.engine.SavedRun;
import expedition.exception.SaveException;
import java.util.List;

public interface SaveStore {
    // the longest name a save can have
    int MAX_NAME_LENGTH = 40;

    // every save, the most recently played first
    List<SaveSlot> list() throws SaveException;

    // replaces the autosave
    void autosave(SavedRun run) throws SaveException;

    // a name that is already used replaces that save, otherwise it is a new one
    void saveAs(String name, SavedRun run) throws SaveException;

    // null when the save has gone
    SavedRun load(int slotId) throws SaveException;

    // a finished run removes its autosave, the named saves are kept
    void deleteAutosave() throws SaveException;

    default boolean hasSave() throws SaveException {
        return !list().isEmpty();
    }
}
