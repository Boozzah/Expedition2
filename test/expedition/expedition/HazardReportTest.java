/*
 * HazardReportTest
 * Checks the hazard report tells the player which items were found or lost,
 * since the effects themselves don't say anything.
 */

package expedition.expedition;

import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.item.ItemTable;
import expedition.item.MedKit;
import expedition.item.Stimulant;
import expedition.ship.Inventory;
import expedition.ship.Ship;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// AI assisted - Claude Code wrote this test, we reviewed it and ran it
public class HazardReportTest {
    
    private RunState state;
    private Inventory bag;
    
    @Before
    public void setUp()
    {
        GameConfig config = GameConfig.load();
        Ship ship = new Ship(config);
        state = new RunState(1L, ship, List.of(), List.of(), 0, config, new ItemTable(List.of()));
        bag = ship.inventory();
    }
    
    @Test
    public void reportsItemsFoundAndLost()
    {
        bag.add(new MedKit("First Aid Kit", "test", 40));
        HazardReport report = new HazardReport(state);
        
        // what a hazard might do: take the first aid kit, give two caffeine
        bag.take("First Aid Kit");
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        report.addShipChanges(0);
        
        assertTrue(report.lines().toString(), report.lines().contains("found: Caffeine x2"));
        assertTrue(report.lines().toString(), report.lines().contains("lost: First Aid Kit"));
    }
    
    @Test
    public void reportsNothingWhenTheInventoryDidNotChange()
    {
        bag.add(new MedKit("First Aid Kit", "test", 40));
        HazardReport report = new HazardReport(state);
        report.addShipChanges(0);
        
        assertTrue(report.lines().isEmpty());
    }
}
