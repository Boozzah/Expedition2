/*
 * SavedRunDaoTest
 * Checks a save makes the round trip into SAVED_RUN and SAVED_CREW and back out
 * unchanged, that a new save fully replaces the old one, and that delete clears it.
 * These tests use the real database, so whatever save was there before is put back after.
 */

package expedition.io.db;

import expedition.crew.Role;
import expedition.engine.SavedRun;
import expedition.engine.SavedRun.SavedCrew;
import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.io.TextFileDataSource;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SavedRunDaoTest {

    private final SavedRunDao dao = new SavedRunDao();

    // the player's own save, if they had one
    private SavedRun playersSave;

    @Before
    public void keepThePlayersSave() throws DatabaseException, DataLoadException
    {
        new DatabaseInitialiser(new TextFileDataSource()).initialise();
        playersSave = dao.load();
    }

    @After
    public void putThePlayersSaveBack() throws DatabaseException
    {
        if (playersSave != null) {
            dao.save(playersSave);
        } else {
            dao.delete();
        }
    }

    private SavedRun sample(List<String> items, List<SavedCrew> crew)
    {
        // seeds bigger than an INTEGER can hold, to check the BIGINT columns
        return new SavedRun(8_000_000_000L, -5_000_000_000L, 3, 140, 95, 70,
                List.of("Hades", "Asteria", "Vayle"), items, crew);
    }

    private final List<SavedCrew> threeCrew = List.of(
            new SavedCrew("Zed", Role.MINER, 80, 60, 55, true),
            new SavedCrew("Ana", Role.DOCTOR, 0, 40, 30, false),
            new SavedCrew("Mo", Role.PILOT, 65, 45, 70, true));

    @Test
    public void aSavedRunComesBackUnchangedAndInTheSameOrder() throws DatabaseException
    {
        // crew deliberately not in name order, POSITION has to keep them as they were
        SavedRun saved = sample(List.of("Repair Kit", "Caffeine", "Repair Kit"), threeCrew);
        dao.save(saved);

        assertEquals(saved, dao.load());
    }

    @Test
    public void anEmptyInventoryComesBackEmpty() throws DatabaseException
    {
        dao.save(sample(List.of(), threeCrew));

        assertEquals(List.of(), dao.load().items());
    }

    @Test
    public void savingAgainReplacesTheOldSave() throws DatabaseException
    {
        dao.save(sample(List.of("Caffeine"), threeCrew));
        SavedRun smaller = sample(List.of(), threeCrew.subList(0, 1));
        dao.save(smaller);

        // no crew rows left over from the first save
        assertEquals(smaller, dao.load());
    }

    @Test
    public void deleteLeavesNoSave() throws DatabaseException
    {
        dao.save(sample(List.of(), threeCrew));
        dao.delete();

        assertNull(dao.load());
        assertFalse(dao.exists());
    }
}
