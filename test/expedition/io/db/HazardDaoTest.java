/*                                                          
   * HazardDaoTest
   * Checks the hazards make the round trip from hazards.txt into the HAZARDS and
   * HAZARD_OUTCOMES tables and back out again - every hazard, every severity
 */
package expedition.io.db;

import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.hazard.Hazard;
import expedition.hazard.HazardOutcome;
import expedition.io.TextFileDataSource;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class HazardDaoTest {

    @Test
    public void findAllReturnsTheSameHazardsAsTheTextFile() throws DatabaseException,
            DataLoadException {
        new DatabaseInitialiser(new TextFileDataSource()).initialise();

        List<Hazard> expected = new TextFileDataSource().loadHazards();
        List<Hazard> actual = new HazardDao().findAll();

        assertEquals("should be the same number of hazards", expected.size(), actual.size());

        // in order - a seeded run picks hazards by position, so the order has to match
        for (int i = 0; i < expected.size(); i++) {
            Hazard e = expected.get(i);
            Hazard a = actual.get(i);
            assertEquals(e.name(), a.name());
            assertEquals(e.name() + " set", e.set(), a.set());

            for (int severity = 1; severity <= 6; severity++) {
                HazardOutcome eo = e.outcomeFor(severity);
                HazardOutcome ao = a.outcomeFor(severity);
                String where = e.name() + " severity " + severity;
                assertEquals(where + " text", eo.text(), ao.text());
                assertEquals(where + " codes", eo.effectCodes(), ao.effectCodes());
                // Effect has no equals(), so check the codes parsed into the same number of effects
                assertEquals(where + " effect count", eo.effects().size(), ao.effects().size());
            }
        }
    }
}
