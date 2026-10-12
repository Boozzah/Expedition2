/*
 * MedKit
 * Item. Restores a crew member's health (First Aid Kit).
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember;

public class MedKit extends Item
{
    private final int healthAmount;

    public MedKit(String name, String description, int healthAmount)
    {
        super(name, description);
        this.healthAmount = healthAmount;
    }

    // gainHealth already stops at the max so a full health crew member just wastes it
    @Override
    public void use(EffectContext ctx, CrewMember target)
    {
        target.gainHealth(healthAmount);
    }

    @Override
    public boolean needsCrewTarget()
    {
        return true;
    }
}
