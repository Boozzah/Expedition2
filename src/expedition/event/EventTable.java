/*
 * EventTable
 * Every jump event that can happen, and the roll for whether one does. Each event's
 * weight is how often it comes up compared to the others, picked with WeightedRoller
 * the same way items are.
 */
package expedition.event;

import expedition.util.WeightedRoller;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EventTable {

    private final List<JumpEvent> events;
    // percent chance of an event on any one jump
    private final int chance;

    public EventTable(List<JumpEvent> events, int chance) {
        this.events = List.copyOf(events);
        this.chance = chance;
    }

    // The event for this jump, or null for a quiet one. Uses the run's seeded random,
    // so a seeded run gets the same events every time
    public JumpEvent roll(Random rng) {
        if (events.isEmpty() || rng.nextInt(100) >= chance) {
            return null;
        }

        // LinkedHashMap keeps the events in file order, which a seeded run's pick depends on.
        // Events with a weight of 0 never come up, and are left out
        Map<JumpEvent, Integer> weights = new LinkedHashMap<>();
        for (JumpEvent e : events) {
            if (e.weight() > 0) {
                weights.put(e, e.weight());
            }
        }
        if (weights.isEmpty()) {
            return null;
        }
        return WeightedRoller.roll(weights, rng);
    }
}
