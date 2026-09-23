/*
 * FuelEffect
 * Effect for code FUEL: Spends and adds fuel
 */
package expedition.hazard.effect;

import expedition.ship.Ship;

// AI corrected the bugs in this code and we wrote what was recommended so we could understand it 
public record FuelEffect(int amount) implements Effect{
    
    @Override
    public void apply(EffectContext ctx) {
        // just so during the method you can just call ship instead of the long version and you'll get the same result
        Ship ship = ctx.state().ship();
        
        // checks if the amount is negative then that is saying that the hazard or event is going to cost fuel
        if (amount < 0) {
            // it then gets catagorized as a spending of fuel
            ship.spendFuel(- amount);
        }
        else {
            // and if thats not the case and the amount was positive it calls add fuel
            ship.addFuel(amount);
        }
    }
}
