/*
 * RepairKit
 * Item. Repairs a ship part; does not need a crew target. From the item menu it fixes
 * the part in the worst shape, the ship health screen lets the player pick the part.
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember;
import expedition.ship.Ship;
import expedition.ship.ShipPart;

public class RepairKit extends Item
{
    private final int hullAmount;

    public RepairKit(String name, String description, int hullAmount)
    {
        super(name, description);
        this.hullAmount = hullAmount;
    }

    // target is ignored, it fixes the ship not a person
    @Override
    public void use(EffectContext ctx, CrewMember target)
    {
        repair(ctx.ship(), ctx.ship().mostDamagedPart());
    }

    // fixes the part the player chose on the ship screen. Returns how much it went up,
    // repair stops at 100 so a nearly full part takes less than the whole kit
    public int repair(Ship ship, ShipPart part)
    {
        return ship.repair(part, hullAmount);
    }

    @Override
    public boolean needsCrewTarget()
    {
        return false;
    }
}
