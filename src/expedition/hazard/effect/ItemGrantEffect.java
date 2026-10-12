/*
 * ItemGrantEffect
 * Effect for code ITEM:+n / ITEM:-n — grants or destroys random
 * items from the ship's inventory.
 */
package expedition.hazard.effect;

import expedition.item.Item;
import expedition.ship.Inventory;
import java.util.Random;

public record ItemGrantEffect(int count) implements Effect {
    
    @Override
    public void apply(EffectContext ctx) {
        Inventory bag = ctx.ship().inventory();
        // the run's seeded rng, never new Random(), so a replayed seed gets the same items
        Random rng = ctx.state().rng();
        
        // positive adds, negative removes
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                Item item = ctx.state().itemTable().roll(rng);
                // effects can't ask the player anything, so if the bag is full the item is just left behind
                if (item != null) {
                    bag.add(item);
                }
            }
        } else {
            // removeRandom does nothing once the bag is empty, so ITEM:-3 with 1 item just loses that one
            for (int i = 0; i < -count; i++) {
                bag.removeRandom(rng);
            }
        }
    }
    
}
