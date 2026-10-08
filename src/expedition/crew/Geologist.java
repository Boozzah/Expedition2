/*
 * Geologist
 * Crew role. Implements VeinInsight — reports how many levels remain
 * in a vein and stops the drill at the true bottom, so the team comes up
 * safely instead of drilling into nothing.
 */
package expedition.crew;
import expedition.crew.hooks.VeinInsight;

public class Geologist extends CrewMember implements VeinInsight {

    public Geologist(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }

    @Override
    public String describeRemaining(int levelsLeft) {
        // Geologist's ability shows levels remaining in vein
        return getName() + " (Geologist): " + levelsLeft + " level(s) left in this vein.";
    }

    @Override
    public boolean shouldAutoEject(int levelsLeft) {
        // auto ejects at the bottom - 0 levels left so leave automatically
        return levelsLeft <= 0;
    }

    @Override
    public Role getRole() {
        return Role.GEOLOGIST;
    }
}
