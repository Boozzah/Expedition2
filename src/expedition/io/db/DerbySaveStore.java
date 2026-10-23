/*
 * DerbySaveStore
 * The database version of SaveStore. Hands each request to SavedRunDao and turns any
 * DatabaseException into a SaveException, the same way DerbyDataSource does for content.
 * Works out which slot a save goes in, so SavedRunDao only ever deals with slot numbers
 */
package expedition.io.db;

import expedition.engine.SavedRun;
import expedition.exception.DatabaseException;
import expedition.exception.SaveException;
import expedition.io.SaveSlot;
import expedition.io.SaveStore;
import java.util.List;

public class DerbySaveStore implements SaveStore {

    // what the autosave is called on the Load Game screen
    private static final String AUTOSAVE_NAME = "Autosave";

    private final SavedRunDao dao = new SavedRunDao();

    @Override
    public List<SaveSlot> list() throws SaveException {
        try {
            return dao.list();
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public void autosave(SavedRun run) throws SaveException {
        try {
            Integer slot = dao.autosaveSlot();
            if (slot == null) {
                slot = dao.nextSlot();
            }
            dao.save(slot, AUTOSAVE_NAME, true, run);
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public void saveAs(String name, SavedRun run) throws SaveException {
        // the name is checked here as well as on screen, so the column can never overflow
        String trimmed = (name == null) ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new SaveException("a save needs a name");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new SaveException("a save name can be at most " + MAX_NAME_LENGTH + " characters");
        }
        try {
            Integer slot = dao.slotNamed(trimmed);
            if (slot == null) {
                slot = dao.nextSlot();
            }
            dao.save(slot, trimmed, false, run);
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public SavedRun load(int slotId) throws SaveException {
        try {
            return dao.load(slotId);
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteAutosave() throws SaveException {
        try {
            Integer slot = dao.autosaveSlot();
            if (slot != null) {
                dao.delete(slot);
            }
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }
}
