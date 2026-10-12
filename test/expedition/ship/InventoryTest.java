/*
 * InventoryTest
 * Checks the inventory keeps to its capacity, takes items out by name
 * and groups them by name for the screen.
 */

package expedition.ship;

import expedition.item.Item;
import expedition.item.MedKit;
import expedition.item.Stimulant;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class InventoryTest {

    private Item caffeine() {
        return new Stimulant("Caffeine", "test", 20, 0);
    }

    private Item firstAid() {
        return new MedKit("First Aid Kit", "test", 40);
    }

    @Test
    public void addingPastCapacityIsRefusedAndNothingIsLost()
    {
        Inventory bag = new Inventory(2);
        assertTrue(bag.add(caffeine()));
        assertTrue(bag.add(firstAid()));

        // the 3rd item doesnt fit, the player has to choose what to throw away
        assertFalse("a full bag should refuse the item", bag.add(caffeine()));
        assertEquals(2, bag.size());
        assertTrue(bag.isFull());
    }

    @Test
    public void takeRemovesOneItemWithThatName()
    {
        Inventory bag = new Inventory(8);
        bag.add(caffeine());
        bag.add(caffeine());
        bag.add(firstAid());

        Item taken = bag.take("Caffeine");

        assertEquals("Caffeine", taken.name());
        assertEquals("only one should come out", 2, bag.size());
        assertEquals(Integer.valueOf(1), bag.stacked().get("Caffeine"));
    }

    @Test
    public void takeReturnsNullWhenThereIsNoneLeft()
    {
        Inventory bag = new Inventory(8);
        bag.add(firstAid());

        assertNull(bag.take("Caffeine"));
        assertEquals("nothing else should be removed", 1, bag.size());
    }

    @Test
    public void stackedGroupsItemsByNameInPickupOrder()
    {
        Inventory bag = new Inventory(8);
        bag.add(firstAid());
        bag.add(caffeine());
        bag.add(firstAid());

        Map<String, Integer> stacks = bag.stacked();

        assertEquals(2, stacks.size());
        assertEquals(Integer.valueOf(2), stacks.get("First Aid Kit"));
        assertEquals(Integer.valueOf(1), stacks.get("Caffeine"));
        // first aid was picked up first so it should be listed first
        assertEquals("First Aid Kit", stacks.keySet().iterator().next());
    }

    @Test
    public void removeRandomIsTheSameForTheSameSeed()
    {
        Inventory first = new Inventory(8);
        Inventory second = new Inventory(8);
        for (int i = 0; i < 4; i++) {
            first.add(i % 2 == 0 ? caffeine() : firstAid());
            second.add(i % 2 == 0 ? caffeine() : firstAid());
        }

        // a seeded run has to lose the same items every time it is replayed
        Random rngA = new Random(42);
        Random rngB = new Random(42);
        for (int i = 0; i < 3; i++) {
            assertEquals(first.removeRandom(rngA).name(), second.removeRandom(rngB).name());
        }
    }

    @Test
    public void removeRandomOnAnEmptyBagReturnsNull()
    {
        Inventory bag = new Inventory(8);
        assertNull(bag.removeRandom(new Random(1)));
        assertTrue(bag.isEmpty());
    }
}
