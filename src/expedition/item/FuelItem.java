/*
 * FuelItem
 * Item. Adds fuel to the ship — e.g. Condensed Oil, which ignores
 * the tank cap.
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember;

public class FuelItem extends Item
{
    private final int fuelAmount;

    public FuelItem(String name, String description, int fuelAmount)
    {
        super(name, description);
        this.fuelAmount = fuelAmount;
    }

    // ignores the cap so using it with a nearly full tank isnt a waste (GDD 10)
    @Override
    public void use(EffectContext ctx, CrewMember target)
    {
        ctx.ship().addFuelIgnoringCap(fuelAmount);
    }

    @Override
    public boolean needsCrewTarget()
    {
        return false;
    }
}
