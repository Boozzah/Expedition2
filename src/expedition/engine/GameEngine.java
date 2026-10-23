/*
 * GameEngine
 * Orchestrator for the game. Owns the main run loop (menu, new run,
 * visiting planets, jumping between them, resolving the ending).
 * Delegates all game logic to other classes and contains no game
 * maths of its own.
 */
package expedition.engine;

import expedition.crew.CrewMember;
import expedition.exception.QuitGameException;
import expedition.exception.ReturnToMenuException;
import expedition.ship.FuelEconomy;
import expedition.ship.Ship;
import expedition.ui.GameView;
import expedition.ui.MenuChoice;
import expedition.world.Planet;
import expedition.item.ItemMenu;
import expedition.item.ItemTable;
import expedition.io.GameDataSource;
import expedition.exception.DataLoadException;
import expedition.exception.SaveException;
import expedition.io.SaveStore;
import expedition.io.ScoreStore;
import expedition.expedition.ExpeditionSelector;
import expedition.expedition.Expedition;
import expedition.world.DrillSite;
import expedition.expedition.DrillSession;
import expedition.expedition.AscentPhase;
import expedition.io.RunLogWriter;
import expedition.hazard.HazardSet;
import expedition.hazard.Hazard;
import expedition.util.TextWrapper; 

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class GameEngine {
    
    // 8 planets in a run
    private static final int ROUTE_LENGTH = 8;
    
    // how many runs the high-score screen lists, as many as fit on Nadia's screen
    private static final int HIGH_SCORES_SHOWN = 8;
    
    private final GameView view;
    private final GameConfig config;
    private final GameDataSource data;
    private final ExpeditionSelector selector;
    private final RunLogWriter log;
    private final SaveStore saves;
    private final ScoreStore scores;
    private List<Hazard> allHazards = List.of();
    private final Long fixedSeed;
    
    public GameEngine(GameView view, GameConfig config, 
                      GameDataSource data, ExpeditionSelector selector, 
                      RunLogWriter log, SaveStore saves, ScoreStore scores, Long fixedSeed) {
        this.view = view;
        this.config = config;
        this.data = data;
        this.selector = selector;
        this.log = log;
        this.saves = saves;
        this.scores = scores;
        this.fixedSeed = fixedSeed;
    }
    
    // owns the whole outer game loop
    // QuitGameException is not caught here - gets unwind back to main
    // then prints "Goodbye."
    
    public void run() throws QuitGameException, DataLoadException {
        boolean playing = true;
        while(playing) {
            // main menu
            MenuChoice choice = view.showMainMenu(hasSave());
            // AI assisted - Claude suggested catching ReturnToMenuException here so leaving a run goes back to the menu
            try {
                switch(choice) {
                    case NEW_GAME:
                        if (confirmNewRun()) {
                            playRun(startNewRun());
                        }
                        break;
                    case RESUME:
                        RunState resumed = resumeRun();
                        if (resumed != null) {
                            playRun(resumed);
                        }
                        break;
                    case HOW_TO_PLAY: showInstructions(); break;
                    case HIGH_SCORES: showHighScores(); break;
                    case QUIT: playing = false; break;
                }
            } catch (ReturnToMenuException e) {
                // the player left from the in-game menu. The save isn't deleted, so
                // Load Game carries on from the last planet the ship reached
            }
        }
    }
    
    // Builds everything for a new RunState. Crew selection doesn't exist yet
    private RunState startNewRun() throws DataLoadException {
        // Option to make a run reproducable with a seed, 
        long seed = (fixedSeed != null) ? fixedSeed : System.currentTimeMillis();
        
        log.startRun(seed);
        
        // create new ship based off config values
        Ship ship = new Ship(config);
        
        // Load Crew and planets
        List<CrewMember> crew = data.loadCrew();
        List<Planet> planets = data.loadPlanets();
        
        // load hazards
        allHazards = data.loadHazards();
        
        // every item that can be found this run, used by drops and ITEM hazards
        ItemTable items = new ItemTable(data.loadItems());
        
        return new RunState(seed, ship, crew, planets, ROUTE_LENGTH, config, items);
    }
    
    // There is only one save, so a new run will write over it at the first planet.
    // Checks first rather than losing someone's run without warning
    private boolean confirmNewRun() {
        if (!hasSave()) {
            return true;
        }
        return view.readYesNo("Starting a new run will replace your saved run. Carry on?");
    }
    
    // Builds the run back up from the autosave. The content is loaded fresh the same way
    // a new run does it, then the save says which planets, crew stats and items to use.
    // Returns null (back to the menu) if there is no save or it can't be used
    private RunState resumeRun() throws DataLoadException {
        try {
            SavedRun saved = saves.load();
            if (saved == null) {
                view.print("There is no saved run to resume.");
                return null;
            }
            
            allHazards = data.loadHazards();
            ItemTable items = new ItemTable(data.loadItems());
            RunState state = RunState.restore(saved, data.loadPlanets(), items, config);
            
            log.resumeRun(saved.seed());
            view.print("");
            view.print("Resuming your run.");
            return state;
        } catch (SaveException e) {
            view.print("Your saved run could not be loaded: " + e.getMessage());
            return null;
        }
    }
    
    // a save that can't be checked is treated as no save, the game still plays without one
    private boolean hasSave() {
        try {
            return saves.hasSave();
        } catch (SaveException e) {
            view.print("Saved runs aren't available: " + e.getMessage());
            return false;
        }
    }
    
    // Saves the run each time the ship arrives at a planet (GDD P2 4: autosave at each planet).
    // checkpoint() runs even if the save fails, so the run's rolls stay the same either way
    private void autosave(RunState state) {
        SavedRun snapshot = state.checkpoint();
        try {
            saves.save(snapshot);
            view.print("Run saved.");
        } catch (SaveException e) {
            view.print("The run could not be saved: " + e.getMessage());
        }
    }
    
    private void showHighScores() {
        try {
            view.showHighScores(scores.top(HIGH_SCORES_SHOWN));
        } catch (SaveException e) {
            view.print("The high scores could not be read: " + e.getMessage());
        }
    }
    
    // shows where the run's score came from, and adds it to the high-score table
    private void recordScore(Ending ending, ScoreSheet sheet) {
        HighScore score = HighScore.from(ending, sheet.total(), LocalDateTime.now());
        view.print("");
        for (Map.Entry<ScoreSheet.Reason, Integer> line : sheet.breakdown().entrySet()) {
            String sign = (line.getValue() > 0) ? "+" : "";
            view.print(String.format("  %-20s %s%d", line.getKey().label(), sign, line.getValue()));
        }
        view.print("Score: " + score.score());
        try {
            scores.add(score);
        } catch (SaveException e) {
            view.print("The score could not be saved: " + e.getMessage());
        }
    }
    
    // once a run is over there is nothing left to resume
    private void deleteSave() {
        try {
            saves.delete();
        } catch (SaveException e) {
            view.print("The finished run's save could not be removed: " + e.getMessage());
        }
    }
    
    // The actual gameplay loop
    private void playRun(RunState state) {
        // while run is not over, do the gameplay loop
        while (!state.isRunOver()) {
            reportStatus(state);
            autosave(state);
            
            // set the current planet and initialise team
            Planet planet = state.currentPlanet();
            Expedition team = null;
            
            // visit planet with team
            if (planet != null) {
                team = visitPlanet(state, planet);
                checkTerminalStates(state);
                if (state.isRunOver()){
                    break;
                }
            }
            // cant afford jump cost, even after using any items that might help
            if (!readyToJump(state)) {
                state.endRun(EndingReason.ADRIFT);
                break;
            }
            
            // calculate jump cost. worked out after readyToJump because a Repair Kit used
            // there lowers it
            int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
            
            // spend fuel
            state.ship().spendFuel(jumpCost);
            
            state.advancePlanet(team);
            checkTerminalStates(state);
            
            // if jump was the last jump end run
            if (state.jumpNumber() > ROUTE_LENGTH) {
                state.endRun(EndingReason.HOME);
                
            }
        }
        reportEnding(state);
        deleteSave();
    }
    
    // The jump prompt. The player can use items first (GDD 10: items can be used before a jump),
    // which also matters when the ship is short on fuel - Condensed Oil or a Repair Kit might
    // be enough to make the jump. Returns true to jump, false if the ship can't afford it
    // and the player has nothing left to try, which leaves them adrift
    private boolean readyToJump(RunState state) {
        ItemMenu itemMenu = new ItemMenu(state, view);
        
        while (true) {
            // worked out again each time round, using an item can change it
            int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
            boolean canAfford = state.ship().canAfford(jumpCost);
            boolean hasItems = !state.ship().inventory().isEmpty();
            
            view.print("");
            if (!canAfford) {
                view.print("The ship doesn't have enough fuel for the next jump. You require: " + jumpCost + " fuel");
                if (!hasItems) {
                    return false;
                }
            }
            
            List<String> options = new ArrayList<>();
            if (canAfford) {
                options.add("Jump");
            }
            if (hasItems) {
                options.add("Use item");
            }
            // giving up is only an option when jumping isn't
            if (!canAfford) {
                options.add("Drift");
            }
            
            String prompt = canAfford
                    ? "Jump " + state.jumpNumber() + " costs " + jumpCost + " fuel. Ready to jump? Type: "
                    : "Use an item, or drift? Type: ";
            String choice = view.readChoice(prompt, options);
            
            if (choice.equals("Jump")) {
                return true;
            }
            if (choice.equals("Drift")) {
                return false;
            }
            // aboard the ship, so anyone alive can be given an item
            itemMenu.open(null);
        }
    }
    
    private void showInstructions()
    {
        view.print("");
        view.print("=== EXPEDITION ==="); 
        view.print("After an expedition you ascend to the mothership, but they arent there...");
        view.print("You check the location tracker of the ship and see that they have left beacons planets away.");
        view.print(TextWrapper.wrap("Theres only so much fuel this ship can hold so you know you will have to make some more stops along the way before you get home."));
        view.print("");
        view.print(TextWrapper.wrap("Your mission is to collect enough fuel to get home, you must jump to a planet and decide if you want to drill. Just know that drilling can be risky."));
        view.print(TextWrapper.wrap("Hazards can cost you fuel, or even your crews life. Every jump costs more than the last so manage your fuel wisely. Good luck on your mission."));
        view.print("");
    }
    
    private Expedition visitPlanet(RunState state, Planet planet) {
        // flavour formatting
        view.print("");
        view.print("=== " + planet.name() + " (" + planet.archetype() + ") ===");
        view.print(TextWrapper.wrap(planet.introFlavour()));
        
        // write to run log
        log.log("=== " + planet.name() + " (" + planet.archetype() + ") ===");
        log.log(planet.introFlavour());
        
        // sites are generated on arrival, using the runs single seeded random
        planet.generateSites(state.rng(), config);
        
        // skipping is a real strategic option - banking fuel instead of drilling
        if (!view.readYesNo("Send an expedition down?")) {
            view.print("You stay aboard and conserve fuel.");
            return null;
        }
        // select crew members for expedition
        Expedition team = selector.select(state);
        
        if (team == null) {
            return null;
        }
        
        
        // The team may drill at several sites in one trip down
        // each is a drill phase. No extra stamina, the cost is more hazard
        // exposure, with a bigger haul at stake
        
        // the oil stays on the shuttle until the team is back on the ship,
        // so it only goes in the tank after the ascent
        int oilAboard = 0;
        
        while (true) {
            // re read each pass: closed by a hazard, or drilled dry,
            // drops out of the list before the next choice
            List<DrillSite> sites = planet.openSites();
            
            if (sites.isEmpty()) {
                view.print("Every site here is drilled out.");
                break;
            }
            
            showSites(sites);
            
            int pick = view.readInt("Choose a site: ", 1, sites.size());
            if (pick == GameView.BACK) {
                view.print("The team stands down.");
                break;
            }
            DrillSite site = sites.get(pick - 1);
            
            // counts drill phases, which is what raises ascent risk
            planet.incrementExpeditionCount();
            
            int haul = new DrillSession(site, team, state, view, config, hazardsFor(planet), oilAboard).run();
            oilAboard += haul;
            view.print("");
            view.print("Hauled " + haul + " oil.");
            
            // a hazard could have killed the last of the crew
            if (state.isRunOver()) {
                break;
            }
            
            // or everyone who came down, so there's nobody left to move
            if (team.livingMembers().isEmpty()) {
                break;
            }
            
            if (!view.readYesNo("Move the team to another site?")) {
                break;
            }
        }
        
        ascend(state, planet, team, oilAboard);
        return team;
    }
    
    // AI assisted - Claude suggested splitting showSites and hazardsFor out of visitPlanet to make it shorter
    // the numbered list of sites to drill, with each one's hazard chance at the top and bottom
    private void showSites(List<DrillSite> sites) {
        view.print("");
        view.print("--- Drill sites ---");
        for(int i = 0; i < sites.size(); i++) {
            DrillSite s = sites.get(i);
            int riskStart = (int) Math.round(s.tier().hazardChance(1) * 100);
            int riskDeep = (int) Math.round(s.tier().hazardChance(s.tier().getMaxDepth()) * 100);
            
            view.print("  " + (i + 1) + ". " + s.name()
                      + "  [" + s.tier() + "]"
                      + "  est. " + s.estimatedOil()
                      + "  hazard " + riskStart + "% rising to " + riskDeep + "%");
        }
    }
    
    // the hazards that can happen while drilling this planet: the universal ones plus its archetype's own
    private List<Hazard> hazardsFor(Planet planet) {
        List<Hazard> applicable = new ArrayList<>();
        for(Hazard h : allHazards) {
            if (h.set() == HazardSet.UNIVERSAL || h.set().name().equals(planet.archetype().name())) {
                applicable.add(h);
            }
        }
        return applicable;
    }
    
    // The climb back to the ship, then whatever oil survived it goes in the tank
    private void ascend(RunState state, Planet planet, Expedition team, int oilAboard) {
        // the whole team died down there, nobody is left to fly the shuttle back up
        if (team.livingMembers().isEmpty()) {
            if (oilAboard > 0) {
                view.print("Nobody is left to fly the shuttle back up. The " + oilAboard + " oil is lost with it.");
            }
            return;
        }
        
        // only the ascent hazards, the planet's own hazards are for drilling
        List<Hazard> ascentHazards = new ArrayList<>();
        for (Hazard h : allHazards) {
            if (h.set() == HazardSet.ASCENT) {
                ascentHazards.add(h);
            }
        }
        
        int oil = new AscentPhase(team, state, view, config, ascentHazards, oilAboard).run(planet);
        
        // The oil is the fuel. addOil banks it toward the run total and
        // pours it into the tank, returning how much actually fitted
        int stored = state.addOil(oil);
        view.print("");
        view.print("Back aboard the ship with " + oil + " oil.");
        if (stored < oil) {
            view.print("The tank is full. " + (oil - stored) + " oil left behind.");
        }
    }
    
    // hull goes down from ascent hazards. It makes every jump cost more, and at 0% the run ends
    private void reportStatus(RunState state) {
        view.print(""); 
        view.print("=== Jump " + state.jumpNumber() + " ===");

        // the cost of leaving this planet, shown on arrival so the player knows how much
        // oil they need before deciding whether to send anyone down
        int fuel = state.ship().fuel();
        int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
        String nextJump = "Next jump: " + jumpCost + " fuel";
        if (fuel < jumpCost) {
            nextJump += " (" + (jumpCost - fuel) + " short)";
        }
        view.print("Fuel: " + fuel + " Hull: " + state.ship().hullPercent() + "%   " + nextJump);
    }
    
    private void reportEnding(RunState state) {
        Ending ending = state.ending(); 
        
        
        // ending texts
        switch (ending.reason()) {
            case HOME: 
                view.print(TextWrapper.wrap("You see the mothership, its lights through the windows, and you are so excited to be back in the warmth of your home. You dock the ship, and your "
                        + "family is already waiting for you. Tears are streaming down everyones faces. You watch your other crewmates get the same welcoming. You will not "
                        + "be going for another expedition for a while. There will be a celebration tonight but all you care about is that you are home, you are with your "
                        + "family again and you are safe... CONGRATULATIONS YOU HAVE WON THE GAME")); 
                break;
            case ADRIFT: 
                view.print(TextWrapper.wrap("You search for anywhere fuel could be hidden, maybe you could make fuel out of your food. You try blending your food and putting it into the engine "
                        + "but all that did was waste what you had left. There is no hope. You will never see your family again, never see your home, your friends, or eat your "
                        + "favourite food. You try to break open the window. But all that leaves you with is broken tools, so you sit there hopeless with only one path you "
                        + "can take... GAME OVER, TRY AGAIN")); 
                break;
            case HULL_FAILURE:
                view.print(TextWrapper.wrap("The ships lights slowly turn off, the alarms sound and you realize that the only piece of home you have left is gone. The only small piece of saftey "
                        + "is breaking apart. You grab the only photo you have of your family, get into bed and try to dream that you made it home. That they welcomed you "
                        + "back. Having missed you for so long. And then you hear the oxygen waring... GAME OVER, TRY AGAIN"));
                break; 
            case CREW_LOST:
                view.print(TextWrapper.wrap("There is noone left. The ship sits empty with noone to take it home to the mother ship. Now a floating piece of rubbish in space... GAME OVER, TRY AGAIN"));
                break; 
            default: view.print("The run has ended");
        }
        
        // print out run stats
        view.print("");
        view.print("Planets reached: " + ending.planetsReached()); 
        view.print("Oil collected: " + ending.totalOilCollected());
        
        if (ending.anyoneSurvived()) {
            view.print("Survivors: " + namesOrNone(ending.survivorNames()));
            view.print("Lost: " + namesOrNone(ending.deadNames()));
        } else {
            view.print("Lost on the expedition: " + namesOrNone(ending.deadNames()));
            // dies on the ship
            view.print("Lost with the ship: " + namesOrNone(ending.survivorNames()));
        }
        
        recordScore(ending, state.score());
        
        // write run stats to a log
        log.logEnding(ending);
    }
    
    // helper method - "nobody" rather than an empty line
    private String namesOrNone(List<String> names) {
        return names.isEmpty() ? "nobody" : String.join(", ", names);
    }
    
    
    // The two ways a run can end without a player choosing anything
    // both are due to hazard effects, so this gets check after anything that can fire
    // one. endRun() ignores repeat calls, so checking often is safe
    private void checkTerminalStates(RunState state) {
        if (state.isRunOver()) {
            return;
        }
        
        // all crew are dead
        if (state.livingCrew().isEmpty()) {
            state.endRun(EndingReason.CREW_LOST);
            return;
        }
        
        // any ship part reaches 0% hull failure
        // ascent hazards are what wear the parts down to this
        if (state.ship().isBroken()) {
            state.endRun(EndingReason.HULL_FAILURE);
        }
    }
}
