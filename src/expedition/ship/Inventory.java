/*
 * Inventory
 * Fixed-capacity bag of Items carried aboard the ship. Supports
 * add/remove and a stacked name -> count view for display.
 */
package expedition.ship;

import expedition.item.Item;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Inventory
{
    private final int capacity;
    private final List<Item> items = new ArrayList<>();

    public Inventory(int capacity)
    {
        this.capacity = capacity;
    }

    // returns false when the bag is full so whoever found the item can ask the player
    // what to throw away instead (GDD 10 - finding a 9th item is a discard choice)
    public boolean add(Item item)
    {
        if (isFull())
        {
            return false;
        }
        items.add(item);
        return true;
    }

    // used for the discard choice, takes out that exact item
    public boolean remove(Item item)
    {
        return items.remove(item);
    }

    // takes out one item with this name, for when the player picks a stack to use from.
    // returns null if there isnt one so the caller can tell the player
    public Item take(String name)
    {
        for (Item item : items)
        {
            if (item.name().equals(name))
            {
                items.remove(item);
                return item;
            }
        }
        return null;
    }

    // hazards like ITEM:-2 destroy random items. Uses the run's rng so a seeded run
    // always loses the same items. returns null if the bag is already empty
    public Item removeRandom(Random rng)
    {
        if (items.isEmpty())
        {
            return null;
        }
        return items.remove(rng.nextInt(items.size()));
    }

    // groups the items by name for the screen, e.g. "Caffeine x2" instead of listing it twice.
    // LinkedHashMap keeps them in the order they were picked up
    public Map<String, Integer> stacked()
    {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Item item : items)
        {
            counts.merge(item.name(), 1, Integer::sum);
        }
        return counts;
    }

    // read only so nothing outside can skip the capacity check by adding to the list directly
    public List<Item> items()
    {
        return Collections.unmodifiableList(items);
    }

    public int size()
    {
        return items.size();
    }

    public int capacity()
    {
        return capacity;
    }

    public boolean isFull()
    {
        return items.size() >= capacity;
    }

    public boolean isEmpty()
    {
        return items.isEmpty();
    }
}
