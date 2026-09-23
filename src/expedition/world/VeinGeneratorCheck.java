
package expedition.world;

// AI corrected error - missing import for list 
import java.util.List; 
import java.util.Random; 

// AI assited with the code to create this check
public class VeinGeneratorCheck 
{
    public static void main(String[] args)
    {
        for(SiteTier tier : SiteTier.values())
        {
            for(int seed = 0; seed < 20; seed ++)
            {
                Vein vein = VeinGenerator.generate(tier, new Random(seed)); 
                List<Integer> levels = vein.levelsForTesting(); 
                
                for(int i = 0; i < levels.size() - 1; i++)
                {
                    if(levels.get(i) == 0 && levels.get(i + 1) == 0)
                    {
                        System.out.println("FAIL (" + tier + ", seed " + seed + "): there are adjacent pockets found " + i); 
                    }
                }
                
                int lastIndex = levels.size() - 1; 
                if (levels.get(lastIndex) == 0)
                {
                    System.out.println("FAIL (" + tier + ", seed " + seed + "): last level is a pocket! " + levels); 
                }
            }
        }
        
        System.out.println("Check Complete");
    }
}
