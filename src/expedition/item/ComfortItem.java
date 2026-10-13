/*
 * ComfortItem
 * Item. Restores a crew member's composure (Sensory Toy).
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember;

public class ComfortItem extends Item
{
    private final int composureAmount;

    public ComfortItem(String name, String description, int composureAmount)
    {
        super(name, description);
        this.composureAmount = composureAmount;
    }

    @Override
    public void use(EffectContext ctx, CrewMember target)
    {
        target.gainComposure(composureAmount);
    }

    @Override
    public boolean needsCrewTarget()
    {
        return true;
    }
}
