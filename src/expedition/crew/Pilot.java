/*
 * Pilot
 * Crew role. Implements AscentModifier — strips the hull-damage
 * effects from a rolled ascent hazard outcome. Crew injury, fuel loss,
 * oil loss and death still happen (GDD 8).
 */
package expedition.crew;

import expedition.crew.hooks.AscentModifier;
import expedition.hazard.HazardOutcome;
import expedition.hazard.effect.Effect;
import expedition.hazard.effect.HullEffect;

import java.util.ArrayList;
import java.util.List;

// AI assisted - Claude Code helped write the Pilot ability as part of the ascent phase
public class Pilot extends CrewMember implements AscentModifier{
    public Pilot(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }
    
    @Override
    public HazardOutcome modifyAscent(HazardOutcome base) {
        // Build a new list and new HazardOutcome rather than changing the base effect.
        // HazardOutcome is immutable on purpose - the shared HazardOutcome object is loaded once,
        // from file and is reused every run. Editing it here would strip hull damage from that hazard for the rest of the game.
        List<Effect> filtered = new ArrayList<>();
        for(Effect effect : base.effects()) {
            if (!(effect instanceof HullEffect)) {
                filtered.add(effect);
            }
        }
        return new HazardOutcome(base.severity(), base.text(), filtered, base.effectCodes());
    }
    
    @Override
    public Role getRole() {
        return Role.PILOT;
    }
}
