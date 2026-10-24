/*
 * DrillSessionItemTest
 * Checks finding items while drilling - an item goes in the hold, a full hold
 * asks what to throw out, and the Surveyor's bonus shows in the drill status.
 */

package expedition.expedition;

import expedition.crew.CrewMember;
import expedition.crew.Role;
import expedition.crew.Surveyor;
import expedition.crew.hooks.ItemFinder;
import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.exception.DataLoadException;
import expedition.io.TextFileDataSource;
import expedition.item.ItemTable;
import expedition.item.MedKit;
import expedition.ship.Inventory;
import expedition.ship.Ship;
import expedition.ship.ShipPart;
import expedition.ui.ConsoleView;
import expedition.world.DrillSite;
import expedition.world.SiteTier;
import expedition.world.Vein;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// AI assisted - Claude Code wrote this test, we reviewed it and ran it
public class DrillSessionItemTest {
    
    // a crew member who always finds something, so the test doesnt depend on an 8% roll
    private static class AlwaysFinds extends CrewMember implements ItemFinder {
        AlwaysFinds() {
            super("Lucky", 100, 100, 100);
        }
        @Override
        public double modifyDropChance(double base) {
            return 1.0;
        }
        @Override
        public int modifyDropWeight(String itemName, int baseWeight) {
            return baseWeight;
        }
        @Override
        public Role getRole() {
            return Role.SURVEYOR;
        }
    }
    
    private GameConfig config;
    private RunState state;
    private Inventory bag;
    private ByteArrayOutputStream printed;
    
    @Before
    public void setUp() throws DataLoadException
    {
        config = GameConfig.load();
        Ship ship = new Ship(config);
        ItemTable items = new ItemTable(new TextFileDataSource().loadItems());
        state = new RunState(1L, ship, List.of(), List.of(), 0, config, items);
        bag = ship.inventory();
        printed = new ByteArrayOutputStream();
    }
    
    // a session on a small vein with no hazards, where the player types the given answers
    private DrillSession session(CrewMember member, String typed)
    {
        ConsoleView view = new ConsoleView(new ByteArrayInputStream(typed.getBytes()), new PrintStream(printed));
        DrillSite site = new DrillSite("Test Site", SiteTier.LOW, new Vein(List.of(10, 10, 10)));
        return new DrillSession(site, new Expedition(List.of(member)), state, view, config, List.of(), 0);
    }
    
    // fills the hold with something that isn't in items.txt, so it can't be mixed up with a find
    private void fillHold()
    {
        while (!bag.isFull()) {
            bag.add(new MedKit("Old Bandage", "test", 1));
        }
    }
    
    @Test
    public void aFindGoesInTheHold()
    {
        session(new AlwaysFinds(), "").drillOneLevel();
        
        assertEquals(1, bag.size());
        assertTrue(printed.toString().contains("Found: "));
    }
    
    @Test
    public void aFullHoldCanLeaveTheNewItemBehind()
    {
        fillHold();
        session(new AlwaysFinds(), "1\n").drillOneLevel();
        
        assertEquals(bag.capacity(), bag.size());
        assertEquals("nothing should have been thrown out", Integer.valueOf(bag.capacity()), bag.stacked().get("Old Bandage"));
        assertTrue(printed.toString().contains("left behind"));
    }
    
    @Test
    public void aFullHoldCanThrowSomethingOutForTheNewItem()
    {
        fillHold();
        // option 2 is the first kind of item in the hold, the Old Bandages
        session(new AlwaysFinds(), "2\n").drillOneLevel();
        
        assertEquals(bag.capacity(), bag.size());
        assertEquals(Integer.valueOf(bag.capacity() - 1), bag.stacked().get("Old Bandage"));
        assertTrue(printed.toString().contains("Threw out one Old Bandage"));
    }
    
    @Test
    public void drillMenuCanUseAnItemWithoutDrilling()
    {
        state.ship().damage(ShipPart.ENGINE, 30);
        bag.add(new expedition.item.RepairKit("Repair Kit", "test", 25));
        
        // 3 = use item, 1 = the Repair Kit, then 2 = ascend
        DrillSession session = session(new Surveyor("Hannah", 100, 100, 100), "3\n1\n2\n");
        session.run();
        
        assertEquals(95, state.ship().part(ShipPart.ENGINE));
        assertFalse("using an item shouldn't drill a level", printed.toString().contains("Level 1:"));
    }
    
    @Test
    public void drillStatusShowsTheSurveyorBonus()
    {
        // 2 = ascend straight away, run() still prints the status first
        session(new Surveyor("Hannah", 100, 100, 100), "2\n").run();
        assertTrue(printed.toString(), printed.toString().contains("Item chance: 15%"));
    }
    
    @Test
    public void aFatiguedSurveyorGivesNoBonus()
    {
        // stamina under 50 means fatigued, so the ability doesnt fire
        session(new Surveyor("Hannah", 100, 30, 100), "2\n").run();
        assertTrue(printed.toString(), printed.toString().contains("Item chance: 8%"));
    }
}
