/*
 * SeverityRoller
 * Weighted 1-6 severity roll, banded by drill depth (deeper = worse
 * odds). rollDefault() gives the fixed band used for ascent and jump
 * hazards.
 */
package expedition.hazard;

import expedition.util.WeightedRoller;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class SeverityRoller {
    
    // --- TEST TO MAKE SURE THE ROLLER WORKS ----
    /*
    public static void main(String[] args) {
          // 1. Distribution check: roll each band many times with a fixed seed,
          //    count outcomes, and print the percentage each severity landed at.
          //    Compare by eye against the GDD §6.1 table — e.g. depth 2 (Levels 1-3)
          //    should land near 40/28/18/9/4/1, not skip severity 2 entirely.
          Random rng = new Random(42);
          int[] counts = new int[7]; // index 1-6 used, 0 unused
          int rolls = 100_000;
          for (int i = 0; i < rolls; i++) {
              int severity = SeverityRoller.roll(2, rng); // depth 2 -> Levels 1-3 band
              counts[severity]++;
          }
          for (int s = 1; s <= 6; s++) {
              System.out.printf("severity %d: %.1f%%%n", s, 100.0 * counts[s] / rolls);
          }

          // 2. Band-boundary check: confirm depth 3 vs 4, 6 vs 7, 9 vs 10 pick the
          //    right band (do this by checking a specific depth doesn't crash and
          //    roughly matches the expected band's shape from part 1).
          for (int depth : new int[]{1, 3, 4, 6, 7, 9, 10, 15}) {
              System.out.println("depth " + depth + " -> severity " + SeverityRoller.roll(depth,
            rng));
          }

          // 3. rollDefault sanity check — should behave like the Levels 4-6 band.
          System.out.println("rollDefault -> " + SeverityRoller.rollDefault(rng));
      }
      */
    
    private SeverityRoller() {}
    
    // percentage weight spread for each severity for each depth level
    private static final Map<Integer, Integer> LEVELS_1_TO_3 = band(40, 28, 18, 9, 4, 1);
    private static final Map<Integer, Integer> LEVELS_4_TO_6 = band(28, 26, 22, 14, 7, 3);
    private static final Map<Integer, Integer> LEVELS_7_TO_9 = band(18, 22, 24, 19, 11, 6);
    private static final Map<Integer, Integer> LEVELS_10_PLUS = band(10, 16, 22, 24, 18, 10);
    
    // puts them all to a map
    private static Map<Integer, Integer> band(int s1, int s2, int s3, int s4, int s5, int s6) {
        Map<Integer, Integer> weights = new LinkedHashMap<>();
        weights.put(1, s1);
        weights.put(2, s2);
        weights.put(3, s3);
        weights.put(4, s4);
        weights.put(5, s5);
        weights.put(6, s6);
        return weights;
    }
    
    // rolls 1 to 6, weighted by whichever depth band this drill level falls into
    public static int roll(int depth, Random rng) {
        return WeightedRoller.roll(bandFor(depth), rng);
    }
    
    // fixed levels 4 to 6 band, used for ascent and jump hazards, which do not have a 
    // depth of their own
    public static int rollDefault(Random rng) {
        return WeightedRoller.roll(LEVELS_4_TO_6, rng);
    }
    
    private static Map<Integer, Integer> bandFor(int depth) {
        if (depth <= 3) return LEVELS_1_TO_3;
        if (depth <= 6) return LEVELS_4_TO_6;
        if (depth <= 9) return LEVELS_7_TO_9;
        return LEVELS_10_PLUS;
    }
}
