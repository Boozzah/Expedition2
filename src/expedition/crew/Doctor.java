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

    // "Once per session" tracked with a flag rather than a counter, since
    // it's a single use. Not static, so the flag belongs to this doctor and not
    // to the Doctor class as a whole
    private boolean used = false;

    public Doctor(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }

    // DrillSession calls this at the start of every session so the heal is available again
    @Override
    public void resetForSession() {
        used = false;
    }

    // false once the heal has been used this session
    @Override
    public boolean canTreat() {
        return !used;
    }

    // heals the target once, then does nothing until the next session.
    // Nobody can be healed back from the dead
    @Override
    public void treat(CrewMember target) {
        if (used || !target.isAlive()) {
            return;
        }
        target.gainHealth(HEAL_AMOUNT);
        used = true;
    }

    @Override
    public Role getRole() {
        return Role.DOCTOR;
    }
}
