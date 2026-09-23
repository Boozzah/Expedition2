/*
 * HullEffect
 * damages or repairs hull
*/
package expedition.hazard.effect;

import expedition.ship.Ship;

public record HullEffect(int amount) implements Effect {
    
    @Override
    public void apply(EffectContext ctx) {
        Ship ship = ctx.state().ship();
        
        // if the amount is negative then it creates hull damage 
        if (amount < 0) {
            ship.damageHull(-amount);
        } else {
            // otherwise you can repair the hull 
            ship.repairHull(amount);
        }
    }
}
