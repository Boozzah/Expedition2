/*
 * EventTableTest
 * Checks the jump event roll: the chance decides whether anything happens at all,
 * events with a weight of 0 never come up, and a seeded run gets the same events.
 */

package expedition.event;

import expedition.exception.DataLoadException;
import expedition.io.TextFileDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class EventTableTest {

    private JumpEvent event(String text, int weight) {
        return new JumpEvent(text, List.of(), "", 0, weight);
    }

    @Test
    public void aChanceOf0NeverRollsAnEventAnd100AlwaysDoes()
    {
        List<JumpEvent> events = List.of(event("a", 1), event("b", 1));
        EventTable never = new EventTable(events, 0);
        EventTable always = new EventTable(events, 100);
        Random rng = new Random(1);

        for (int i = 0; i < 200; i++) {
            assertNull(never.roll(rng));
            assertNotNull(always.roll(rng));
        }
    }

    @Test
    public void anEventWithAWeightOf0NeverComesUp()
    {
        EventTable table = new EventTable(List.of(event("never", 0), event("always", 5)), 100);
        Random rng = new Random(7);

        for (int i = 0; i < 200; i++) {
            assertEquals("always", table.roll(rng).text());
        }
    }

    @Test
    public void theSameSeedRollsTheSameEvents() throws DataLoadException
    {
        EventTable table = new EventTable(new TextFileDataSource().loadEvents(), 60);

        List<JumpEvent> first = new ArrayList<>();
        List<JumpEvent> second = new ArrayList<>();
        Random a = new Random(42);
        Random b = new Random(42);
        for (int i = 0; i < 50; i++) {
            first.add(table.roll(a));
            second.add(table.roll(b));
        }
        assertEquals(first, second);
    }

    @Test
    public void everyEventInTheFileLoads() throws DataLoadException
    {
        List<JumpEvent> events = new TextFileDataSource().loadEvents();
        assertFalse(events.isEmpty());
        for (JumpEvent e : events) {
            assertFalse(e.text().isEmpty());
        }
    }
}
