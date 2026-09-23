/*
 * DrillSession
 * Runs one trip down a DrillSite: the drill/ascend/use-item loop,
 * yield accumulation, item drops and hazard rolls. The core gameplay
 * loop everything else exists to support.
 */
package expedition.expedition;

import expedition.world.DrillSite; 
import expedition.engine.RunState; 
import expedition.engine.GameConfig; 
import expedition.ui.ConsoleUI;
import expedition.ui.InputValidator; 
import expedition.crew.hooks.DrillModifier; 
import expedition.hazard.Hazard;
import expedition.hazard.HazardOutcome; 
import expedition.hazard.SeverityRoller; 
import expedition.hazard.effect.EffectContext; 
import expedition.ship.FuelEconomy;
import expedition.crew.CrewMember;
import expedition.util.TextWrapper; 

import java.util.List;

public class DrillSession 
{
   /*
   * TODO: Reset Doctors ability each drill session - will be implemented in project 2
   */
    
    private final DrillSite site;
    private final Expedition team;
    private final RunState state; 
    private final ConsoleUI ui; 
    private final InputValidator input; 
    private final GameConfig config; 
    private int haul = 0; 
    private int consecutiveEmpty = 0; 
    private boolean forceEnd = false; 
    private List<Hazard> hazards;
    
    public DrillSession(DrillSite site, Expedition team, RunState state, ConsoleUI ui, InputValidator input, GameConfig config, List<Hazard> hazards)
    {
        this.site = site; 
        this.team = team; 
        this.state = state;
        this.ui = ui; 
        this.input = input; 
        this.config = config; 
        this.hazards = hazards;
    }
    
    // this runs until the player ascends, a hazard closes the site or two barren levels in a row signal that youve hit the bottom
    public int run()
    {
        // we user try catch so that quitting middrill means that you still get the oil you had collected prior
        try
        { 
            while (!forceEnd)
            {
                // drilling actions
                // this shows the progress of what the crew has already collected in this run and asks the player to choose whether they want to drill deeper or return to the ship
                printDrillStatus();
                ui.print("1) Drill  2) Ascend");
            
                // this takes in what the choice was from the user (the input) 
                int choice = input.readInt("Choose an option: ", 1, 2); 
            
                // if they picked 2 then they go back to the ship 
                if (choice == 2)
                {
                    break; 
                }
            
                // checks if the player got a hazard so that it can also quit out for you even though it hasnt reached the end
                drillOneLevel(); 
            
                if (forceEnd)
                {
                    break; 
                }
            
                // This kicks the player off if they go past the bottom, and gives a small story
                if(consecutiveEmpty >= 2)
                {
                    ui.print("You watch the site collapse in on itself. Theres nothing more to find here");
                    break;
                }
            }
        }
        catch (expedition.exception.QuitGameException e)
        {
            // bank whatever was collected up until you quit or was forced to quit early
            state.addOil(haul);
            throw e;       
        }
        
        return haul; 
    }
    
    // drills down a level and then does whatever is necessary - like collecting the oil, applying the miner bonus, updating the haul, and adding to the count. 
    public void drillOneLevel()
    {
        // adds to the count for the current depth as you have drilled down and what the yeild is
        int depth = site.vein().currentDepth() + 1; 
        int yield = site.vein().drillNext(); 
        
        // applies any bonuses - currently just miners bonus 
        for(DrillModifier m : team.drillModifiers())
        {
            yield = m.modifyYield(yield, depth); 
        }
        
        // this calculates the haul 
        haul += yield; 
        
        // if there is no yield for 2 levels in a row then it is consecutivley empty and is a force quit because you have hit the end. 
        consecutiveEmpty = (yield == 0) ? consecutiveEmpty + 1 : 0; 
        
        ui.print("Level " + depth + ": " + yield + " oil.");
        
        // calls the method that rolls a potentional hazard each time youve drilled a level
        rollHazard(depth); 
    }
    
    // the reason we look at depth is because the deeper you are the more likely - severity is weighted by depth
    public void rollHazard(int depth)
    {
        // creates a random number and if that random number is greater than or equal to the hazardchance nothing happens and it exits early
        if(state.rng().nextDouble() >= site.tier().hazardChance(depth))
        { 
            return;
        }
        
        if (hazards.isEmpty()) {
            return;
        }
        
        Hazard hazard = hazards.get(state.rng().nextInt(hazards.size()));
        
        // a hazard is happening as it didnt exit early by this time, this rolls the number using the already formed weighted severity roller
        int severity = SeverityRoller.roll(depth, state.rng()); 
        // finds the outcome for that severity and holds it 
        HazardOutcome outcome = hazard.outcomeFor(severity); 
        
        // shows the flavour text that gives the story of what happened
        ui.print(""); 
        ui.print(TextWrapper.wrap(outcome.text(), 100));
        
        // bundles all the details so that the effect of the hazard can take from them, like the state, the crew you chose, and everything in this drill session
        EffectContext ctx = new EffectContext(state, team, this); 
        
        // apply hazard effects and report to the player
        applyAndReport(outcome, ctx);
    }
    
    
    // important for hazards as you may have loss 40% of oil, and this calculates that
    public void reduceHaulByPercent(int percent)
    {
        haul = haul - (haul * percent / 100); 
    }
    
    // if the site closes then you are forced to end and this site will stay permanently closed. 
    public void closeSiteAndEnd()
    {
        site.close();
        forceEnd = true; 
    }
    
    // The numbers the drill or ascend decision depends on. Without this the player is choosing blind.
    // The important one is tank space: oil over capacity is thrown away when the haul is banked
    private void printDrillStatus()
    {
        int nextLevel = site.vein().currentDepth() + 1;
        int fuel = state.ship().fuel();
        int capacity = config.tankCapacity();               
        int space = capacity - fuel;
        int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
        int hazardPct = (int) Math.round(site.tier().hazardChance(nextLevel) * 100);

        ui.print("");
        ui.print("Next Level: " + nextLevel + "   Current haul: " + haul + " oil"
                + "   Hazard next level: " + hazardPct + "%");

        String tank = "Tank: " + fuel + "/" + capacity + " (" + space + " space)";
        if (haul > space) {
            tank += " - " + (haul - space) + " oil would spill";
        }                                                   
        ui.print(tank + "   Next jump: " + jumpCost + " fuel");
    }
    
    // Fires a hazard's effects and says what it does to the team/fuelsupply etc
    // The effects themselves are silent they just change number
    // so the player would otherwise read severl senteces of story and find the damage later on
    // report it to the player here
    private void applyAndReport(HazardOutcome outcome, EffectContext ctx) {
        List<CrewMember> crew = state.crew();
        int n = crew.size();
        
        // everything that an effect can touch
        int[] health = new int[n];                          
        int[] stamina = new int[n];                                                     
        int[] composure = new int[n];
        boolean[] alive = new boolean[n];
        for (int i = 0; i < n; i++) {
            CrewMember m = crew.get(i);
            health[i] = m.getHealth();
            stamina[i] = m.getStamina();                    
            composure[i] = m.getComposure();
            alive[i] = m.isAlive();
        }                                                   
        int haulBefore = haul;
        int fuelBefore = state.ship().fuel();
        int hullBefore = state.ship().hullPercent();
        boolean closedBefore = forceEnd;                                                

        outcome.applyAll(ctx);
        
        // now output what changed
        for (int i = 0; i < n; i++) {
            CrewMember m = crew.get(i);

            if (alive[i] && !m.isAlive()) {
                ui.print("  " + m.getName() + " is killed.");
                continue;   // their stat losses are not the story here
            }

            String changes = "";
            changes += change(changes, "health", m.getHealth() - health[i]);
            changes += change(changes, "stamina", m.getStamina() - stamina[i]);
            changes += change(changes, "composure", m.getComposure() - composure[i]);

            if (!changes.isEmpty()) {
                ui.print("  " + m.getName() + ": " + changes);
            }
        }
        
        String ship = "";
        ship += change(ship, "oil", haul - haulBefore);
        ship += change(ship, "fuel", state.ship().fuel() - fuelBefore);
        ship += change(ship, "hull", state.ship().hullPercent() - hullBefore);

        if (!ship.isEmpty()) {
            ui.print("  " + ship);
        }
                                                              
        if (forceEnd && !closedBefore) {                                                
            ui.print("  The site is closed. You are coming up.");
        }
    }
    
    // changes numbers into readable text, and handles commas between items
    private String change(String soFar, String label, int change)
    {
        if (change == 0) {
            return "";
        }                                                   
        String sep = soFar.isEmpty() ? "" : ", ";                                       
        String sign = (change > 0) ? "+" : "";
        return sep + label + " " + sign + change;
    }
}
