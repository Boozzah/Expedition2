/*
 * GameEngine
 * Orchestrator for the game. Owns the main run loop (menu, new run,
 * visiting planets, jumping between them, resolving the ending).
 * Delegates all game logic to other classes and contains no game
 * maths of its own.
 */
package expedition.engine;

import expedition.crew.CrewMember;
import expedition.event.EventTable;
import expedition.event.JumpEvent;
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
import expedition.io.SaveSlot;
import expedition.io.SaveStore;
import expedition.io.ScoreStore;
import expedition.expedition.ExpeditionSelector;
import expedition.expedition.Expedition;
import expedition.world.DrillSite;
import expedition.expedition.DrillSession;
import expedition.expedition.AscentPhase;
import expedition.expedition.HazardReport;
import expedition.hazard.effect.EffectContext;
import expedition.io.RunLogWriter;
import expedition.hazard.HazardSet;
import expedition.hazard.Hazard;
import expedition.item.Item;
import expedition.item.RepairKit;
import expedition.ship.Inventory;
import expedition.ship.ShipPart;
import expedition.ui.RunControls;
import expedition.ui.screen.Choices;
import expedition.ui.screen.EndingInfo;
import expedition.ui.screen.PlanetInfo;
import expedition.ui.screen.Popup;
import expedition.ui.screen.ShipStatus;
import expedition.ui.screen.SiteInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameEngine {
    
    // 8 planets in a run
    private static final int ROUTE_LENGTH = 8;
    
    // how many runs the high-score screen lists, as many as fit on Nadia's screen
    private static final int HIGH_SCORES_SHOWN = 8;
    
    // the story shown when a new run starts, from Nadia's lore screen
    private static final String LORE_TITLE = "they are gone...";
    private static final String LORE = "After returning from an expedition, you ascend to the mothership, only to find they are "
            + "not there. You check the location tracker on your ship and you see that they have left beacons planets away. You must "
            + "get home, your family and friends are waiting for you. But there is only so much fuel this ship can hold so you know "
            + "you will have to make some more stops along the way before you get home. This will not be an easy Expedition. "
            + "Good luck on your mission.";
    
    // the pages of the How to Play book, two to a spread. The first two are Nadia's
    private static final List<String> HOW_TO_PLAY = List.of(
            "You have been left by the mothership and your mission is to get back home, you must collect enough fuel to get "
                    + "home. To do that you jump to a planet and decide if you want to drill. Drilling can be risky. You can come",
            "across hazards that cost you fuel, and can risk your crews lives. Every jump costs more than the last and you never "
                    + "know what can happen during your flight. Manage your fuel wisely, protect your ship and your crew. Flip to "
                    + "the next page for details...",
            "At each planet choose Y to send 2 or 3 crew down, or N to stay aboard and save fuel. SHIP shows the health of "
                    + "each part of the ship, and lets you fix one with a Repair Kit.",
            "Pick a site on the planet to drill. Moving to another site costs fuel. The deeper you drill the more oil you "
                    + "find, but the more likely a hazard is. Ascend before it's too late.",
            "Between planets press JUMP and pull the lever. If the fuel runs out on the way you can go back in time to the "
                    + "last planet while it still has oil, for " + ScoreSheet.BACK_IN_TIME_COST + " points.",
            "Points come from oil, planets, items, repairs, deep drilling and getting the crew home. Press the menu button "
                    + "or Esc at any time to save the game, get help or quit.");
    
    // AI assisted - Claude Code gave the step by step plan to move this class over to GameView
    private final GameView view;
    private final GameConfig config;
    private final GameDataSource data;
    private final ExpeditionSelector selector;
    private final RunLogWriter log;
    private final SaveStore saves;
    private final ScoreStore scores;
    private List<Hazard> allHazards = List.of();
    private EventTable events = new EventTable(List.of(), 0);
    
    // the snapshot taken when the ship reached this planet, which is what the in-game
    // menu's Save Game saves. A run can only be rebuilt from a planet arrival
    private SavedRun lastCheckpoint;
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
        view.setHelpTopics(helpTopics());
        boolean playing = true;
        while(playing) {
            // main menu
            MenuChoice choice = view.showMainMenu(hasSave());
            // AI assisted - Claude suggested catching ReturnToMenuException here so leaving a run goes back to the menu
            try {
                switch(choice) {
                    case NEW_GAME:
                        if (confirmNewRun()) {
                            RunState state = startNewRun();
                            view.showLore(LORE_TITLE, LORE);
                            playRun(state);
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
        
        // load hazards and the jump events
        allHazards = data.loadHazards();
        events = new EventTable(data.loadEvents(), config.jumpEventChance());
        
        // every item that can be found this run, used by drops and ITEM hazards
        ItemTable items = new ItemTable(data.loadItems());
        
        return new RunState(seed, ship, crew, planets, ROUTE_LENGTH, config, items);
    }
    
    // There is only one autosave, so a new run will write over it at the first planet.
    // Checks first rather than losing someone's run without warning. Named saves are kept
    private boolean confirmNewRun() {
        if (!hasAutosave()) {
            return true;
        }
        return view.readYesNo("Starting a new run will replace the autosave. Your named saves are kept. Carry on?");
    }
    
    // Builds a run back up from the save the player picks. The content is loaded fresh the same way
    // a new run does it, then the save says which planets, crew stats and items to use.
    // Returns null (back to the menu) if there is no save or it can't be used
    private RunState resumeRun() throws DataLoadException {
        try {
            List<SaveSlot> slots = saves.list();
            if (slots.isEmpty()) {
                view.print("There is no saved run to resume.");
                return null;
            }
            SaveSlot slot = view.chooseSave(slots);
            if (slot == null) {
                return null;
            }
            SavedRun saved = saves.load(slot.id());
            if (saved == null) {
                view.print("That save is no longer there.");
                return null;
            }
            
            allHazards = data.loadHazards();
            events = new EventTable(data.loadEvents(), config.jumpEventChance());
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
    
    private boolean hasAutosave() {
        try {
            for (SaveSlot slot : saves.list()) {
                if (slot.autosave()) {
                    return true;
                }
            }
            return false;
        } catch (SaveException e) {
            return false;
        }
    }
    
    // Saves the run each time the ship arrives at a planet (GDD P2 4: autosave at each planet).
    // checkpoint() runs even if the save fails, so the run's rolls stay the same either way
    private void autosave(RunState state) {
        lastCheckpoint = state.checkpoint();
        try {
            saves.autosave(lastCheckpoint);
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
    
    // adds the finished run to the high-score table
    private void recordScore(Ending ending, ScoreSheet sheet) {
        HighScore score = HighScore.from(ending, sheet.total(), LocalDateTime.now());
        try {
            scores.add(score);
        } catch (SaveException e) {
            view.print("The score could not be saved: " + e.getMessage());
        }
    }
    
    // once a run is over there is nothing left to resume from the autosave
    private void deleteSave() {
        try {
            saves.deleteAutosave();
        } catch (SaveException e) {
            view.print("The finished run's save could not be removed: " + e.getMessage());
        }
    }
    
    // What the in-game menu can do while this run is going. It is called from the window's
    // thread while the engine waits on the player, so it only reads the score and saves the
    // snapshot from the last planet, it never changes the run
    private RunControls controlsFor(RunState state) {
        return new RunControls() {
            @Override
            public int score() {
                return state.score().total();
            }
            
            @Override
            public String save(String name) {
                try {
                    saves.saveAs(name, lastCheckpoint);
                    return "Saved as \"" + name.trim() + "\". Loading it carries on from when the ship reached this planet.";
                } catch (SaveException e) {
                    return "The run could not be saved: " + e.getMessage();
                }
            }
            
            @Override
            public String saveAndQuit() {
                try {
                    saves.autosave(lastCheckpoint);
                    return "Saved.";
                } catch (SaveException e) {
                    return "The run could not be saved: " + e.getMessage();
                }
            }
        };
    }
    
    // The actual gameplay loop
    private void playRun(RunState state) {
        view.runStarted(controlsFor(state));
        try {
            // false after going back in time, the ship is at a planet it has already been to
            boolean newPlanet = true;
            
            // while run is not over, do the gameplay loop
            while (!state.isRunOver()) {
                if (newPlanet) {
                    autosave(state);
                }
                
                // visit the current planet with a team, if the player sends one down
                Planet planet = state.currentPlanet();
                Expedition team = null;
                if (planet != null) {
                    team = visitPlanet(state, planet, newPlanet);
                    checkTerminalStates(state);
                    if (state.isRunOver()) {
                        break;
                    }
                }
                
                newPlanet = jump(state, team);
            }
            reportEnding(state);
            deleteSave();
        } finally {
            view.runEnded();
        }
    }
    
    // The jump to the next planet. The player can use items first (GDD 10: items can be used
    // before a jump), which matters when the ship is short on fuel - Condensed Oil or a Repair
    // Kit might be enough to make it. Returns false if the fuel ran out part way and the
    // player went back in time to this planet instead
    private boolean jump(RunState state, Expedition team) {
        ItemMenu itemMenu = new ItemMenu(state, view);
        while (true) {
            Choices.Jump choice = view.jumpScreen(ShipStatus.of(state, config));
            if (choice.jump()) {
                break;
            }
            // aboard the ship, so anyone alive can be given an item
            itemMenu.use(choice.item(), null);
        }
        
        // calculate jump cost. worked out after the items because a Repair Kit lowers it
        int jumpCost = FuelEconomy.jumpCost(state.jumpNumber(), state.ship().hullPercent(), config);
        
        // Not enough fuel. Going back in time to the last planet is only offered when there
        // is still oil to drill there, otherwise there is nothing to go back for
        if (!state.ship().canAfford(jumpCost)) {
            Planet planet = state.currentPlanet();
            boolean canGoBack = planet != null && !planet.openSites().isEmpty();
            if (view.outOfFuel(state.ship().fuel(), jumpCost, canGoBack, ScoreSheet.BACK_IN_TIME_COST)) {
                state.score().wentBackInTime();
                return false;
            }
            state.endRun(EndingReason.ADRIFT);
            return true;
        }
        
        // spend fuel
        state.ship().spendFuel(jumpCost);
        
        state.advancePlanet(team);
        checkTerminalStates(state);
        
        // if jump was the last jump end run
        if (state.jumpNumber() > ROUTE_LENGTH) {
            state.endRun(EndingReason.HOME);
        } else if (!state.isRunOver()) {
            // something might happen on the way to the next planet
            jumpEvent(state);
            checkTerminalStates(state);
        }
        return true;
    }
    
    // GDD 9: an event on the way to the next planet. Good, bad or just something to see.
    // Reported the same way as a hazard, by comparing the run before and after
    private void jumpEvent(RunState state) {
        JumpEvent event = events.roll(state.rng());
        if (event == null) {
            return;
        }
        
        HazardReport report = new HazardReport(state);
        // nobody is on a planet, so there is no team and nothing carrying oil
        event.applyAll(new EffectContext(state, null, null));
        report.addCrewChanges(null);
        report.addShipChanges(0);
        
        int points = state.score().jumpEvent(event.points());
        if (points != 0) {
            report.add((points > 0 ? "+" : "") + points + " points");
        }
        view.showPopup(new Popup(Popup.Kind.EVENT, "", event.text(), report.lines()));
    }
    
    // AI assisted - Claude Code drafted the help topics from our rules and config
    // The in-game menu's Help, one page per topic so no screen has to explain itself.
    // Built from the config so the numbers always match the game
    private Map<String, String> helpTopics() {
        Map<String, String> topics = new LinkedHashMap<>();
        topics.put("THE GOAL", "The mothership has gone and you have " + ROUTE_LENGTH + " jumps to get home. Every jump burns "
                + "fuel, and the only fuel out here is the oil you drill on the planets along the way. Get home with as "
                + "much of the crew as you can.");
        topics.put("FUEL & JUMPS", "Each jump costs " + config.jumpCostIncrement() + " more fuel than the last, and a damaged "
                + "ship burns more. The tank holds " + config.tankCapacity() + ", so oil past that is lost. The arrival, site "
                + "map and cockpit screens all show the next jump's cost. Press N at a planet to stay aboard and save fuel.");
        topics.put("DRILLING", "Press Y at a planet to send a team down, then click a site to see its risk, oil and hazard "
                + "chance. The first site is free and moving to another costs " + config.siteMoveCost() + " fuel. Each level "
                + "down finds more oil but the hazard chance rises. The oil stays on the shuttle until you ASCEND, and the "
                + "climb can still lose some.");
        topics.put("THE CREW", "Send 2 or 3 crew down. Hover over someone to see their stats and click them to put them in "
                + "the shuttle. Going down costs " + config.staminaExpeditionCost() + " stamina, and a tired crew member can't "
                + "use their ability. Crew who stay aboard rest and recover. Each role's ability is on their card.");
        topics.put("ITEMS & REPAIRS", "Items turn up while drilling. Click a found item to collect it, then use it from ITEMS "
                + "on the drill or cockpit screen. The hold has " + config.inventorySlots() + " slots. SHIP shows each part, "
                + "and a Repair Kit fixes the part you click. A part at 0% destroys the ship.");
        topics.put("BACK IN TIME", "If a jump runs out of fuel you can END GAME, or go BACK IN TIME to the last planet and "
                + "drill it again for " + ScoreSheet.BACK_IN_TIME_COST + " points. It's only there while that planet still "
                + "has oil to drill.");
        topics.put("SCORING", "Points come from the oil you drill, each planet reached, items, repairs, deep drilling, "
                + "drilling a site dry, jump events, getting home and each crew member you bring home. Every death costs "
                + "points. The menu shows your score so far.");
        return topics;
    }

    private void showInstructions() {
        view.showHowToPlay(HOW_TO_PLAY);
    }
    
    // Arriving at a planet. The player can check the ship as many times as they like,
    // then either sends a team down or stays aboard. Returns the team, or null if nobody went.
    // firstVisit is false after going back in time, when the sites are already there
    private Expedition visitPlanet(RunState state, Planet planet, boolean firstVisit) {
        if (firstVisit) {
            // write to run log
            log.log("=== " + planet.name() + " (" + planet.archetype() + ") ===");
            log.log(planet.introFlavour());
            
            // sites are generated on arrival, using the runs single seeded random
            planet.generateSites(state.rng(), config);
        }
        
        while (true) {
            Choices.Arrival choice = view.arrive(PlanetInfo.of(planet, state.jumpNumber(), ROUTE_LENGTH),
                    ShipStatus.of(state, config));
            
            if (choice == Choices.Arrival.SHIP) {
                shipScreen(state);
                continue;
            }
            
            // skipping is a real strategic option - banking fuel instead of drilling
            if (choice == Choices.Arrival.STAY_ABOARD) {
                view.print("You stay aboard and conserve fuel.");
                return null;
            }
            
            // select crew members for expedition. Nobody fit, or the player stood them down,
            // goes back to the arrival screen
            Expedition team = selector.select(state);
            if (team == null) {
                continue;
            }
            
            drillPlanet(state, planet, team);
            return team;
        }
    }
    
    // The team may drill at several sites in one trip down, each is a drill phase. Moving
    // to a site after the first costs fuel, and more hazard exposure, with a bigger haul at
    // stake. Ends when the player leaves the planet or ascends from a site
    private void drillPlanet(RunState state, Planet planet, Expedition team) {
        // the oil stays on the shuttle until the team is back on the ship,
        // so it only goes in the tank after the ascent
        int oilAboard = 0;
        boolean firstSite = true;
        
        while (true) {
            // closed by a hazard, or drilled dry, stays on the map but can't be picked
            if (planet.openSites().isEmpty()) {
                view.print("Every site here is drilled out.");
                break;
            }
            
            int moveCost = firstSite ? 0 : config.siteMoveCost();
            List<DrillSite> sites = planet.sites();
            int pick = view.chooseSite(SiteInfo.of(sites), moveCost, ShipStatus.of(state, config));
            if (pick == GameView.BACK) {
                break;
            }
            DrillSite site = sites.get(pick);
            if (site.isClosed() || site.isExhausted()) {
                continue;
            }
            
            if (!state.ship().canAfford(moveCost)) {
                view.showPopup(Popup.info("Moving the team costs " + moveCost + " fuel, and the ship only has " + state.ship().fuel() + "."));
                continue;
            }
            state.ship().spendFuel(moveCost);
            firstSite = false;
            
            // counts drill phases, which is what raises ascent risk
            planet.incrementExpeditionCount();
            
            DrillSession session = new DrillSession(site, team, state, view, config, hazardsFor(planet), oilAboard);
            int haul = session.run();
            oilAboard += haul;
            view.print("");
            view.print("Hauled " + haul + " oil.");
            
            // a hazard could have killed the last of the crew, or everyone who came down,
            // so there's nobody left to move
            if (state.isRunOver() || team.livingMembers().isEmpty()) {
                break;
            }
            if (session.wantsToAscend()) {
                break;
            }
        }
        
        ascend(state, planet, team, oilAboard);
    }
    
    // The ship health screen. A repair uses one Repair Kit on the part the player picked
    private void shipScreen(RunState state) {
        while (true) {
            Choices.ShipScreen choice = view.showShip(ShipStatus.of(state, config));
            ShipPart part = choice.repair();
            if (part == null) {
                return;
            }
            
            RepairKit kit = findRepairKit(state.ship().inventory());
            if (kit == null) {
                view.showPopup(Popup.info("You don't have a Repair Kit. They can be found while drilling, and the Engineer finds them more often."));
                continue;
            }
            if (state.ship().part(part) >= 100) {
                view.showPopup(Popup.info("The " + part.label().toLowerCase() + " doesn't need repairing."));
                continue;
            }
            
            int scoreBefore = state.score().total();
            state.ship().inventory().take(kit.name());
            int fixed = kit.repair(new EffectContext(state, null, null), part);
            int points = state.score().total() - scoreBefore;
            view.showPopup(Popup.info("Used a " + kit.name() + " on the " + part.label().toLowerCase()
                    + ": +" + fixed + "%, +" + points + " points."));
        }
    }
    
    private RepairKit findRepairKit(Inventory bag) {
        for (Item item : bag.items()) {
            if (item instanceof RepairKit kit) {
                return kit;
            }
        }
        return null;
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
    
    private void reportEnding(RunState state) {
        Ending ending = state.ending(); 
        
        // the breakdown is keyed by label for the screens, in the ScoreSheet's order
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (Map.Entry<ScoreSheet.Reason, Integer> line : state.score().breakdown().entrySet()) {
            breakdown.put(line.getKey().label(), line.getValue());
        }
        
        view.showEnding(new EndingInfo(ending.reason(), endingStory(ending.reason()), ending.planetsReached(),
                ending.totalOilCollected(), ending.survivorNames(), ending.deadNames(), ending.anyoneSurvived(),
                breakdown, state.score().total()));
        
        recordScore(ending, state.score());
        
        // write run stats to a log
        log.logEnding(ending);
    }
    
    // ending texts
    private String endingStory(EndingReason reason) {
        switch (reason) {
            case HOME: 
                return "You see the mothership, its lights through the windows, and you are so excited to be back in the warmth of your home. You dock the ship, and your "
                        + "family is already waiting for you. Tears are streaming down everyones faces. You watch your other crewmates get the same welcoming. You will not "
                        + "be going for another expedition for a while. There will be a celebration tonight but all you care about is that you are home, you are with your "
                        + "family again and you are safe... CONGRATULATIONS YOU HAVE WON THE GAME"; 
            case ADRIFT: 
                return "You search for anywhere fuel could be hidden, maybe you could make fuel out of your food. You try blending your food and putting it into the engine "
                        + "but all that did was waste what you had left. There is no hope. You will never see your family again, never see your home, your friends, or eat your "
                        + "favourite food. You try to break open the window. But all that leaves you with is broken tools, so you sit there hopeless with only one path you "
                        + "can take... GAME OVER, TRY AGAIN"; 
            case HULL_FAILURE:
                return "The ships lights slowly turn off, the alarms sound and you realize that the only piece of home you have left is gone. The only small piece of saftey "
                        + "is breaking apart. You grab the only photo you have of your family, get into bed and try to dream that you made it home. That they welcomed you "
                        + "back. Having missed you for so long. And then you hear the oxygen waring... GAME OVER, TRY AGAIN";
            case CREW_LOST:
                return "There is noone left. The ship sits empty with noone to take it home to the mother ship. Now a floating piece of rubbish in space... GAME OVER, TRY AGAIN";
            default:
                return "The run has ended";
        }
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
