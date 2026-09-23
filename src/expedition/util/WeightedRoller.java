/*
 * WeightedRoller
 * Generic weighted-random pick from a Map<T,Integer> of weights.
 * Reused for severity, hazard selection, jump events, item drops and
 * site tiers.
 */
package expedition.util;

import java.util.Map;
import java.util.Random;

public final class WeightedRoller {
    
    private WeightedRoller() {}
    
    // Generic lets one method handle all cases that need a weighted roller
    // takes Map of options and their weight, and shared Random object
    public static <T> T roll(Map<T, Integer> weights, Random rng) {
        int total = 0;
        
        // sum all the weights
        for (int weight : weights.values()) {
            total += weight;
        }
        // e.g for item drops Repair Kit 25, Caffeine 20, First Aid kit 20, Sensory Toy 15,
        // Condensed Oil 12, White Monster 8. Total = 100
        
        // choose a random number from the total weights
        // e.g 0 - 99 for this example (inclusive)
        int pick = rng.nextInt(total);
        
        
        // walk all the entries until the random number lands between the last entry and the upper bound
        // of the next entry
        int cumulative = 0;
        for (Map.Entry<T, Integer> entry: weights.entrySet()) {
            cumulative += entry.getValue();
            if (pick < cumulative) {
                return entry.getKey();
            }
        }
        
        // Unreachable if every weight is positive and the map is not empty
        throw new IllegalStateException("WeightedRoller: weights summed to " + total + " but no entry was picked");
    }
}
