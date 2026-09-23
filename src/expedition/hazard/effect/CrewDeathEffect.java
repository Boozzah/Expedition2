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
        // randomly choosing any crew members as the victim (they have to be living) 
        List<CrewMember> victims = ctx.state().randomLivingCrew(count);
        
        // loops through the victims and marks them dead as well as gives the composure penalty to the survivors
        for (CrewMember member : victims) {
            ctx.state().killCrew(member);
        }
    }
}
