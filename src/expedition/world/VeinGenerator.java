/*
 * VeinGenerator
 * Builds a Vein for a given SiteTier: random depth and oil per
 * level, with pockets that are never adjacent and never on the
 * final level.
 */
package expedition.world;

import java.util.List; 
import java.util.ArrayList; 
import java.util.Random; 

public final class VeinGenerator 
{
    private VeinGenerator() { }
    
    // depending on the tier and the randomized details it creates a new vein
    public static Vein generate(SiteTier tier, Random rng)
    {
        // decides how many levels deep the new vein will be 
        // the + 1 is because without it it isnt inclusive, rng gives the slots of the array 0 to 5 if the max is 6 slots but we really want it to hit 6 so we do +1 
        int depth = tier.getMinDepth() + rng.nextInt(tier.getMaxDepth() - tier.getMinDepth() + 1); 
        
        // creates the array which is used in the Vein class - this array holds the oil amounts in each level of the vein 
        List<Integer> levels = new ArrayList<>(); 
        
        // adds a value for each level,
        for(int i = 0; i < depth; i++)
        {
            int oil = tier.getMinOil() + rng.nextInt(tier.getMaxOil() - tier.getMinOil() + 1);
            levels.add(oil); 
        }
        
        // this creates the pockets where there is no oil and you may believe thats the end of the vein but it coulod just be a temporary pocket of no oil and it means that 
        // you may want to keep drilling even if you hit the bottom cause you think its just a pocket etc. 
        // depth - 1 is important because their shouldnt be a pocket in the last level 
        for(int i = 0; i < depth - 1; i++)
        {
            // this says whether there should be a pocket at this level through randomizing it 
            boolean rolledPocket = rng.nextDouble() < tier.getPocketChance(); 
            // double checks that its fine to place a pocket in this location - its fine if either its at the first level as there is nothing before it, or the one before it
            // has no pocket, this is so theres not a pocket after a pocket. 
            boolean safeToPlace = (i == 0) || (levels.get(i - 1) != 0); 
            
            // if it rolled a pocket and also was a safe to place level (the prior level did not have a pocket) then it will set the value in that level to 0
            if (rolledPocket && safeToPlace)
            {
                levels.set(i, 0); 
            }
        }
        
        // now it has a complete vein it return it so that the other classes can use it
        return new Vein(levels); 
    }
}
