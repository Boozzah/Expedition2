package expedition.ship;

/*
 * Ship
 * Fuel, tank capacity, hull percentage, costs/spend.
 */
import expedition.engine.GameConfig;

public class Ship 
{
    private int fuel; 
    private final int tankCapacity; 
    private int hullPercent; 
    
    public Ship(GameConfig c)
    {
        // takes value from GameConfig
        tankCapacity = c.tankCapacity(); 
        // The ship will always start at 100% health 
        hullPercent = 100; 
        // fuel is set in GameConfig 
        fuel = c.startingFuel();  
    }
    
    public int fuel()
    {
        return fuel; 
    }
    
    public int hullPercent()
    {
        return hullPercent; 
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
    
    // takes away some of the 100% heath of the hull when the hull gets damaged but it cant go below 0 as a ship can only be between 0 and 100 health.  
    public void damageHull(int damage)
    {
        hullPercent = Math.max(0, hullPercent - damage); 
    }
    
    // adds to the percentage of hull health as a repair 
    public void repairHull(int repair)
    {
        hullPercent = Math.min(100, hullPercent + repair); 
    }
}
