/*
 * ShipTest
 * Checks the five ship parts: the hull percentage is their average, damage and repairs
 * stay between 0 and 100, and the ship only breaks once a part is gone.
 */

package expedition.ship;

import expedition.engine.GameConfig;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// AI assisted - Claude Code wrote this test, we reviewed it and ran it
public class ShipTest {

    private Ship ship;

    @Before
    public void setUp()
    {
        ship = new Ship(GameConfig.load());
    }

    @Test
    public void everyPartStartsFullAndTheHullIsTheirAverage()
    {
        for (ShipPart part : ShipPart.values()) {
            assertEquals(100, ship.part(part));
        }
        assertEquals(100, ship.hullPercent());

        // 50 off one of five parts takes 10 off the average
        ship.damage(ShipPart.ENGINE, 50);
        assertEquals(90, ship.hullPercent());
    }

    @Test
    public void damageAndRepairsStayBetween0And100()
    {
        ship.damage(ShipPart.PRESSURE, 250);
        assertEquals(0, ship.part(ShipPart.PRESSURE));

        ship.repair(ShipPart.PRESSURE, 30);
        ship.repair(ShipPart.PRESSURE, 90);
        assertEquals(100, ship.part(ShipPart.PRESSURE));
    }

    @Test
    public void theShipBreaksWhenAnyOnePartIsGone()
    {
        ship.damage(ShipPart.HULL, 99);
        ship.damage(ShipPart.WINDOWS, 99);
        assertFalse("parts at 1% still hold together", ship.isBroken());

        ship.damage(ShipPart.WINDOWS, 1);
        assertTrue(ship.isBroken());
    }

    @Test
    public void theMostDamagedPartIsTheOneWithTheLowestHealth()
    {
        assertEquals("a tie goes to the first part", ShipPart.HULL, ship.mostDamagedPart());

        ship.damage(ShipPart.EXHAUST, 20);
        ship.damage(ShipPart.ENGINE, 35);
        assertEquals(ShipPart.ENGINE, ship.mostDamagedPart());
    }

    @Test
    public void aResumedShipKeepsItsParts()
    {
        ship.damage(ShipPart.ENGINE, 40);
        Map<ShipPart, Integer> saved = ship.parts();

        Ship resumed = new Ship(GameConfig.load(), 120, saved);
        assertEquals(60, resumed.part(ShipPart.ENGINE));
        assertEquals(120, resumed.fuel());
        assertEquals(ship.hullPercent(), resumed.hullPercent());
    }
}
