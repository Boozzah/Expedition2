/*
 * SiteTier
 * LOW/MEDIUM/HIGH site difficulty bands. Carries the depth range,
 * oil range, pocket chance and hazard-chance-by-depth constants for
 * that tier.
 */
package expedition.world;

public enum SiteTier 
{
    // values used in each field
    // in this order: minDepth, maxDepth, minOil, maxOil, pocketChance, hazardBase
    // hazardPerLevel, hazardCap
    LOW(4, 6, 20, 30, 0.10, 0.05, 0.03, 0.40),
    MEDIUM(3, 8, 30, 45, 0.18, 0.10, 0.05, 0.60),
    HIGH(2, 11, 40, 60, 0.25, 0.16, 0.07, 0.75); 
    
    // initilized fields 
    private final int minDepth; 
    private final int maxDepth; 
    private final int minOil; 
    private final int maxOil; 
    private final double pocketChance; 
    private final double hazardBase; 
    private final double hazardPerLevel; 
    private final double hazardCap; 
    
    // constructor 
    SiteTier(int minDepth, int maxDepth, int minOil, int maxOil, double pocketChance, double hazardBase, double hazardPerLevel, double hazardCap)
    {
        this.minDepth = minDepth; 
        this.maxDepth = maxDepth; 
        this.minOil = minOil;
        this.maxOil = maxOil; 
        this.pocketChance = pocketChance; 
        this.hazardBase = hazardBase; 
        this.hazardPerLevel = hazardPerLevel; 
        this.hazardCap = hazardCap; 
    }
    
    // getters since they are private for only the ones that need extra code later 
    public int getMinDepth()
    {
        return minDepth; 
    }
    
    public int getMaxDepth()
    {
        return maxDepth; 
    }
    
    public int getMinOil()
    {
        return minOil; 
    }
    
    public int getMaxOil()
    {
        return maxOil; 
    }
    
    public double getPocketChance()
    {
        return pocketChance; 
    }
    
    // calculates the chance of a hazard using the other variables
    public double hazardChance(int depth)
    {
        // creates a chance using the given base hazard level of that location 
        // plus the hazard chances per level and the current depth the player is 
        // at. This now has the chance percentage which in order to keep it under
        // the cap you return the min of either chance or cap. If the cap is smaller 
        // then the chance caluclated then the cap is returned etc. 
        double chance = hazardBase + (hazardPerLevel * depth); 
        return Math.min(chance, hazardCap); 
    }
    
}
