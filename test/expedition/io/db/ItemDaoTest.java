/*
 * ItemDaoTest
 * Checks the items make the round trip from items.txt into the ITEMS table and
 * back out again unchanged, and in the same order.
 */

package expedition.io.db;

import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.io.TextFileDataSource;
import expedition.item.ItemDefinition;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemDaoTest {

    // compares against items.txt like CrewDaoTest does, so no test rows are added to the real database
    @Test
    public void findAllReturnsTheSameItemsAsTheTextFileInTheSameOrder() throws DatabaseException, DataLoadException
    {
        new DatabaseInitialiser(new TextFileDataSource()).initialise();

        List<ItemDefinition> expected = new TextFileDataSource().loadItems();
        List<ItemDefinition> actual = new ItemDao().findAll();

        // ItemDefinition is a record, so equals compares every field. no sorting here
        // because the order matters - a seeded run's item picks depend on it
        assertEquals(expected, actual);
    }
}
