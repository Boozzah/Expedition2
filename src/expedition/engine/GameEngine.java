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
import expedition.ship.FuelEconomy;
import expedition.ship.Ship;
import expedition.ui.ConsoleUI;
import expedition.ui.InputValidator;
import expedition.world.Planet;
import expedition.io.GameDataSource;
import expedition.exception.DataLoadException;
import expedition.expedition.ExpeditionSelector;
import expedition.expedition.Expedition;
import expedition.world.DrillSite;
import expedition.expedition.DrillSession;
import expedition.io.RunLogWriter;
import expedition.hazard.HazardSet;
import expedition.hazard.Hazard;
import expedition.util.TextWrapper; 

import java.util.List;
import java.util.ArrayList;

public class GameEngine {
    
    // 8 planets in a run
    private static final int ROUTE_LENGTH = 8;
    
    private final ConsoleUI ui;
    private final InputValidator input;
    private final GameConfig config;
    private final GameDataSource data;
    private final ExpeditionSelector selector;
    private final RunLogWriter log;
    private List<Hazard> allHazards = List.of();
    private final Long fixedSeed;
    
    public GameEngine(ConsoleUI ui, InputValidator input, GameConfig config, 
                      GameDataSource data, ExpeditionSelector selector, 
                      RunLogWriter log, Long fixedSeed) {
        this.ui = ui;
        this.input = input;
        this.config = config;
        this.data = data;
        this.selector = selector;
        this.log = log;
        this.fixedSeed = fixedSeed;
    }
    
    // owns the whole outer game loop
    // QuitGameException is not caught here - gets unwind back to main
    // then prints "Goodbye."
    
    public void run() throws QuitGameException, DataLoadException {
        boolean playing = true;
        while(playing) {
            String choice = input.readChoice("--- MAIN MENU ---", List.of("New Game (n)", "How to Play (h)", "Quit (q)"));
            switch(choice) {
                case "New Game (n)": playRun(startNewRun()); break;
                case "How to Play (h)": showInstructions(); break; 
                case "Quit (q)": playing = false; break;
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
        
        return new RunState(seed, ship, crew, planets, ROUTE_LENGTH, config);
    }
    
    // The actual gameplay loop
    private void playRun(RunState state) {
        // while run is not over, do the gameplay loop
        while (!state.isRunOver()) {
            reportStatus(state);
            
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
            // calculate jump cost
            int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
            
            // cant afford jump cost
            if (!state.ship().canAfford(jumpCost)) {
                ui.print("The ship doesn't have enough fuel for the next jump. You require: " + jumpCost + " fuel");
                state.endRun(EndingReason.ADRIFT);
                break;
            }
            // Only one real option exists at this stage
            ui.print(""); 
            input.readChoice("Jump " + state.jumpNumber() + " costs " + jumpCost + " fuel. Ready to jump? Type: ", List.of("Jump"));
            
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
    }
    
    private void showInstructions()
    {
        ui.print("");
        ui.print("=== EXPEDITION ==="); 
        ui.print("After an expedition you ascend to the mothership, but they arent there...");
        ui.print("You check the location tracker of the ship and see that they have left beacons planets away.");
        ui.print(TextWrapper.wrap("Theres only so much fuel this ship can hold so you know you will have to make some more stops along the way before you get home.", 100));
        ui.print("");
        ui.print(TextWrapper.wrap("Your mission is to collect enough fuel to get home, you must jump to a planet and decide if you want to drill. Just know that drilling can be risky.", 100));
        ui.print(TextWrapper.wrap("Hazards can cost you fuel, or even your crews life. Every jump costs more than the last so manage your fuel wisely. Good luck on your mission.", 100));
        ui.print("");
    }
    
    private Expedition visitPlanet(RunState state, Planet planet) {
        // flavour formatting
        ui.print("");
        ui.print("=== " + planet.name() + " (" + planet.archetype() + ") ===");
        ui.print(TextWrapper.wrap(planet.introFlavour(), 100));
        
        // write to run log
        log.log("=== " + planet.name() + " (" + planet.archetype() + ") ===");
        log.log(planet.introFlavour());
        
        // sites are generated on arrival, using the runs single seeded random
        planet.generateSites(state.rng(), config);
        
        // skipping is a real strategic option - banking fuel instead of drilling
        if (!input.readYesNo("Send an expedition down?")) {
            ui.print("You stay aboard and conserve fuel.");
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
        
        while (true) {
            // re read each pass: closed by a hazard, or drilled dry,
            // drops out of the list before the next choice
            List<DrillSite> sites = planet.openSites();
            
            if (sites.isEmpty()) {
                ui.print("Every site here is drilled out.");
                break;
            }
            
            // flavour text
            ui.print("");
            ui.print("--- Drill sites ---");
            for(int i = 0; i < sites.size(); i++) {
                DrillSite s = sites.get(i);
                // randomise hazard chance for each site option
                int riskStart = (int) Math.round(s.tier().hazardChance(1) * 100);
                int riskDeep = (int) Math.round(s.tier().hazardChance(s.tier().getMaxDepth()) * 100);
                
                ui.print("  " + (i + 1) + ". " + s.name()
                          + "  [" + s.tier() + "]"
                          + "  est. " + s.estimatedOil()
                          + "  hazard " + riskStart + "% rising to " + riskDeep + "%");
            }
            
            int pick = input.readInt("Choose a site: ", 1, sites.size());
            if (pick == InputValidator.BACK) {
                ui.print("The team stands down.");
                break;
            }
            DrillSite site = sites.get(pick - 1);
            
            // counts drill phases, which is what raises ascent risk
            planet.incrementExpeditionCount();
            
            // create a list of applicable hazards for a given planet
            List<Hazard> applicable = new ArrayList<>();
            for(Hazard h : allHazards) {
                if (h.set() == HazardSet.UNIVERSAL || h.set().name().equals(planet.archetype().name())) {
                    applicable.add(h);
                }
            }
            
            int haul = new DrillSession(site, team, state, ui, input, config, applicable).run();
            
            // The oil is the fuel. addOil banks it toward the run total and
            // pours it into the tank, returning how much actually fitted
            int stored = state.addOil(haul);
            ui.print("");
            ui.print("Hauled " + haul + " oil.");
            if (stored < haul) {
                ui.print("The tank is full. " + (haul - stored) + " oil left behind.");
            }
            
            // a hazard could have killed the last of the crew
            if (state.isRunOver()) {
                break;
            }
            
            if (!input.readYesNo("Move the team to another site?")) {
                break;
            }
        }
        return team;
    }
    
    // hull percent does not mean anything at this stage, will be more useful in project 2
    private void reportStatus(RunState state) {
        ui.print(""); 
        ui.print("=== Jump " + state.jumpNumber() + " ===");
        ui.print("Fuel: " + state.ship().fuel() + " Hull: " + state.ship().hullPercent() + "%");
    }
    
    private void reportEnding(RunState state) {
        Ending ending = state.ending(); 
        
        
        // ending texts
        switch (ending.reason()) {
            case HOME: 
                ui.print(TextWrapper.wrap("You see the mothership, its lights through the windows, and you are so excited to be back in the warmth of your home. You dock the ship, and your "
                        + "family is already waiting for you. Tears are streaming down everyones faces. You watch your other crewmates get the same welcoming. You will not "
                        + "be going for another expedition for a while. There will be a celebration tonight but all you care about is that you are home, you are with your "
                        + "family again and you are safe... CONGRATULATIONS YOU HAVE WON THE GAME", 100)); 
                break;
            case ADRIFT: 
                ui.print(TextWrapper.wrap("You search for anywhere fuel could be hidden, maybe you could make fuel out of your food. You try blending your food and putting it into the engine "
                        + "but all that did was waste what you had left. There is no hope. You will never see your family again, never see your home, your friends, or eat your "
                        + "favourite food. You try to break open the window. But all that leaves you with is broken tools, so you sit there hopeless with only one path you "
                        + "can take... GAME OVER, TRY AGAIN", 100)); 
                break;
            case HULL_FAILURE:
                ui.print(TextWrapper.wrap("The ships lights slowly turn off, the alarms sound and you realize that the only piece of home you have left is gone. The only small piece of saftey "
                        + "is breaking apart. You grab the only photo you have of your family, get into bed and try to dream that you made it home. That they welcomed you "
                        + "back. Having missed you for so long. And then you hear the oxygen waring... GAME OVER, TRY AGAIN", 100));
                break; 
            case CREW_LOST:
                ui.print(TextWrapper.wrap("There is noone left. The ship sits empty with noone to take it home to the mother ship. Now a floating piece of rubbish in space... GAME OVER, TRY AGAIN", 100));
                break; 
            default: ui.print("The run has ended");
        }
        
        // print out run stats
        ui.print("");
        ui.print("Planets reached: " + ending.planetsReached()); 
        ui.print("Oil collected: " + ending.totalOilCollected());
        
        if (ending.anyoneSurvived()) {
            ui.print("Survivors: " + namesOrNone(ending.survivorNames()));
            ui.print("Lost: " + namesOrNone(ending.deadNames()));
        } else {
            ui.print("Lost on the expedition: " + namesOrNone(ending.deadNames()));
            // dies on the ship
            ui.print("Lost with the ship: " + namesOrNone(ending.survivorNames()));
        }
        
        // write run stats to a log
        log.logEnding(ending);
        log.appendScore(ending);
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
        
        // hull reaches 0% hull failure
        // not actually a reachable end until project 2
        if (state.ship().hullPercent() <=0) {
            state.endRun(EndingReason.HULL_FAILURE);
        }
    }
}
