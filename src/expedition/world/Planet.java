/*
 * Planet
 * A stop on the route: name, archetype, flavour text and the three
 * DrillSites generated on arrival. Tracks how many expeditions have
 * been flown here, used for ascent risk and rest-recovery.
 */
package expedition.world;

// AI corrected this error, found import GameConfig was missing 
import java.util.List; 
import java.util.ArrayList; 
import java.util.Random; 
import expedition.engine.GameConfig; 

public class Planet 
{
    private final String name; 
    private final PlanetArchetype archetype;
    private final String introFlavour; 
    private final List<String> siteNamePool; 
    private List<DrillSite> sites = new ArrayList<>(); 
    private int expeditionCount = 0; 
    
    // Ai suggested this correction of this line of code - adding list<String> 
    public Planet(String name, PlanetArchetype archetype, String introFlavour, List<String> siteNamePool)
    {
        this.name = name; 
        this.archetype = archetype;
        this.introFlavour = introFlavour; 
        this.siteNamePool = siteNamePool; 
    }
    
    // builds the sites with their unique names
    // There will always be one low, one high and one random tier so that it keeps the planets sites different
    public void generateSites(Random rng, GameConfig c)
    {
        // creates a new list so that when the names are shuffled it isnt constant scrambling the original list
        List<String> shuffledNames = new ArrayList<>(siteNamePool);  
        java.util.Collections.shuffle(shuffledNames, rng);
        
        // creates the fixed array set at 3 sites as every planet just has 3
        SiteTier[] tiers = new SiteTier[3]; 
        // these are assigned to LOW and to HIGH so that some difference is always guaranteed
        tiers[0] = SiteTier.LOW; 
        tiers[1] = SiteTier.HIGH;
        // this is the array of the enum values - LOW - MEDIUM or HIGH, so that the random will be able to choose from them
        SiteTier[] allTiers = SiteTier.values(); 
        // this says that the final tier will look at the enum and pick a random one 
        tiers[2] = allTiers[rng.nextInt(allTiers.length)]; 
        
        // it refreshes the site as a just in case, when you first reach the planet its already an empty array, however if this for some reason gets called again, this is here
        // as a measure to just make sure that this is an empty array ready to fill with the new sites, instead of the 6 when there should only be 3 sites
        sites = new ArrayList<>();
        // loops through the sites and gives each index a name 
        for (int i = 0; i < 3; i++)
        {
            // grabs the name sitting at the current loop of i in the shuffled list, and also the tier sitting in this same position and pairs them up
            String siteName = shuffledNames.get(i); 
            SiteTier tier = tiers[i]; 
            // generates the vein for this site
            Vein vein = VeinGenerator.generate(tier, rng); 
            // puts all of this information into one Drill site
            DrillSite site = new DrillSite(siteName, tier, vein); 
            // adds the drill site to the new empty array
            sites.add(site); 
        }
    }
    
    // this means that it will only show you sites that arent closed, the sites being closed is worked out in the DrillSite class and used here. 
    public List<DrillSite> openSites()
    {
       List<DrillSite> open = new ArrayList<>(); 
       
       // loops through all the sites that the planet has (3) 
       for (DrillSite site : sites)
       {
           // if site is not closed and  if site is not exhausted then show the open site (added to the open list) 
           if (!site.isClosed() && !site.isExhausted())
           {
               open.add(site); 
           }
       }
       
       return open; 
    }
    
    // the more you take trips/expeditions to the drill sites the higher the hazards go, this keeps us updated
    public int expeditionCount()
    {
        return expeditionCount; 
    }
    
    public void incrementExpeditionCount()
    {
        expeditionCount++; 
    }
    
    public String name()
    {
        return name; 
    }
    
    public String introFlavour()
    {
        return introFlavour; 
    }
    
    public PlanetArchetype archetype() {
        return archetype;
    }
    
    // PlanetDao needs to read the site names to save them, so Planet needs a getter
    public List<String> siteNamePool() {
        return siteNamePool;
    }
}
