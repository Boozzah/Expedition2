/*
 * SaveRoundTripTest
 * Checks a run can be turned into a SavedRun and back without anything changing,
 * and that the resumed run rolls the same random numbers as the one that never stopped.
 * Uses the text files for content so it doesn't touch the database.
 */

package expedition.engine;

import expedition.crew.CrewMember;
import expedition.exception.DataLoadException;
import expedition.exception.SaveException;
import expedition.io.TextFileDataSource;
import expedition.item.ItemTable;
import expedition.ship.Ship;
import expedition.world.Planet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SaveRoundTripTest {

    private final TextFileDataSource data = new TextFileDataSource();
    private GameConfig config;
    private ItemTable items;
    private List<Planet> planets;

    @Before
    public void loadContent() throws DataLoadException
    {
        config = GameConfig.load();
        items = new ItemTable(data.loadItems());
        planets = data.loadPlanets();
    }

    // a run a few planets in, with some damage, a death and some items, so every
    // part of the snapshot has something other than its starting value
    private RunState runPartWayThrough() throws DataLoadException
    {
        RunState state = new RunState(42L, new Ship(config), data.loadCrew(), planets, 8, config, items);
        state.checkpoint();
        state.advancePlanet(null);
        state.checkpoint();
        state.advancePlanet(null);

        state.ship().spendFuel(30);
        state.ship().damageHull(15);
        state.addOil(12);
        state.ship().inventory().add(items.create("First Aid Kit"));
        state.ship().inventory().add(items.create("Repair Kit"));

        CrewMember first = state.crew().get(0);
        first.loseHealth(25);
        first.loseStamina(10);
        state.killCrew(state.crew().get(1));
        return state;
    }

    @Test
    public void restoringASaveGivesBackTheSameRunWithTheSameRolls() throws DataLoadException, SaveException
    {
        RunState original = runPartWayThrough();
        SavedRun saved = original.checkpoint();

        RunState resumed = RunState.restore(saved, data.loadPlanets(), items, config);

        // SavedRun is a record, so equals compares every field including the order of
        // the route, the items and the crew. Checkpointing the resumed run again (the engine
        // does this on arrival) must not reseed a second time or the rolls would drift apart
        assertEquals(saved, resumed.checkpoint());
        assertEquals(original.currentPlanet().name(), resumed.currentPlanet().name());
        for (int i = 0; i < 50; i++) {
            assertEquals("roll " + i, original.rng().nextInt(1000), resumed.rng().nextInt(1000));
        }
    }
}
