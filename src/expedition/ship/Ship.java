package expedition.ship;

/*
 * Ship
 * Fuel, tank capacity, the health of each ship part, costs/spend, and the inventory
 * of items carried aboard.
 * The hull percentage the fuel cost uses is the average of the five parts, so a ship
 * with one badly damaged part costs a bit more to fly, and one with every part worn
 * down costs a lot more. The ship breaks when any one part gets to 0.
 */
import expedition.engine.GameConfig;
import java.util.EnumMap;
import java.util.Map;

public class Ship 
{
    // a part can only be between 0 and 100 health
    private static final int PART_MAX = 100;

    private int fuel; 
    private final int tankCapacity; 
    private final Map<ShipPart, Integer> parts = new EnumMap<>(ShipPart.class); 
    private final Inventory inventory; 
    
    public Ship(GameConfig c)
    {
        // takes value from GameConfig
        tankCapacity = c.tankCapacity(); 
        // The ship will always start with every part at 100% health 
        for (ShipPart part : ShipPart.values())
        {
            parts.put(part, PART_MAX);
        }
        // fuel is set in GameConfig 
        fuel = c.startingFuel();  
        // starts empty, items are found while drilling or given by hazards
        inventory = new Inventory(c.inventorySlots());
    }

    // for resuming a saved run, the fuel and the parts carry on from where they were.
    // the items are added to the inventory afterwards
    public Ship(GameConfig c, int fuel, Map<ShipPart, Integer> savedParts)
    {
        this(c);
        this.fuel = fuel;
        parts.putAll(savedParts);
    }

    public int fuel()
    {
        return fuel; 
    }
    
    // the average of the five parts, used for the fuel cost of a jump
    public int hullPercent()
    {
        int total = 0;
        for (int health : parts.values())
        {
            total += health;
        }
        return total / parts.size(); 
    }
    
    public int part(ShipPart part)
    {
        return parts.get(part);
    }
    
    // a copy, so the screens and the save can read every part without changing them
    public Map<ShipPart, Integer> parts()
    {
        return new EnumMap<>(parts);
    }
    
    // the ship can't fly once any part is gone, see GameEngine.checkTerminalStates
    public boolean isBroken()
    {
        return parts.containsValue(0);
    }
    
    // the part a Repair Kit goes to when the player doesn't pick one. On a tie the first
    // part in the list wins, so it is always the same part for the same damage
    public ShipPart mostDamagedPart()
    {
        ShipPart worst = ShipPart.HULL;
        for (ShipPart part : ShipPart.values())
        {
            if (parts.get(part) < parts.get(worst))
            {
                worst = part;
            }
        }
        return worst;
    }
    
    public Inventory inventory()
    {
        return inventory; 
    }
    
    // you want the add fuel to stop at the limit and waste anythig extra
    // also tell the player how much was added in this collection as maybe if it was alot and you had only a little space it would be wasting fuel
    
    public int addFuel(int amount)
    {
        // remembers the original fuel amount so that you can compare after 
        int before = fuel; 
        // adds the fuel but no more than tankCapacity
        fuel = Math.min(fuel + amount, tankCapacity); 
        // fuel added - what the fuel was at calculates what the amount that was actually put into the tank 
        int actuallyAdded = fuel - before; 
        return actuallyAdded; 
    }
    
    // this is specifically for special parts of the game where you get something like condensed oil, and it is allowed to go over the max tank capacity 
    public void addFuelIgnoringCap(int amount)
    {
        fuel += amount; 
    }
    
    // can the ship pay the cost in fuel? 
    public boolean canAfford(int cost)
    {
        return fuel >= cost; 
    }
    
    // this takes a way the amount it cost in fuel for the jump, we already found out if you can afford it but the minimum being 0 is necessary just in case because you
    // dont want to make it minus money when you just shouldnt be able to afford it anyway 
    public void spendFuel(int cost)
    {
        fuel = Math.max(0, fuel - cost); 
    }
    
    // takes away some of a part's 100% health when it gets damaged but it cant go below 0 as a part can only be between 0 and 100 health.  
    public void damage(ShipPart part, int damage)
    {
        parts.put(part, Math.max(0, parts.get(part) - damage)); 
    }
    
    // adds to the percentage of a part's health as a repair, and returns how much it
    // actually went up, as a part that is nearly full can't take the whole repair
    public int repair(ShipPart part, int repair)
    {
        int before = parts.get(part);
        parts.put(part, Math.min(PART_MAX, before + repair)); 
        return parts.get(part) - before;
    }
}
