/*
 * PlanetDaoTest
 * Checks the planets make the round trup from planets.txt into the PLANETS table and 
 * back out again unchanged
*/
package expedition.io.db;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.io.TextFileDataSource;
import expedition.world.Planet;

public class PlanetDaoTest {
    
    @Test
    public void findAllReturnsTheSamePlanetsAsTheTextFile() throws DatabaseException, DataLoadException {
        new DatabaseInitialiser(new TextFileDataSource()).initialise();
        
        List<Planet> expected = new TextFileDataSource().loadPlanets();
        List<Planet> actual = new PlanetDao().findAll();
        
        assertEquals("should be the same number of planets", expected.size(), actual.size());
        
        // compare in order, which also checks ORDER BY ID keeps the text file order
        for (int i = 0; i < expected.size(); i++) {
            Planet e = expected.get(i);
            Planet a = actual.get(i);
            
            assertEquals(e.name(), a.name());
            assertEquals(e.name() + " archetype", e.archetype(), a.archetype());
            // this is the one with apostrophes in it
            assertEquals(e.name() + " flavour", e.introFlavour(), a.introFlavour());
            // List.equals checks same names, same order
            assertEquals(e.name() + " sites", e.siteNamePool(), a.siteNamePool());
        }
    }
}
