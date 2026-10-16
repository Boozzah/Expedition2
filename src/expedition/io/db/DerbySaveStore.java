/*
 * DerbySaveStore
 * The database version of SaveStore. Hands each request to SavedRunDao and turns any
 * DatabaseException into a SaveException, the same way DerbyDataSource does for content
 */
package expedition.io.db;

import expedition.engine.SavedRun;
import expedition.exception.DatabaseException;
import expedition.exception.SaveException;
import expedition.io.SaveStore;

public class DerbySaveStore implements SaveStore {

    private final SavedRunDao dao = new SavedRunDao();

    @Override
    public boolean hasSave() throws SaveException {
        try {
            return dao.exists();
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public void save(SavedRun run) throws SaveException {
        try {
            dao.save(run);
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public SavedRun load() throws SaveException {
        try {
            return dao.load();
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }

    @Override
    public void delete() throws SaveException {
        try {
            dao.delete();
        } catch (DatabaseException e) {
            throw new SaveException(e.getMessage(), e);
        }
    }
}
