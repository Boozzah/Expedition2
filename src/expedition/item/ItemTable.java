/*
 * ItemTable
 * Every item that can be found, with its drop weight. Picks a random item
 * by weight, so a Repair Kit (25) turns up about three times as often as a
 * White Monster (8). Crew with an ItemFinder ability can change the weights.
 */
package expedition.item;

import expedition.crew.hooks.ItemFinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ItemTable {

    private final List<ItemDefinition> definitions;

    public ItemTable(List<ItemDefinition> definitions) {
        this.definitions = new ArrayList<>(definitions);
    }

    // for hazards that just say "+1 random item", where nobody's ability applies
    public Item roll(Random rng) {
        return roll(rng, List.of());
    }

    // finders are the crew on the expedition whose ability changes the weights, e.g. the
    // Engineer doubling Repair Kits. Must be the run's seeded rng so a replay finds the same items.
    // returns null if every weight is 0, so there is nothing to find
    public Item roll(Random rng, List<ItemFinder> finders) {

        // work out each item's weight once, with every finder's change applied
        int[] weights = new int[definitions.size()];
        int total = 0;
        for (int i = 0; i < definitions.size(); i++) {
            ItemDefinition d = definitions.get(i);
            int weight = d.dropWeight();
            for (ItemFinder finder : finders) {
                weight = finder.modifyDropWeight(d.name(), weight);
            }
            weights[i] = weight;
            total += weight;
        }

        if (total <= 0) {
            return null;
        }

        // pick a number below the total and walk along the list until the
        // running weight passes it - bigger weights take up more of the range
        int pick = rng.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0) {
                return ItemFactory.create(definitions.get(i));
            }
        }
        return null; // never reached, pick is always below total
    }

    // builds the item with this name, for putting a saved inventory back together.
    // returns null if no item has that name any more
    public Item create(String name) {
        for (ItemDefinition d : definitions) {
            if (d.name().equals(name)) {
                return ItemFactory.create(d);
            }
        }
        return null;
    }

    public List<ItemDefinition> definitions() {
        return Collections.unmodifiableList(definitions);
    }
}
