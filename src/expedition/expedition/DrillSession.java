/*
 * DrillSession
 * Runs one trip down a DrillSite: the drill/ascend/use-item loop,
 * yield accumulation, item drops and hazard rolls. The core gameplay
 * loop everything else exists to support.
 * The Miner, Geologist, Doctor, Surveyor and Engineer abilities all fire in here.
 */
package expedition.expedition;

import expedition.world.DrillSite; 
import expedition.engine.RunState; 
import expedition.engine.GameConfig; 
import expedition.ui.GameView;
import expedition.crew.hooks.DrillModifier;
import expedition.crew.hooks.FieldMedic;
import expedition.crew.hooks.ItemFinder;
import expedition.crew.hooks.VeinInsight;
import expedition.hazard.Hazard;
import expedition.hazard.HazardOutcome; 
import expedition.hazard.SeverityRoller; 
import expedition.hazard.effect.EffectContext; 
import expedition.item.Item;
import expedition.item.ItemMenu;
import expedition.ship.Inventory;
import expedition.ship.FuelEconomy;
import expedition.crew.CrewMember;
import expedition.util.TextWrapper; 

import java.util.ArrayList;
import java.util.List;

public class DrillSession implements HaulHolder
{
    private final DrillSite site;
    private final Expedition team;
    private final RunState state; 
    private final GameView view;
    private final GameConfig config; 
    private int haul = 0; 
    private int consecutiveEmpty = 0; 
    private boolean forceEnd = false; 
    private List<Hazard> hazards;
    // oil from earlier sites on this trip, still on the shuttle until the ascent
    private final int oilAboard;
    private final ItemMenu itemMenu;
    
    public DrillSession(DrillSite site, Expedition team, RunState state, GameView view, GameConfig config, List<Hazard> hazards, int oilAboard)
    {
        this.site = site; 
        this.team = team; 
        this.state = state;
        this.view = view;
        this.config = config; 
        this.hazards = hazards;
        this.oilAboard = oilAboard;
        this.itemMenu = new ItemMenu(state, view);
    }
    
    // this runs until the player ascends, a hazard closes the site or two barren levels in a row signal that youve hit the bottom
    public int run()
    {
        // the Doctor's heal is once per session, so every new session makes it available again
        for (FieldMedic medic : team.fieldMedics())
        {
            medic.resetForSession();
        }

        // we user try catch so that quitting middrill means that you still get the oil you had collected prior
        try
        { 
            while (!forceEnd)
            {
                // drilling actions
                // this shows the progress of what the crew has already collected in this run and asks the player to choose whether they want to drill deeper or return to the ship
                printDrillStatus();
                
                // GDD 10: items can be used between drill levels. Only offered when there
                // is something in the hold, so an empty hold doesnt add a dead option
                boolean hasItems = !state.ship().inventory().isEmpty();
                view.print(hasItems ? "1) Drill  2) Ascend  3) Use item" : "1) Drill  2) Ascend");
            
                // this takes in what the choice was from the user (the input) 
                int choice = view.readInt("Choose an option: ", 1, hasItems ? 3 : 2); 
                
                // using an item doesnt drill, it goes back round to the same choice
                if (choice == 3)
                {
                    itemMenu.open(team);
                    continue;
                }
            
                // only an actual 1 drills deeper. 2 and Back both take the team back up to the
                // ship, before this Back fell through and drilled another level
                if (choice != 1)
                {
                    break;
                }
            
                // checks if the player got a hazard so that it can also quit out for you even though it hasnt reached the end
                drillOneLevel(); 
            
                if (forceEnd)
                {
                    break;
                }

                // the Geologist knows when the vein has run out, so the team comes up
                // at the true bottom instead of drilling into nothing
                if (geologistCallsBottom())
                {
                    state.score().siteDrilledDry();
                    break;
                }

                // This kicks the player off if they go past the bottom, and gives a small story
                if(consecutiveEmpty >= 2)
                {
                    view.print("You watch the site collapse in on itself. Theres nothing more to find here");
                    state.score().siteDrilledDry();
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
        
        // points for the oil, and a bonus for pushing deeper than most would
        int points = state.score().oilDrilled(yield) + state.score().levelDrilled(depth);
        
        view.print("Level " + depth + ": " + yield + " oil, +" + points + " points.");
        
        // rolled on every level, even empty ones - GDD 5.4 says the drop is independent of
        // the yield, so finding an item doesnt tell the player anything about the vein
        rollItemDrop();
        
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
        view.print(""); 
        view.print(TextWrapper.wrap(outcome.text()));
        
        // bundles all the details so that the effect of the hazard can take from them, like the state, the crew you chose, and everything in this drill session
        EffectContext ctx = new EffectContext(state, team, this); 
        
        // apply hazard effects and report to the player
        applyAndReport(outcome, ctx);
    }
    
    
    // the chance of an item on each level. Starts at ITEM_DROP_BASE and each crew member
    // with an ItemFinder ability can change it (the Surveyor adds 7%). itemFinders() already
    // leaves out anyone fatigued, so a tired Surveyor doesnt help
    private double itemDropChance()
    {
        double chance = config.itemDropBase() / 100.0;
        for (ItemFinder finder : team.itemFinders())
        {
            chance = finder.modifyDropChance(chance);
        }
        return chance;
    }
    
    // the Surveyor and Engineer abilities. The Surveyor changes the chance of finding
    // something at all, the Engineer changes which item it is (more Repair Kits)
    private void rollItemDrop()
    {
        if (state.rng().nextDouble() >= itemDropChance())
        {
            return;
        }
        
        Item item = state.itemTable().roll(state.rng(), team.itemFinders());
        if (item == null)
        {
            return;
        }
        
        view.print("  Found: " + item.name() + " - " + item.description());
        
        Inventory bag = state.ship().inventory();
        if (bag.add(item) || makeRoomFor(item, bag))
        {
            state.score().itemCollected();
        }
    }
    
    // GDD 10: a full hold turns finding an item into a choice - leave it, or throw
    // something out for it. Numbered like the drill menu so it works in the window too.
    // Returns true if the item was kept
    private boolean makeRoomFor(Item item, Inventory bag)
    {
        view.print("  The hold is full (" + bag.capacity() + " items).");
        view.print("  1) Leave the " + item.name() + " behind");
        
        // one option per kind of item rather than per item, so 3 Caffeine is one line
        List<String> names = new ArrayList<>(bag.stacked().keySet());
        for (int i = 0; i < names.size(); i++)
        {
            String name = names.get(i);
            view.print("  " + (i + 2) + ") Throw out one " + name + " (have " + bag.stacked().get(name) + ")");
        }
        
        int choice = view.readInt("Choose an option: ", 1, names.size() + 1);
        
        // 1 and Back both leave the new item, so nothing is thrown away by accident
        if (choice < 2)
        {
            view.print("  The " + item.name() + " is left behind.");
            return false;
        }
        
        String thrownOut = names.get(choice - 2);
        bag.take(thrownOut);
        bag.add(item);
        view.print("  Threw out one " + thrownOut + " to make room for the " + item.name() + ".");
        return true;
    }
    
    // important for hazards as you may have loss 40% of oil, and this calculates that
    @Override
    public void reduceHaulByPercent(int percent)
    {
        haul = haul - (haul * percent / 100); 
    }
    
    // if the site closes then you are forced to end and this site will stay permanently closed. 
    @Override
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
        // oil from earlier sites goes in the tank first, so it takes up space too
        int space = Math.max(0, capacity - fuel - oilAboard);
        int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
        int hazardPct = (int) Math.round(site.tier().hazardChance(nextLevel) * 100);
        int itemPct = (int) Math.round(itemDropChance() * 100);

        view.print("");
        view.print("Next Level: " + nextLevel + "   Current haul: " + haul + " oil"
                + "   Hazard next level: " + hazardPct + "%"
                + "   Item chance: " + itemPct + "%");

        String tank = "Tank: " + fuel + "/" + capacity;
        if (oilAboard > 0) {
            tank += " + " + oilAboard + " oil on the shuttle";
        }
        tank += " (" + space + " space)";

        int spill = oilAboard + haul - (capacity - fuel);
        if (spill > 0) {
            tank += " - " + spill + " oil would spill";
        }                                                   
        view.print(tank + "   Next jump: " + jumpCost + " fuel");

        // the Geologist's reading, so the player knows if an empty level is just
        // a pocket or the real bottom of the vein
        for (VeinInsight geologist : team.veinInsights())
        {
            view.print(geologist.describeRemaining(site.vein().levelsRemaining()));
        }
    }

    // The Geologist's auto eject. True when the vein has nothing left, so run() can
    // stop the session. Says who called it so it reads as the ability working
    private boolean geologistCallsBottom()
    {
        for (VeinInsight geologist : team.veinInsights())
        {
            if (geologist.shouldAutoEject(site.vein().levelsRemaining()))
            {
                view.print(geologist.getName() + " calls it: that was the last of the vein. The team heads back up.");
                return true;
            }
        }
        return false;
    }

    // The Doctor's ability. After a hazard, a doctor who hasn't healed yet this session
    // treats whoever on the team is now lowest on health. They can't treat themselves,
    // the ability is for helping another crew member
    private void treatInjured(List<CrewMember> injured)
    {
        for (FieldMedic medic : team.fieldMedics())
        {
            // already used their heal this session
            if (!medic.canTreat())
            {
                continue;
            }

            // finds the injured crew member with the lowest health, skipping the doctor themselves
            CrewMember target = null;
            for (CrewMember m : injured)
            {
                if (m != medic && (target == null || m.getHealth() < target.getHealth()))
                {
                    target = m;
                }
            }

            // nobody else on the team was hurt, so the heal is saved for later in the session
            if (target == null)
            {
                continue;
            }

            // health is capped, so report what was actually gained rather than the full 20
            int before = target.getHealth();
            medic.treat(target);
            view.print("  " + medic.getName() + " treats " + target.getName() + ": health +" + (target.getHealth() - before));

            // one heal per hazard is enough
            return;
        }
    }

    // Fires a hazard's effects and says what it does to the team/fuelsupply etc
    // The effects themselves are silent they just change numbers, so HazardReport
    // takes a snapshot first and reports the difference to the player afterwards
    private void applyAndReport(HazardOutcome outcome, EffectContext ctx) {
        HazardReport report = new HazardReport(state, view);
        int haulBefore = haul;
        boolean closedBefore = forceEnd;

        outcome.applyAll(ctx);

        // now output what changed. injured is the crew on this expedition who
        // survived but lost health, for the Doctor
        List<CrewMember> injured = report.printCrewChanges(team);

        // shown after the damage so the player sees what the hazard did, then the heal
        treatInjured(injured);

        report.printShipChanges(haul - haulBefore);

        if (forceEnd && !closedBefore) {
            view.print("  The site is closed. You are coming up.");
        }
    }
}
