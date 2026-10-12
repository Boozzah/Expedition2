/*
 * RepairKit
 * Item. Repairs hull percentage; does not need a crew target.
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember;

public class RepairKit extends Item
{
    private final int hullAmount;

    public RepairKit(String name, String description, int hullAmount)
    {
        super(name, description);
        this.hullAmount = hullAmount;
    }

    // target is ignored, it fixes the ship not a person. repairHull stops at 100
    @Override
    public void use(EffectContext ctx, CrewMember target)
    {
        ctx.ship().repairHull(hullAmount);
    }

    @Override
    public boolean needsCrewTarget()
    {
        return false;
    }
}
