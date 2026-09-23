/*
 * Hazard
 * One named hazard (e.g. "Cave Collapse") loaded from file: which
 * hazard set it belongs to and its HazardOutcome for each severity
 * level 1-6.
 */
package expedition.hazard;

import java.util.Map;

public record Hazard(String name, HazardSet set, Map<Integer, HazardOutcome> outcomes) {
        
    // keeps its own copy so the loader can't change it later
    public Hazard {
        outcomes = Map.copyOf(outcomes);
    }
    
    
    // returns the hazard outcome for a particular severity
    public HazardOutcome outcomeFor(int severity) {
        HazardOutcome outcome = outcomes.get(severity);
        
        // for debugging, if the hazards.txt file is written wrong it will return this.
        if (outcome == null) {
            throw new IllegalArgumentException(name + " has no outcome for severity " + severity);
        }
        return outcome;
    }
}
