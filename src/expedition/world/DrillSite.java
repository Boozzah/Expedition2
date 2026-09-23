/*
 * DrillSite
 * One of a planet's three drillable sites: name, SiteTier, its Vein
 * and whether it has been closed by a hazard. Only ever shows the
 * player a fuzzed oil estimate, never the true total.
 */
package expedition.world;

public class DrillSite 
{
   private final String name;  
   private final SiteTier tier; 
   private final Vein vein; 
   private boolean closed = false; 
   
   public DrillSite(String name, SiteTier tier, Vein vein)
   {
       this.name = name; 
       this.tier = tier; 
       this.vein = vein; 
   }
   
   public String estimatedOil()
   {
       // avgOil and avgDepth finds a mid point of the tiers oil range and depth range. The range is important because we dont want to tell the user what they should be expecting
       double avgOil = (tier.getMinOil() + tier.getMaxOil()) / 2.0; 
       double avgDepth = (tier.getMinDepth() + tier.getMaxDepth()) / 2.0; 
       // multiplying them together gives a rough typical total oil for that tier 
       double centerEstimate = avgOil * avgDepth; 
       
       // this widens the range more so that it gives more of a guess for the user instead of a precise average
       int low = (int) (centerEstimate * 0.8);
       int high = (int) (centerEstimate * 1.2); 
       
       // this is so it can print like ~ 100 - 150
       return "~ " + low + " - " + high + " oil -"; 
   }
   
   // a closed site stays closed - you cannot go back if you decided you wanted to drill some more
   public boolean isClosed()
   {
       return closed; 
   }
   
   public void close()
   {
       closed = true; 
   }
   
   // checks with Vein instead of also putting the logic here
   public boolean isExhausted()
   {
       return vein.pastBottom(); 
   }
   
   public Vein vein()
   {
       return vein; 
   }
   
   public SiteTier tier()
   {
       return tier; 
   }
   
   public String name()
   {
       return name; 
   }
}
