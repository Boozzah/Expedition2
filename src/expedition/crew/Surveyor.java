/*
 * Surveyor
 * Crew role. Implements ItemFinder — improves the chance of finding
 * an item while drilling.
 */
package expedition.crew;

import expedition.crew.hooks.ItemFinder;

public class Surveyor extends CrewMember implements ItemFinder {
    // +7% drop chance per level
    private static final double DROP_CHANCE_BONUS = 0.07;
    
    public Surveyor(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }
    
    @Override
    public double modifyDropChance(double base) {
        return base + DROP_CHANCE_BONUS;
    }
    
    @Override
    public int modifyDropWeight(String itemName, int baseWeight) {
        //surveyor only affects drop chance, not weight - must still implement
        return baseWeight;
    }
    
    @Override
    public Role getRole() {
        return Role.SURVEYOR;
    }
    
}
