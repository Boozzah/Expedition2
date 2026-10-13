/*
 * ItemTableTest
 * Checks the weighted item pick - items turn up by their drop weight, a 0 weight
 * never drops, the Engineer's bonus works, and a seeded run finds the same items.
 */

package expedition.item;

import expedition.crew.Engineer;
import expedition.crew.hooks.ItemFinder;
import expedition.exception.DataLoadException;
import expedition.io.TextFileDataSource;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTableTest {

    private ItemDefinition def(String name, ItemKind kind, int weight) {
        return new ItemDefinition(name, kind, 10, 0, weight, "test");
    }

    @Test
    public void anItemWithZeroWeightNeverDrops()
    {
        ItemTable table = new ItemTable(List.of(
                def("Caffeine", ItemKind.STIMULANT, 0),
                def("First Aid Kit", ItemKind.MEDKIT, 5)));

        Random rng = new Random(1);
        for (int i = 0; i < 500; i++) {
            assertEquals("First Aid Kit", table.roll(rng).name());
        }
    }

    @Test
    public void rollReturnsNullWhenNothingCanDrop()
    {
        ItemTable table = new ItemTable(List.of(def("Caffeine", ItemKind.STIMULANT, 0)));
        assertNull(table.roll(new Random(1)));
    }

    @Test
    public void engineerDoublesTheRepairKitWeight()
    {
        // equal weights, so without the Engineer it would be about half and half
        ItemTable table = new ItemTable(List.of(
                def("Repair Kit", ItemKind.REPAIR, 10),
                def("Caffeine", ItemKind.STIMULANT, 10)));
        List<ItemFinder> finders = List.of(new Engineer("Test", 100, 100, 100));

        Random rng = new Random(7);
        int repairKits = 0;
        for (int i = 0; i < 3000; i++) {
            if (table.roll(rng, finders).name().equals("Repair Kit")) {
                repairKits++;
            }
        }
        // with 20 to 10 it should be about two thirds, 2000 of 3000
        assertTrue("expected about 2000 repair kits, got " + repairKits, repairKits > 1850 && repairKits < 2150);
    }

    @Test
    public void sameSeedFindsTheSameItems() throws DataLoadException
    {
        ItemTable table = new ItemTable(new TextFileDataSource().loadItems());
        Random rngA = new Random(99);
        Random rngB = new Random(99);
        for (int i = 0; i < 50; i++) {
            assertEquals(table.roll(rngA).name(), table.roll(rngB).name());
        }
    }

    @Test
    public void factoryMakesTheRightKindOfItem()
    {
        assertTrue(ItemFactory.create(def("a", ItemKind.MEDKIT, 1)) instanceof MedKit);
        assertTrue(ItemFactory.create(def("a", ItemKind.STIMULANT, 1)) instanceof Stimulant);
        assertTrue(ItemFactory.create(def("a", ItemKind.COMFORT, 1)) instanceof ComfortItem);
        assertTrue(ItemFactory.create(def("a", ItemKind.REPAIR, 1)) instanceof RepairKit);
        assertTrue(ItemFactory.create(def("a", ItemKind.FUEL, 1)) instanceof FuelItem);
    }

    @Test
    public void itemsFileHasAllSixGddItems() throws DataLoadException
    {
        List<ItemDefinition> items = new TextFileDataSource().loadItems();
        assertEquals(6, items.size());

        // the Engineer looks for this exact name, so if it gets renamed in items.txt this fails
        boolean hasRepairKit = false;
        for (ItemDefinition d : items) {
            if (d.name().equals("Repair Kit") && d.kind() == ItemKind.REPAIR) {
                hasRepairKit = true;
            }
        }
        assertTrue("items.txt needs a REPAIR item called \"Repair Kit\"", hasRepairKit);
    }
}
