/*
 * FuelEconomy
 * Stateless helper for jump-cost maths: base cost per jump, the
 * hull-damage cost multiplier, and total cost across a range of
 * jumps. Pure functions only, easy to unit test.
 */
package expedition.ship;

import expedition.engine.GameConfig;

public final class FuelEconomy 
{
    private FuelEconomy()
    {
        // private and so nobody is abel to call this hence theres no need to put any code in here 
    }
    
    // the base jump cost uses a figure that I have set in the GameConfig class,
    // it incrememnts as the amount of jumps reduce. So the closer you get to the end. 
    // also using the figure set in GameConfig. This is necessary to have the starting 
    // number like in an array i = 0, and so jumps still have a cost but the cost gets worse as 
    // you get further into the game and the more jumps that you do, 
    // meaning its harder as you go through which is necesary for a game 
    public static int baseJumpCost(int jumpNumber, GameConfig c)
    {
        int base = c.jumpCostBase(); 
        int increment = c.jumpCostIncrement(); 
        return base + increment * (jumpNumber - 1); 
    }
    
    // this is the calculation for how much the cost of the jump should be multiplied
    // by in order to caluclate the cost - like in the next method. 
    // Depending on the percentage wrecked of the ship the worse the multiplier is 
    // it works like say theres 100% - 76% damage there is no penalty, 
    // 75 - 51 goes to 1.08 which is a slight extra, 50 - 26% is 1.16x and 
    // 25 - 1 is 1.24x 
    public static double hullMultiplier(int hullPercent)
    {
        // the hull is split into 4 25% wide bands and divides the hullPercentage 
        // into a number between 1 and 3 and using a switch case which is a easier and more
        // efiicient way to do it. 
        switch ((hullPercent - 1) / 25)
        {
            case 3: return 1.00; 
            case 2: return 1.08; 
            case 1: return 1.16; 
            default: return 1.24; 
        }
    }
    
    // some of the cost in a jump comes from whether there are issues with the ship
    // and therefore test the the baseJumpCost can be edited slightly based on if the ship
    // is damaged, so therefore dont damage your ship 
    public static int jumpCost(int jumpNumber, int hullPercent, GameConfig c)
    {
        int base = baseJumpCost(jumpNumber, c); 
        double multiplier = hullMultiplier(hullPercent); 
        double raw = base * multiplier; 
        return (int) Math.ceil(raw); 
    }
    
    // This is just so the code can know that its even possible to make the jump (and future jumps) 
    // with the current resources you have 
    public static int totalRemainingCost(int fromJump, int toJump, int hullPercent, GameConfig c)
    {
        int total = 0; 
        for (int j = fromJump; j <= toJump; j++)
        {
            total += jumpCost(j, hullPercent, c); 
        }
        return total; 
    }
}
