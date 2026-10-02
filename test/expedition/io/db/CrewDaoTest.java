  /*
   * CrewDaoTest
   * Checks the crew make the round trip from crew.txt into the CREW table and
   * back out again unchanged.
  */

  package expedition.io.db;

  import expedition.crew.CrewMember;
  import expedition.exception.DataLoadException;
  import expedition.exception.DatabaseException;
  import expedition.io.TextFileDataSource;
  import java.util.Comparator;
  import java.util.List;
  import org.junit.Test;
  import static org.junit.Assert.*;

public class CrewDaoTest {
    // AI assisted - Claude seggested comparing against crew.txt instead of inserting test rows
    // so the test never adds fake crew to the real database
    @Test
    public void findAllReturnsTheSameCrewAsTheTextFile() throws DatabaseException, DataLoadException
    {
        // seeds crew from crew.txt if this is the first run
        new DatabaseInitialiser(new TextFileDataSource()).initialise();
        
        List<CrewMember> expected = new TextFileDataSource().loadCrew();
        List<CrewMember> actual = new CrewDao().findAll();
        
        // findAll sorts by name and the text file doesn't, so sort both the same way
        expected.sort(Comparator.comparing(CrewMember::getName));
        actual.sort(Comparator.comparing(CrewMember::getName));
        
        assertEquals("Shoudl be the same number of crew", expected.size(), actual.size());
        
        // assertions that they are the same
        for (int i = 0; i < expected.size(); i++) {
            CrewMember e = expected.get(i);
            CrewMember a = actual.get(i);
            assertEquals(e.getName(), a.getName());
            assertEquals(e.getName() + " role", e.getRole(), a.getRole());
            assertEquals(e.getName() + " health", e.getHealth(), a.getHealth());
            assertEquals(e.getName() + " stamina", e.getStamina(), a.getStamina());
            assertEquals(e.getName() + " composure", e.getComposure(), a.getComposure());
        }
    }
}
