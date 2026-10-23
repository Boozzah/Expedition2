/*
 * HullEffect
 * damages or repairs the ship. The code is still HULL in hazards.txt, but the damage now
 * lands on one of the five ship parts, picked by the run's seeded random so a seeded
 * run always hits the same part
*/
package expedition.hazard.effect;

import expedition.ship.Ship;
import expedition.ship.ShipPart;

public record HullEffect(int amount) implements Effect {

    @Override
    public void apply(EffectContext ctx) {
        Ship ship = ctx.state().ship();

        // if the amount is negative then it damages a random part
        if (amount < 0) {
            ShipPart[] parts = ShipPart.values();
            ShipPart hit = parts[ctx.state().rng().nextInt(parts.length)];
            ship.damage(hit, -amount);
        } else {
            // otherwise it repairs whichever part is in the worst shape
            ship.repair(ship.mostDamagedPart(), amount);
        }
    }
}
