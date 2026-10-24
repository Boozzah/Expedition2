/*
 * CrewStatEffect
 * Effect for code STAT:<stat>:<delta>:<target>:<count> — applies a
 * health/stamina/composure change to one or more chosen crew
 * members. A crew member whose health reaches 0 dies.
 */
package expedition.hazard.effect;

import expedition.crew.CrewMember;
import java.util.List;

public record CrewStatEffect(Stat stat, int amount, Target target, int count) implements Effect {
    
    public enum Stat { 
        HEALTH, STAMINA, COMPOSURE 
    }
    
    public enum Target {
        RANDOM,
        ALL
    }
    
    @Override
    public void apply(EffectContext ctx) {
        
        List<CrewMember> victims;

        // team is null for jump events - those happen in flight, with nobody
        // on a surface, so they hit whoever is aboard instead.
        if (ctx.team() == null) {
            if (target == Target.ALL) {
                victims = ctx.state().livingCrew();
            } else {
                victims = ctx.state().randomLivingCrew(count);
            }

        // ALL means everyone on THIS expedition, not the whole crew - people
        // asleep on the ship are not hurt by a cave-in they weren't at.
        // (Witnessing a death does cost the whole crew composure, but that is
        // handled separately in RunState.killCrew - GDD 3.2.)
        } else if (target == Target.ALL) {
            victims = ctx.team().livingMembers();

        } else {
            victims = ctx.team().randomMembers(count, ctx.state().rng());
        }
        
        for (CrewMember member : victims) {
            applyTo(member);

            // 0 health is a death. Goes through killCrew like the DEATH effect does,
            // so the rest of the crew still lose composure for witnessing it
            // AI assisted - Claude Code, 0 health killing was our decision and it wrote the fix through killCrew
            if (member.isAlive() && member.getHealth() == 0) {
                ctx.state().killCrew(member);
            }
        }
    }
    
    // modifier of stats to a given crew member
    private void applyTo(CrewMember member) {
        switch(stat) {
            case HEALTH: 
                if (amount < 0 ) {
                    member.loseHealth(-amount);
                } else {
                    member.gainHealth(amount);
                } break;
            
            case STAMINA:
                if (amount < 0) {
                    member.loseStamina(-amount);
                } else {
                    member.gainStamina(amount);
                } break;
            
            case COMPOSURE:
                if (amount < 0) {
                    member.loseComposure(-amount);
                } else {
                    member.gainComposure(amount);
                } break;
        }
    }
    
}
