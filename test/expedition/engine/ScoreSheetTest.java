/*
 * ScoreSheetTest
 * Checks the run's score adds up the way the breakdown says it does: oil carries its
 * leftover between hauls, the ending adds its points once, and Back in time costs points.
 */
package expedition.engine;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

// AI assisted - Claude Code wrote this test, we reviewed it and ran it
public class ScoreSheetTest {

    @Test
    public void oilUnderTenCarriesOverToTheNextHaul()
    {
        ScoreSheet sheet = new ScoreSheet();
        assertEquals(2, sheet.oilDrilled(27));
        // 27 + 13 = 40, so this haul tips it over to 4
        assertEquals(2, sheet.oilDrilled(13));
        assertEquals(0, sheet.oilDrilled(9));
        assertEquals(4, sheet.total());
    }

    @Test
    public void onlyLevelsPastFiveGetTheDeepDrillingBonus()
    {
        ScoreSheet sheet = new ScoreSheet();
        for (int depth = 1; depth <= 5; depth++) {
            assertEquals("level " + depth, 0, sheet.levelDrilled(depth));
        }
        assertTrue(sheet.levelDrilled(6) > 0);
        assertTrue(sheet.levelDrilled(9) > 0);
    }

    @Test
    public void gettingHomeScoresTheSurvivorsAndTakesOffTheDead()
    {
        ScoreSheet home = new ScoreSheet();
        home.finish(new Ending(EndingReason.HOME, 8, List.of("Braugn", "Mikela"), List.of("Henry"), 500));

        ScoreSheet adrift = new ScoreSheet();
        adrift.finish(new Ending(EndingReason.ADRIFT, 8, List.of("Braugn", "Mikela"), List.of("Henry"), 500));

        Map<ScoreSheet.Reason, Integer> lines = home.breakdown();
        assertTrue(lines.get(ScoreSheet.Reason.HOME) > 0);
        assertTrue(lines.get(ScoreSheet.Reason.SURVIVORS) > 0);
        assertTrue(lines.get(ScoreSheet.Reason.DEATHS) < 0);

        // adrift kills whoever was left, so there is nothing to score for them
        assertFalse(adrift.breakdown().containsKey(ScoreSheet.Reason.SURVIVORS));
        assertTrue(home.total() > adrift.total());
    }

    @Test
    public void backInTimeCanTakeTheScoreNegative()
    {
        ScoreSheet sheet = new ScoreSheet();
        sheet.planetReached();
        sheet.wentBackInTime();
        assertEquals(-ScoreSheet.BACK_IN_TIME_COST, (int) sheet.breakdown().get(ScoreSheet.Reason.BACK_IN_TIME));
        assertTrue(sheet.total() < 0);
    }

    @Test
    public void theBreakdownAddsUpToTheTotalAndSkipsEmptyLines()
    {
        ScoreSheet sheet = new ScoreSheet();
        sheet.oilDrilled(55);
        sheet.planetReached();
        sheet.itemCollected();
        sheet.partRepaired();
        sheet.jumpEvent(-15);

        int sum = 0;
        for (int points : sheet.breakdown().values()) {
            sum += points;
        }
        assertEquals(sheet.total(), sum);
        assertFalse(sheet.breakdown().containsKey(ScoreSheet.Reason.HOME));
    }

    @Test
    public void aResumedSheetCarriesOnFromTheSave()
    {
        ScoreSheet sheet = new ScoreSheet();
        sheet.oilDrilled(37);
        sheet.planetReached();

        ScoreSheet resumed = new ScoreSheet(sheet.breakdown(), sheet.oilDrilledSoFar());
        assertEquals(sheet.total(), resumed.total());
        // the 7 left over from before the save still counts
        assertEquals(1, resumed.oilDrilled(3));
    }
}
