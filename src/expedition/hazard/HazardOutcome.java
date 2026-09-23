package expedition.hazard;
import expedition.hazard.effect.Effect;
import java.util.List;
import expedition.hazard.effect.EffectContext;

/*
 * HazardOutcome
 * One row of a hazard table — the severity level, the text shown to the
 * player, and the list of Effects that fire.
 *
 * Deliberately IMMUTABLE (a record). The Pilot's ability strips hull
 * damage from an ascent outcome, if this were mutable, that would
 * permanently delete hull damage from the shared copy loaded from file,
 * for the rest of the run. Very hard bug to find.
 */

public record HazardOutcome(int severity, String text, List<Effect> effects) 
{
    public void applyAll(EffectContext ctx)
    {
        for (Effect e : effects)
        {
            e.apply(ctx);
        }
    }
}
