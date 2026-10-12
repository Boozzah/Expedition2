/*
 * ItemTest
 * Checks each kind of item changes the right thing when it is used,
 * and stops at the right limit.
 */

package expedition.item;

import expedition.crew.CrewMember;
import expedition.crew.Miner;
import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.hazard.effect.EffectContext;
import expedition.ship.Ship;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemTest {

    private Ship ship;
    private EffectContext ctx;
    private CrewMember crew;

    // a run with no crew or planets is enough, the items only touch the ship and the target
    @Before
    public void setUp()
    {
        GameConfig config = GameConfig.load();
        ship = new Ship(config);
        RunState state = new RunState(1L, ship, List.of(), List.of(), 0, config);
        ctx = new EffectContext(state, null, null);
        crew = new Miner("Test", 50, 50, 50);
    }

    @Test
    public void medKitHealsButNotPastTheMax()
    {
        new MedKit("First Aid Kit", "test", 40).use(ctx, crew);
        assertEquals(90, crew.getHealth());

        new MedKit("First Aid Kit", "test", 40).use(ctx, crew);
        assertEquals(100, crew.getHealth());
    }

    @Test
    public void stimulantGivesStaminaAndComposure()
    {
        new Stimulant("White Monster", "test", 35, 15).use(ctx, crew);
        assertEquals(85, crew.getStamina());
        assertEquals(65, crew.getComposure());
    }

    @Test
    public void comfortItemGivesComposure()
    {
        new ComfortItem("Sensory Toy", "test", 30).use(ctx, crew);
        assertEquals(80, crew.getComposure());
    }

    @Test
    public void repairKitFixesTheHullButNotPast100()
    {
        ship.damageHull(40);
        new RepairKit("Repair Kit", "test", 25).use(ctx, null);
        assertEquals(85, ship.hullPercent());

        new RepairKit("Repair Kit", "test", 25).use(ctx, null);
        assertEquals(100, ship.hullPercent());
    }

    @Test
    public void fuelItemCanGoOverTheTankCap()
    {
        int capacity = GameConfig.load().tankCapacity();
        ship.addFuel(capacity);

        new FuelItem("Condensed Oil", "test", 60).use(ctx, null);
        assertEquals(capacity + 60, ship.fuel());
    }

    @Test
    public void onlyCrewItemsNeedATarget()
    {
        assertTrue(new MedKit("a", "b", 1).needsCrewTarget());
        assertTrue(new Stimulant("a", "b", 1, 0).needsCrewTarget());
        assertTrue(new ComfortItem("a", "b", 1).needsCrewTarget());
        assertFalse(new RepairKit("a", "b", 1).needsCrewTarget());
        assertFalse(new FuelItem("a", "b", 1).needsCrewTarget());
    }

    @Test
    public void shipStartsWithAnEmptyInventoryOfTheConfiguredSize()
    {
        assertTrue(ship.inventory().isEmpty());
        assertEquals(GameConfig.load().inventorySlots(), ship.inventory().capacity());
    }
}
