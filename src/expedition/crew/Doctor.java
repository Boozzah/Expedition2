/*
 * Doctor
 * Crew role. Implements FieldMedic — can treat an injured crewmate
 * once per drill session.
 */
package expedition.crew;

import expedition.crew.hooks.FieldMedic;

public class Doctor extends CrewMember implements FieldMedic {
    
    // Doctor's ability can heal 20 per drill session
    private static final int HEAL_AMOUNT = 20;
    private static boolean used = false;
    
    // "Once per session" tracked with a flag rather than a counter, since
    // it's a single use. Currently his ability is not wired in as it will be apart of project 2.

    public Doctor(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }
    
    // use this to reset heal proc
    public void resetForSession() {
        used = false;
    }
    
    @Override
    public boolean canTreat() {
        return !used;
    }
    
    // implement ability here in project 2
    @Override
    public void treat(CrewMember target) {
        if (used) {
            return;
        }
        target.gainHealth(HEAL_AMOUNT);
        used = true;
    }
}
