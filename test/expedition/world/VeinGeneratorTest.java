/*
 * VeinGeneratorTest
 * Checks the veins follow the GDD's rules for every tier over lots of seeds: the depth and
 * the oil per level stay in the tier's range, pockets are never next to each other and
 * never on the last level, and the same seed always makes the same vein.
 * This replaces the VeinGeneratorCheck main methods, which only printed when something failed.
 */

package expedition.world;

import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

// AI assisted - Claude Code wrote this test, we reviewed it and ran it
public class VeinGeneratorTest {

    private static final int SEEDS = 200;

    @Test
    public void depthAndOilStayInTheTiersRange()
    {
        for (SiteTier tier : SiteTier.values()) {
            for (int seed = 0; seed < SEEDS; seed++) {
                List<Integer> levels = VeinGenerator.generate(tier, new Random(seed)).levelsForTesting();
                String where = tier + " seed " + seed + " " + levels;

                assertTrue(where, levels.size() >= tier.getMinDepth() && levels.size() <= tier.getMaxDepth());
                for (int oil : levels) {
                    // 0 is a pocket, anything else has to be in the tier's range
                    assertTrue(where, oil == 0 || (oil >= tier.getMinOil() && oil <= tier.getMaxOil()));
                }
            }
        }
    }

    @Test
    public void pocketsAreNeverNextToEachOtherOrOnTheLastLevel()
    {
        for (SiteTier tier : SiteTier.values()) {
            for (int seed = 0; seed < SEEDS; seed++) {
                List<Integer> levels = VeinGenerator.generate(tier, new Random(seed)).levelsForTesting();
                String where = tier + " seed " + seed + " " + levels;

                for (int i = 0; i < levels.size() - 1; i++) {
                    assertFalse("adjacent pockets at " + i + ": " + where, levels.get(i) == 0 && levels.get(i + 1) == 0);
                }
                assertNotEquals("the last level is a pocket: " + where, 0, (int) levels.get(levels.size() - 1));
            }
        }
    }

    @Test
    public void theSameSeedMakesTheSameVein()
    {
        for (SiteTier tier : SiteTier.values()) {
            assertEquals(VeinGenerator.generate(tier, new Random(42)).levelsForTesting(),
                    VeinGenerator.generate(tier, new Random(42)).levelsForTesting());
        }
    }
}
