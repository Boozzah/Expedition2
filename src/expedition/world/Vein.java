/*
 * Vein
 * The ordered list of per-level oil yields for one DrillSite (0 = a
 * barren pocket). Tracks how far it has been drilled.
 */
package expedition.world;

public class Vein 
{
    // holds how much oil is in each level
    private final java.util.List<Integer> levels; 
    // is a count to remember how far you have gone so far which starts at 0 as you havent drilled yet and increments later in this code 
    private int drilled = 0; 
    
    public Vein(java.util.List<Integer> levels)
    {
        this.levels = levels; 
    }
    
    // returns the amount of oil you drilled at the current level and moves one level deeper
    // if it hits the bottom it returns 0 because there is no more oil left but we want it so you can keep risking it because you think its just a pocket of no oil
    public int drillNext()
    {
        // only real levels have oil, past the bottom it stays at 0
        int yield = 0;
        if(drilled < levels.size())
        {
            yield = levels.get(drilled);
        }
        // past the bottom still counts as going a level deeper. If it didn't, the level
        // number would repeat and give away that the vein has run out
        // AI assisted - Claude Code found the level number repeated past the bottom and told us how to fix it
        drilled++;
        return yield;
    }
    
    // this calculates how many are left open, the amount of levels - the amount you have drilled
    // Math.max just makes sure it doesnt become a negative number when you have drilled past the vein level as the lowest it can go is 0
    public int levelsRemaining()
    {
        return Math.max(0, levels.size() - drilled); 
    }
    
    public int currentDepth()
    {
        return drilled; 
    }
    
    // this is just yes or no if you have past the bottom 
    public boolean pastBottom()
    {
        return drilled >= levels.size(); 
    }
    
    // this is a check for the veinGenerator as theres no way to know if the code actually followed
    // the levels, this returns what they are so it can double check that. 
    java.util.List<Integer> levelsForTesting()
    {
        return levels; 
    }
            
}
