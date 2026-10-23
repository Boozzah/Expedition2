/*
 * EventDaoTest
 * Checks the jump events make the round trip from events.txt into the EVENTS table and
 * back out again unchanged, and in the same order.
 */

package expedition.io.db;

import expedition.event.JumpEvent;
import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.io.TextFileDataSource;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventDaoTest {

    // compares against events.txt like ItemDaoTest does, so no test rows are added to the real database
    @Test
    public void findAllReturnsTheSameEventsAsTheTextFileInTheSameOrder() throws DatabaseException, DataLoadException
    {
        new DatabaseInitialiser(new TextFileDataSource()).initialise();

        List<JumpEvent> expected = new TextFileDataSource().loadEvents();
        List<JumpEvent> actual = new EventDao().findAll();

        // the Effect objects are rebuilt from the codes, so they are compared by their codes
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).text(), actual.get(i).text());
            assertEquals(expected.get(i).effectCodes(), actual.get(i).effectCodes());
            assertEquals(expected.get(i).points(), actual.get(i).points());
            assertEquals(expected.get(i).weight(), actual.get(i).weight());
            assertEquals(expected.get(i).effects().size(), actual.get(i).effects().size());
        }
    }
}
