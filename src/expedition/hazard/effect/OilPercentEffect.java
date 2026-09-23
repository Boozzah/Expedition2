/*
 * OilPercentEffect
 * Effect for code OIL_PCT:-n — reduces the current drill session's
 * haul by a percentage. No-op when there is no active session (e.g.
 * a jump event) — guard the null session once, here.
 */
package expedition.hazard.effect;

public record OilPercentEffect(int percent) implements Effect {
    
    @Override
    public void apply(EffectContext ctx) {
        // checks if theres an active drill session, if their isnt nopthing happens
        if(ctx.session() == null) {
        }
        // but if there is you apply the percentage reduction 
        else 
        {
            ctx.session().reduceHaulByPercent(-percent); 
        }
    }
}
