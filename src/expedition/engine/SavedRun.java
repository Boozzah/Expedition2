/*
 * SavedRun
 * A snapshot of a run in progress, taken when the ship arrives at a planet. It only
 * holds plain values (numbers and names) so the database can store it without
 * knowing anything about RunState, Ship or the crew classes.
 *
 * rngSeed is what the run's Random was reseeded with at the moment of the save, so a
 * resumed run rolls exactly the same numbers it would have if the player never quit.
 * Lists are kept in their original order, because the order changes what the seeded
 * random picks (random crew member, random item lost).
*/
package expedition.engine;

import expedition.crew.Role;
import java.util.List;

public record SavedRun(long seed, long rngSeed, int planetIndex, int totalOilCollected,
                       int fuel, int hullPercent, List<String> route, List<String> items,
                       List<SavedCrew> crew) {

    // one crew member, dead ones included so the ending can still list them as lost
    public record SavedCrew(String name, Role role, int health, int stamina, int composure, boolean alive) {
    }

    // copies the lists so the snapshot can't change after it is taken
    public SavedRun {
        route = List.copyOf(route);
        items = List.copyOf(items);
        crew = List.copyOf(crew);
    }
}
