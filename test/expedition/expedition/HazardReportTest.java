/*
 * HazardReportTest
 * Checks the hazard report tells the player which items were found or lost,
 * since the effects themselves don't print anything.
 */

package expedition.expedition;

import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.item.ItemTable;
import expedition.item.MedKit;
import expedition.item.Stimulant;
import expedition.ship.Inventory;
import expedition.ship.Ship;
import expedition.ui.ConsoleView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class HazardReportTest {
    
    private RunState state;
    private Inventory bag;
    private ConsoleView view;
    // everything the view prints ends up in here instead of the console
    private ByteArrayOutputStream printed;
    
    @Before
    public void setUp()
    {
        GameConfig config = GameConfig.load();
        Ship ship = new Ship(config);
        state = new RunState(1L, ship, List.of(), List.of(), 0, config, new ItemTable(List.of()));
        bag = ship.inventory();
        printed = new ByteArrayOutputStream();
        view = new ConsoleView(new ByteArrayInputStream(new byte[0]), new PrintStream(printed));
    }
    
    @Test
    public void reportsItemsFoundAndLost()
    {
        bag.add(new MedKit("First Aid Kit", "test", 40));
        HazardReport report = new HazardReport(state, view);
        
        // what a hazard might do: take the first aid kit, give two caffeine
        bag.take("First Aid Kit");
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        report.printShipChanges(0);
        
        String output = printed.toString();
        assertTrue(output, output.contains("found: Caffeine x2"));
        assertTrue(output, output.contains("lost: First Aid Kit"));
    }
    
    @Test
    public void printsNothingWhenTheInventoryDidNotChange()
    {
        bag.add(new MedKit("First Aid Kit", "test", 40));
        HazardReport report = new HazardReport(state, view);
        report.printShipChanges(0);
        
        assertEquals("", printed.toString());
    }
}
