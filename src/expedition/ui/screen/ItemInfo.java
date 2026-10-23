/*
 * ItemInfo
 * One kind of item in the hold, as a screen shows it: 3 Caffeine is one ItemInfo with a
 * count of 3. needsCrewTarget says whether using it asks who to give it to.
 */
package expedition.ui.screen;

import expedition.item.Item;
import expedition.ship.Inventory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ItemInfo(String name, String description, int count, boolean needsCrewTarget) {

    // the hold grouped by name, in the order the items were picked up
    public static List<ItemInfo> of(Inventory bag) {
        List<ItemInfo> items = new ArrayList<>();
        for (Map.Entry<String, Integer> stack : bag.stacked().entrySet()) {
            Item first = null;
            for (Item item : bag.items()) {
                if (item.name().equals(stack.getKey())) {
                    first = item;
                    break;
                }
            }
            items.add(new ItemInfo(stack.getKey(), first.description(), stack.getValue(), first.needsCrewTarget()));
        }
        return items;
    }
}
