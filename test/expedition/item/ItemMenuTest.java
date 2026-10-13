/*
 * ItemMenuTest
 * Checks the use-item screen - using an item on a crew member, backing out
 * without losing it, items that would do nothing going back in the hold, and
 * only the team being offered as targets down on the surface.
 */

package expedition.item;

import expedition.crew.CrewMember;
import expedition.crew.Doctor;
import expedition.crew.Miner;
import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.expedition.Expedition;
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

public class ItemMenuTest {
    
    private Ship ship;
    private Inventory bag;
    private CrewMember braugn;
    private CrewMember mikela;
    private RunState state;
    private ByteArrayOutputStream printed;
    
    @Before
    public void setUp()
    {
        GameConfig config = GameConfig.load();
        ship = new Ship(config);
        bag = ship.inventory();
        braugn = new Miner("Braugn", 50, 40, 60);
        mikela = new Doctor("Mikela", 100, 100, 100);
        state = new RunState(1L, ship, List.of(braugn, mikela), List.of(), 0, config, new ItemTable(List.of()));
        printed = new ByteArrayOutputStream();
    }
    
    // a menu where the player types the given answers
    private ItemMenu menu(String typed)
    {
        ConsoleView view = new ConsoleView(new ByteArrayInputStream(typed.getBytes()), new PrintStream(printed));
        return new ItemMenu(state, view);
    }
    
    @Test
    public void usingAnItemOnACrewMemberTakesItOutOfTheHold()
    {
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        
        // item 1, crew member 1 (Braugn)
        menu("1\n1\n").open(null);
        
        assertEquals(60, braugn.getStamina());
        assertTrue(bag.isEmpty());
        assertTrue(printed.toString().contains("Used the Caffeine on Braugn: stamina +20"));
    }
    
    @Test
    public void backingOutOfTheCrewListKeepsTheItem()
    {
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        
        // item 1, then 3 is Back on a list of two crew
        menu("1\n3\n").open(null);
        
        assertEquals(1, bag.size());
        assertEquals(40, braugn.getStamina());
    }
    
    @Test
    public void anItemThatWouldDoNothingGoesBackInTheHold()
    {
        bag.add(new MedKit("First Aid Kit", "test", 40));
        
        // Mikela is already at full health
        menu("1\n2\n").open(null);
        
        assertEquals(1, bag.size());
        assertTrue(printed.toString().contains("goes back in the hold"));
    }
    
    @Test
    public void shipItemsDontAskForACrewMember()
    {
        ship.damageHull(30);
        bag.add(new RepairKit("Repair Kit", "test", 25));
        
        // only one answer, there is no crew list for a Repair Kit
        menu("1\n").open(null);
        
        assertEquals(95, ship.hullPercent());
        assertTrue(bag.isEmpty());
    }
    
    @Test
    public void onTheSurfaceOnlyTheTeamCanBeGivenAnItem()
    {
        bag.add(new Stimulant("Caffeine", "test", 20, 0));
        Expedition team = new Expedition(List.of(mikela));
        
        // crew member 1 is Mikela here, Braugn is up on the ship
        menu("1\n1\n").open(team);
        
        String output = printed.toString();
        assertFalse("Braugn isn't on the team", output.contains("Braugn"));
        assertEquals(40, braugn.getStamina());
    }
}
