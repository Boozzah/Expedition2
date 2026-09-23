package expedition.engine;

/*
 * Ending
 * Immutable summary of how a run ended: reason, planets reached,
 * survivor/dead crew names and total oil collected. Pure data plus a boolean method.
 */

import java.util.List; 

public record Ending(EndingReason reason,
                     int planetsReached, 
                     List<String> survivorNames,
                     List<String> deadNames,
                     int totalOilCollected) { 
    
    // AI assisted fix for an error we came across toward the end
    // Only a run that reaches home has survivors. Every other ending kills whoever
    // was still alive - adrift, hull failure, and stranded all end with crew dead.
    public boolean anyoneSurvived() {
        return reason == EndingReason.HOME;
    }
}


