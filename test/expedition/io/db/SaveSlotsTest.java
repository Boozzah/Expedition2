/*
 * SaveSlotsTest
 * Checks named saves go into the database and come back out the same, that saving under
 * a name that is already used replaces that save, and that the Load Game list shows them.
 * This uses the real database like the other DAO tests, so it only touches its own save
 * and removes it at the end. The player's autosave and saves are left alone.
 */

package expedition.io.db;

import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.engine.SavedRun;
import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.exception.SaveException;
import expedition.io.SaveSlot;
import expedition.io.TextFileDataSource;
import expedition.item.ItemTable;
import expedition.ship.Ship;
import expedition.ship.ShipPart;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SaveSlotsTest {

    private static final String NAME = "JUnit SaveSlotsTest";

    private final DerbySaveStore store = new DerbySaveStore();
    private final SavedRunDao dao = new SavedRunDao();
    private SavedRun run;

    @Before
    public void setUp() throws DatabaseException, DataLoadException
    {
        new DatabaseInitialiser(new TextFileDataSource()).initialise();

        TextFileDataSource data = new TextFileDataSource();
        GameConfig config = GameConfig.load();
        RunState state = new RunState(42L, new Ship(config), data.loadCrew(), data.loadPlanets(), 8, config,
                new ItemTable(data.loadItems()));
        state.ship().damage(ShipPart.ENGINE, 30);
        state.score().oilDrilled(64);
        run = state.checkpoint();
    }

    @After
    public void removeTestSave() throws DatabaseException
    {
        Integer slot = dao.slotNamed(NAME);
        if (slot != null) {
            dao.delete(slot);
        }
    }

    @Test
    public void aNamedSaveComesBackTheSame() throws SaveException, DatabaseException
    {
        store.saveAs(NAME, run);

        SavedRun loaded = store.load(dao.slotNamed(NAME));
        assertEquals(run, loaded);
    }

    @Test
    public void savingUnderTheSameNameReplacesThatSave() throws SaveException
    {
        store.saveAs(NAME, run);
        int before = store.list().size();

        store.saveAs(NAME, run);
        assertEquals("the second save should replace the first", before, store.list().size());
    }

    @Test
    public void theNewestSaveIsFirstInTheList() throws SaveException
    {
        store.saveAs(NAME, run);

        List<SaveSlot> slots = store.list();
        SaveSlot first = slots.get(0);
        assertEquals(NAME, first.name());
        assertFalse(first.autosave());
        assertEquals(1, first.planetsReached());
    }

    @Test(expected = SaveException.class)
    public void aBlankNameIsRefused() throws SaveException
    {
        store.saveAs("   ", run);
    }
}
