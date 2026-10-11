/*
 * CrewDeathEffect
 * Effect for code DEATH:n — kills a crew member via
 * RunState.killCrew, which also applies the composure penalty to the
 * rest of the crew.
 */
package expedition.hazard.effect;

import expedition.crew.CrewMember;
import java.util.List;

public record CrewDeathEffect(int count) implements Effect {
    
    @Override
    public void apply(EffectContext ctx) {
        List<CrewMember> victims;

        // team is null for jump events - nobody is on a surface, so it can be anyone aboard
        if (ctx.team() == null) {
            victims = ctx.state().randomLivingCrew(count);
        } else {
            // otherwise only someone who was actually there. Before this, a cave-in or a shuttle
            // crash could kill a crew member who was resting on the ship the whole time
            victims = ctx.team().randomMembers(count, ctx.state().rng());
        }
        
        // loops through the victims and marks them dead as well as gives the composure penalty to the survivors
        for (CrewMember member : victims) {
            ctx.state().killCrew(member);
        }
    }
}
