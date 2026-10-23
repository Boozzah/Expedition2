/*
 * JumpEvent
 * Something that happens between planets: the text the player reads, the effects it has
 * and the points it is worth. Events can be good, bad or just flavour with nothing attached.
 * The effects are the same Effect objects hazards use, built by EffectParser.
 *
 * A record so it can't change once loaded, the same as HazardOutcome.
 */
package expedition.event;

import expedition.hazard.effect.Effect;
import expedition.hazard.effect.EffectContext;
import java.util.List;

public record JumpEvent(String text, List<Effect> effects, String effectCodes, int points, int weight) {

    public JumpEvent {
        effects = List.copyOf(effects);
    }

    public void applyAll(EffectContext ctx) {
        for (Effect e : effects) {
            e.apply(ctx);
        }
    }
}
