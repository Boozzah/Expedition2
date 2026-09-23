/*
 * Miner
 * Crew role. Implements DrillModifier — boosts oil yield per level,
 * compounding with depth. - Only ability implemented in project 1
 */
package expedition.crew;
import expedition.crew.hooks.DrillModifier;

public class Miner extends CrewMember implements DrillModifier{
    // +8% per level of depth compounding for each level
    private static final double BONUS_PER_LEVEL = 0.08;
    
    public Miner(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }
    
    // as you dig further down greater yield
    @Override
    public int modifyYield(int baseYield, int depth) {
        double multiplier = Math.pow(1 + BONUS_PER_LEVEL, depth);
        return (int) Math.round(baseYield * multiplier);
    }
}
