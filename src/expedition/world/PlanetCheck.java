package expedition.world;


// We are checking that the planet generates exactly 3 sites
// all 3 site names are distinct
// that the tiers include atleast one LOW and one HIGH 
// opensites always returns 3 when nothing is closed or exhausted yet
// each sites estimated oil prints something sensible

import expedition.engine.GameConfig;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Random; 

public class PlanetCheck 
{
    public static void main(String[] args) 
    {
        List<String> names = new ArrayList<>(); 
        names.add("North Shelf");
        names.add("Cryo-Vent 4"); 
        names.add("The Silo"); 
        names.add("Deep Collar");
        names.add("Frostline Ridge");
        names.add("Glacier Cut"); 
        
        Planet planet = new Planet(
                "Asteria",
                PlanetArchetype.FROZEN,
                "Welcome to Asteria, A planet of deep ridges that you do not want to explore, some people have been said to climb down and to have never come back out. Good luck",
                names); 
        
        GameConfig config = GameConfig.load(); 
        Random rng = new Random(42); 
        planet.generateSites(rng, config);
        
        System.out.println(planet.name());
        System.out.println(planet.introFlavour());
        System.out.println();
        
        List<DrillSite> open = planet.openSites(); 
        System.out.println("Open sites: " + open.size()); 
        for (DrillSite site : open)
        {
            System.out.println(site.name() + " - " + site.tier() + " - " + site.estimatedOil());
        }
    }
}
