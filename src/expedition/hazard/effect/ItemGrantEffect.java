/*
 * ItemGrantEffect
 * Effect for code ITEM:+n / ITEM:-n — grants or destroys random
 * items from the ship's inventory. - Items to be implemented in project 2
 */
package expedition.hazard.effect;

public record ItemGrantEffect(int count) implements Effect {
    
    @Override
    public void apply(EffectContext ctx) {
        // TODO: add or remove count number of random items once Inventory and Item exist.
        // Negative counts remove, positive adds. Needs the single seeded RNG from ctx.state().rng()
        // to pick which items, never new Random().
    }
    
}
