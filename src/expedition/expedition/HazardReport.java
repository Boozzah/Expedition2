/*
 * HazardReport
 * Tells the player what a hazard just did. The effects themselves are silent,
 * they just change numbers, so the player would otherwise read the story and
 * find the damage later on. Takes a snapshot of everything an effect can touch
 * when it's made, then compares against it once the effects have fired.
 * Used by both DrillSession and AscentPhase so the damage reads the same in both.
 */
package expedition.expedition;

import expedition.crew.CrewMember;
import expedition.crew.Role;
import expedition.engine.RunState;
import expedition.ship.ShipPart;
import expedition.ui.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HazardReport {

    private final RunState state;
    private final GameView view;

    // everything that an effect can touch, as it was before the hazard
    private final int[] health;
    private final int[] stamina;
    private final int[] composure;
    private final boolean[] alive;
    private final int fuelBefore;
    private final Map<ShipPart, Integer> partsBefore;
    private final Map<String, Integer> itemsBefore;

    // make this right before outcome.applyAll() so the snapshot is the "before"
    public HazardReport(RunState state, GameView view) {
        this.state = state;
        this.view = view;

        List<CrewMember> crew = state.crew();
        int n = crew.size();
        health = new int[n];
        stamina = new int[n];
        composure = new int[n];
        alive = new boolean[n];
        for (int i = 0; i < n; i++) {
            CrewMember m = crew.get(i);
            health[i] = m.getHealth();
            stamina[i] = m.getStamina();
            composure[i] = m.getComposure();
            alive[i] = m.isAlive();
        }
        fuelBefore = state.ship().fuel();
        partsBefore = state.ship().parts();
        // stacked() already makes a new map, so it won't change when the inventory does
        itemsBefore = state.ship().inventory().stacked();
    }

    // Prints a line for each crew member who changed. Returns the members of the team
    // who survived but lost health, so the Doctor knows who to treat. team is null for a
    // jump event, everyone is aboard the ship and there is no Doctor on a team to treat them
    public List<CrewMember> printCrewChanges(Expedition team) {
        List<CrewMember> crew = state.crew();
        List<CrewMember> injured = new ArrayList<>();
        List<CrewMember> onTeam = (team == null) ? List.of() : team.livingMembers();

        for (int i = 0; i < crew.size(); i++) {
            CrewMember m = crew.get(i);

            if (alive[i] && !m.isAlive()) {
                view.print("  " + m.getName() + " is killed.");

                // GDD 3.1: losing the Pilot is said plainly, so the player feels the run
                // get harder now rather than finding out on a later ascent
                if (m.getRole() == Role.PILOT) {
                    view.print("  Ascent risk is now unmitigated.");
                }
                continue;   // their stat losses are not the story here
            }

            String changes = "";
            changes += change(changes, "health", m.getHealth() - health[i]);
            changes += change(changes, "stamina", m.getStamina() - stamina[i]);
            changes += change(changes, "composure", m.getComposure() - composure[i]);

            if (!changes.isEmpty()) {
                view.print("  " + m.getName() + ": " + changes);
            }

            // lost health to this hazard and is still with the team
            if (m.getHealth() < health[i] && onTeam.contains(m)) {
                injured.add(m);
            }
        }
        return injured;
    }

    // Prints one line for the oil, fuel and any ship part that was hit. The oil change is passed in because
    // the oil is held by whoever is carrying it, not by the ship
    public void printShipChanges(int oilChange) {
        String ship = "";
        ship += change(ship, "oil", oilChange);
        ship += change(ship, "fuel", state.ship().fuel() - fuelBefore);
        for (ShipPart part : ShipPart.values()) {
            ship += change(ship, part.label().toLowerCase(), state.ship().part(part) - partsBefore.get(part));
        }

        if (!ship.isEmpty()) {
            view.print("  " + ship);
        }
        printItemChanges();
    }

    // compares the inventory by name, so it reads "found: Caffeine" rather than
    // the player having to open the inventory to notice
    private void printItemChanges() {
        Map<String, Integer> itemsAfter = state.ship().inventory().stacked();
        String found = "";
        String lost = "";

        for (Map.Entry<String, Integer> e : itemsAfter.entrySet()) {
            int gained = e.getValue() - itemsBefore.getOrDefault(e.getKey(), 0);
            if (gained > 0) {
                found += itemName(found, e.getKey(), gained);
            }
        }
        for (Map.Entry<String, Integer> e : itemsBefore.entrySet()) {
            int gone = e.getValue() - itemsAfter.getOrDefault(e.getKey(), 0);
            if (gone > 0) {
                lost += itemName(lost, e.getKey(), gone);
            }
        }

        if (!found.isEmpty()) {
            view.print("  found: " + found);
        }
        if (!lost.isEmpty()) {
            view.print("  lost: " + lost);
        }
    }

    // "Caffeine" for one, "Caffeine x2" for more, with commas between
    private String itemName(String soFar, String name, int count) {
        String sep = soFar.isEmpty() ? "" : ", ";
        return sep + name + (count > 1 ? " x" + count : "");
    }

    // changes numbers into readable text, and handles commas between items
    private String change(String soFar, String label, int change) {
        if (change == 0) {
            return "";
        }
        String sep = soFar.isEmpty() ? "" : ", ";
        String sign = (change > 0) ? "+" : "";
        return sep + label + " " + sign + change;
    }
}
