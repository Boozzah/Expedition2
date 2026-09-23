package expedition.engine;

import expedition.crew.CrewMember;
import expedition.world.Planet;
import expedition.ship.Ship;
import expedition.expedition.Expedition;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

/*
 * RunState
 * The single source of truth for a run in progress — ship, crew, route,
 * RNG seed, current position, and how the run ended.
 */

public class RunState  {
    
    private final long seed;
    private final Random rng;
    private final Ship ship;
    private final List<CrewMember> crew;
    private final List<Planet> route;
    private final GameConfig config;
    
    private int planetIndex = 0;
    private Ending ending = null;
    private int totalOilCollected = 0;
    
    public RunState(long seed, Ship ship, List<CrewMember> crew, List<Planet> planetPool, int planetsPerRun, GameConfig config) {
        this.seed = seed;
        this.rng = new Random(seed);
        this.ship = ship;
        this.crew = crew;
        this.config = config;
        
        // draw this run's route from the planet pool
        List<Planet> pool = new ArrayList<>(planetPool); // copy over don't reorder
        
        // shuffles the planets
        Collections.shuffle(pool, rng);
        
        int count = Math.min(planetsPerRun, pool.size()); // guards a short planets.txt
        this.route = new ArrayList<>(pool.subList(0, count));
    }
    
    
    // getters
    public Random rng() { 
        return this.rng; 
    }
    
    public Ship ship() { 
        return this.ship; 
    }
    
    public List<CrewMember> crew() { 
        return this.crew; 
    }
    
    public List<CrewMember> livingCrew() { 
        List<CrewMember> result = new ArrayList<>();
        for (CrewMember member : crew) {
            if (member.isAlive()) {
                result.add(member);
            }
        }
        return result;
    }
    
    // able to go on expidition
    public List<CrewMember> eligibleCrew() {
        List<CrewMember> alive = livingCrew();
        List<CrewMember> result = new ArrayList<>();
        for (CrewMember member : alive) {
            if (member.isEligible()) {
                result.add(member);
            }
        }
        return result;
    }
    public Planet currentPlanet() { 
        if (planetIndex >= route.size()) {
            return null;
        }
        return route.get(planetIndex); 
    }
    
    public int jumpNumber() { 
        return planetIndex+1; 
    }
                

    // --- run progression ---
    public boolean isRunOver() { 
        return ending != null; 
    }
    

    
    public Ending ending() {
        return this.ending;
    }
    
    // builds the ending which holds all of the planets reached, who lived and died, and the total oil that was collected
    public void endRun(EndingReason reason) { 
        // this makes sure that this isnt a duplicate ending and that its not overwritten - saftey protocol 
        if (ending != null) return;
        
        List<String> survivors = new ArrayList<>(); 
        List<String> dead = new ArrayList<>(); 
        
        // loops through all the crew members and checks if they are alive - if they are alive you add them to survivors List
        for (CrewMember member : crew)
        {
            if(member.isAlive())
            {
                survivors.add(member.getName());
            } 
            // if they are not alive they are added to the empty dead list 
            else 
            {
                dead.add(member.getName()); 
            }
        }
        
        // ending uses these values - planet index + 1 is because I want it to count to what planet you have seen rather than the last planet you have left. You do not need 
        // to have drilled into the planet for it to count. so the plus 1 just means that instead of the first planet you start at being 0 and then you go to the next and you
        // finally see you have been to 1, you have already seen 1 at the start so it counts using the + 1
        // But on a HOME ending planetIndex has already advanced past the end of the route,
        // so cap it at the route length.
        int reached = Math.min(planetIndex + 1, route.size());
        ending = new Ending(reason, reached, survivors, dead, totalOilCollected); 
    }
    public void advancePlanet(Expedition team) {
        List<CrewMember> wentDown = (team == null) ? List.of() : team.members();
        
        // regenerate stats
        for (CrewMember member : livingCrew()) {
            if (!wentDown.contains(member)) {
                member.gainHealth(config.healthRestRecovery());
                member.gainStamina(config.staminaRestRecovery());
                member.gainComposure(config.composureRestRecovery());
            }
        }
        planetIndex++;
    }

    // Kills a crew member AND applies the composure penalty to every other
    // living crew member. This lives here so exactly one place in the program
    // knows what a death costs.
    public void killCrew(CrewMember victim) { 
        victim.die();
        for (CrewMember member: livingCrew()) {
            member.loseComposure(config.composureDeathPenalty());
        }
    }

    //random crew selection, used heavily by hazard effects
    
    // return one random living crew member
    public CrewMember randomLivingCrew() {
        List<CrewMember> pool = livingCrew();
        if (pool.isEmpty()) return null;
        return pool.get(rng.nextInt(0, pool.size()));
    }
    
    // return n random living crew members
    public List<CrewMember> randomLivingCrew(int n) {
        List<CrewMember> pool = livingCrew();
        List<CrewMember> result = new ArrayList<>();
        
        for (int i = 0; i < n; i++) {
            if (pool.isEmpty()) break;
            CrewMember member = pool.get(rng.nextInt(0, pool.size()));
            pool.remove(member);
            result.add(member);
        }
        return result;
    }
    
    // Oil banked over the whole run. DrillSession reports each haul here
    public int addOil(int amount) {
        totalOilCollected += amount;
        return ship.addFuel(amount);
    }
    
    public int totalOilcollected() {
        return totalOilCollected;
    }
    
}
