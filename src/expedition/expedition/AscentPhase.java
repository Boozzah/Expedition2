/*
 * AscentPhase
 * The climb back to the ship once the team has finished drilling on a planet.
 * Rolls whether an ascent hazard fires, lets a Pilot strip the hull damage out
 * of it, then applies what's left. The oil is still on the shuttle during the
 * climb, so this holds it until they dock and some ascent hazards can take it.
 */
package expedition.expedition;

import expedition.crew.hooks.AscentModifier;
import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.hazard.Hazard;
import expedition.hazard.HazardOutcome;
import expedition.hazard.SeverityRoller;
import expedition.hazard.effect.EffectContext;
import expedition.ui.GameView;
import expedition.ui.screen.Popup;
import expedition.world.Planet;

import java.util.List;

// AI assisted - Claude Code helped write the ascent phase, we reviewed the diff and reformatted the constructor
public class AscentPhase implements HaulHolder {

    private final Expedition team;
    private final RunState state;
    private final GameView view;
    private final GameConfig config;
    private final List<Hazard> hazards;
    private int oilAboard;

    public AscentPhase(Expedition team, RunState state, GameView view, GameConfig config, List<Hazard> hazards, int oilAboard) {
        this.team = team;
        this.state = state;
        this.view = view;
        this.config = config;
        this.hazards = hazards;
        this.oilAboard = oilAboard;
    }

    // Flies the team back up. Returns how much oil made it to the ship
    public int run(Planet planet) {
        view.print("");
        view.print("The shuttle lifts off and climbs back to the ship.");

        // GDD 8: 20% chance, plus 8% for every extra site drilled on this planet,
        // because the shuttle and the crew are more worn every time
        int extraTrips = Math.max(0, planet.expeditionCount() - 1);
        int chance = config.ascentTriggerBase() + config.ascentTriggerPerExtraTrip() * extraTrips;

        // same roll as a drill hazard, if the random number is over the chance nothing happens
        if (hazards.isEmpty() || state.rng().nextDouble() >= chance / 100.0) {
            view.print("A smooth climb. The shuttle docks without any trouble.");
            return oilAboard;
        }

        Hazard hazard = hazards.get(state.rng().nextInt(hazards.size()));

        // the ascent has no depth of its own, so it uses the fixed levels 4-6 band
        int severity = SeverityRoller.rollDefault(state.rng());
        HazardOutcome outcome = hazard.outcomeFor(severity);

        // the story first, then what the Pilot saved, then the damage
        HazardReport report = new HazardReport(state);
        String text = outcome.text();
        outcome = pilotMitigates(outcome, report);

        int oilBefore = oilAboard;
        outcome.applyAll(new EffectContext(state, team, this));

        report.addCrewChanges(team);
        report.addShipChanges(oilAboard - oilBefore);
        view.showPopup(new Popup(Popup.Kind.HAZARD, hazard.name(), text, report.lines()));

        return oilAboard;
    }

    // The Pilot's ability. Every pilot on the shuttle gets to change the outcome, and the
    // player is told when they've saved the hull so it reads as the ability working
    private HazardOutcome pilotMitigates(HazardOutcome outcome, HazardReport report) {
        for (AscentModifier pilot : team.ascentModifiers()) {
            HazardOutcome modified = pilot.modifyAscent(outcome);

            // fewer effects means the pilot stripped the hull damage out
            if (modified.effects().size() < outcome.effects().size()) {
                report.add(pilot.getName() + " (Pilot) flies them through it: the ship takes no damage.");
            }
            outcome = modified;
        }
        return outcome;
    }

    // some ascent hazards lose oil, it comes out of what the shuttle is carrying
    @Override
    public void reduceHaulByPercent(int percent) {
        oilAboard = oilAboard - (oilAboard * percent / 100);
    }

    // there is no site to close on the way up, the team has already left it
    @Override
    public void closeSiteAndEnd() {
    }
}
