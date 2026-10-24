/*
 * ItemGrantEffectTest
 * Checks ITEM:+n gives items, ITEM:-n takes them away, and neither one
 * goes past a full or empty inventory.
 */

package expedition.hazard.effect;

import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.exception.DataLoadException;
import expedition.io.TextFileDataSource;
import expedition.item.ItemTable;
import expedition.ship.Inventory;
import expedition.ship.Ship;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// AI assisted - Claude Code wrote this test, we reviewed it and ran it
public class ItemGrantEffectTest {
    
    private Inventory bag;
    private EffectContext ctx;
    
    // a run with the real item list from items.txt, but no crew or planets
    @Before
    public void setUp() throws DataLoadException
    {
        GameConfig config = GameConfig.load();
        Ship ship = new Ship(config);
        ItemTable items = new ItemTable(new TextFileDataSource().loadItems());
        RunState state = new RunState(1L, ship, List.of(), List.of(), 0, config, items);
        ctx = new EffectContext(state, null, null);
        bag = ship.inventory();
    }
    
    @Test
    public void positiveCountAddsThatManyItems()
    {
        new ItemGrantEffect(3).apply(ctx);
        assertEquals(3, bag.size());
    }
    
    @Test
    public void negativeCountRemovesThatManyItems()
    {
        new ItemGrantEffect(4).apply(ctx);
        new ItemGrantEffect(-2).apply(ctx);
        assertEquals(2, bag.size());
    }
    
    @Test
    public void removingMoreThanThereAreJustEmptiesTheBag()
    {
        new ItemGrantEffect(1).apply(ctx);
        new ItemGrantEffect(-3).apply(ctx);
        assertTrue(bag.isEmpty());
    }
    
    @Test
    public void aFullBagStaysAtCapacity()
    {
        new ItemGrantEffect(bag.capacity() + 3).apply(ctx);
        assertEquals(bag.capacity(), bag.size());
    }
}
