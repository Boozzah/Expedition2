/*
 * Role
 * The six crew roles: DOCTOR, SURVEYOR, ENGINEER, MINER, PILOT,
 * GEOLOGIST. Each has a line saying what its ability does, for the crew
 * selection screen's stat card.
 */
package expedition.crew;

public enum Role {
    DOCTOR("Doctor", "Treats an injured crew member once at each drill site"),
    SURVEYOR("Surveyor", "Finds items more often while drilling"),
    ENGINEER("Engineer", "Finds Repair Kits more often"),
    MINER("Miner", "Gets more oil out of every level"),
    PILOT("Pilot", "Flies the shuttle up without the ship taking damage"),
    GEOLOGIST("Geologist", "Can see how many levels the vein is");

    private final String title;
    private final String ability;

    Role(String title, String ability) {
        this.title = title;
        this.ability = ability;
    }

    // "Geologist" rather than GEOLOGIST, for the screens
    public String title() {
        return title;
    }

    public String ability() {
        return ability;
    }
}
