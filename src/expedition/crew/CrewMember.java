/*
 * CrewMember
 * Base type for all six crew roles. Holds health, stamina, composure
 * and alive state, and the clamping logic for changing them. Each
 * role subclass adds its own ability via one of the crew.hooks
 * interfaces.
 */
package expedition.crew;

public abstract class CrewMember {
    
    // max stat needed for gain methods below
    private static final int MAX_STAT = 100;
    
    // crew member can still go on expediton but cannot use their ability
    private static int FATIGUE_THRESHOLD = 50;

    // below any of these a crew member can't go on an expedition at all
    private static final int MIN_HEALTH = 30;
    private static final int MIN_STAMINA = 25;
    private static final int MIN_COMPOSURE = 25;

    // Crew member variables
    private final String name;
    private int health;
    private int stamina;
    private int composure;
    private boolean alive = true;
    
    public CrewMember(String name, int health, int stamina, int composure) {
        this.name = name;
        this.health = health;
        this.stamina = stamina;
        this.composure = composure;
    }
    
    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }


    public int getStamina() {
        return stamina;
    }


    public int getComposure() {
        return composure;
    }
    
    public boolean isAlive() {
        return alive;
    }
    
    // returns boolean if crew member is able to go on expedition
    public boolean isEligible() {
        return ineligibleReason() == null;
    }

    // why this crew member can't go down, or null if they can. The first failing
    // reason wins, so the crew screen shows one clear reason rather than several
    public String ineligibleReason() {
        if (!isAlive()) {
            return "DEAD";
        }
        if (health < MIN_HEALTH) {
            return "Health " + health + ", cannot deploy";
        }
        if (stamina < MIN_STAMINA) {
            return "Stamina " + stamina + ", cannot deploy";
        }
        if (composure < MIN_COMPOSURE) {
            return "Composure " + composure + ", cannot deploy";
        }
        return null;
    }
    
    public void die() {
        alive = false;
    }
    
    
    // --- stat changers --
    public void loseComposure(int n) {
        // if negative set to 0 - same for rest of these ethdos
        composure = Math.max(0, composure - n);
    }
    
    public void gainComposure(int n) {
        // if greater than max set to max -- same for rest of these methods
        composure = Math.min(MAX_STAT, composure + n);
    }
    
    public void loseHealth(int n) {
        health = Math.max(0, health - n);
    }
    
    public void gainHealth(int n) {
        health = Math.min(MAX_STAT, health + n);
    }
    
    public void loseStamina(int n) {
        stamina = Math.max(0, stamina - n);
    }
    
    public void gainStamina(int n) {
        stamina = Math.min(MAX_STAT, stamina + n);
    }
    
    // this crew member can still go on expedition, but their ability does not fire
    // different from isEligible() which is about whether they can go down at all
    public boolean isFatigued() {
        return stamina < FATIGUE_THRESHOLD;
    }
    
    // true if going down would leave this crew member fatigued. The stamina cost comes off
    // when the team launches, so checking isFatigued() at selection missed anyone the cost
    // tips under the threshold, and their ability stopped working with no warning
    // AI assisted - Claude Code fixed the fatigue bug its headless runs found, crew selection checks after the trip's stamina cost
    public boolean fatiguedAfter(int staminaCost) {
        return stamina - staminaCost < FATIGUE_THRESHOLD;
    }
    
    // each subclass returns its own role, so the database can save which role 
    // a crew member is without checking the class with instanceof
    public abstract Role getRole();
}
