/*
 * Engineer
 * Crew role. Implements ItemFinder — doubles the drop weight of
 * Repair Kits. Ability is going to be apart of project 2, currently does nothing in the game
 */
package expedition.crew;
import expedition.crew.hooks.ItemFinder;

public class Engineer extends CrewMember implements ItemFinder {
    private static final String REPAIR_KIT_NAME = "Repair kit";
    
    public Engineer(String name, int health, int stamina, int composure) {
        super(name, health, stamina, composure);
    }
    
    // changes the chance of something dropping while drilling
    @Override
    public double modifyDropChance(double base) {
        //Engineer only affects weight, not chance
        return base;
    }
    
    // changes the weight of drop items while drilling
    @Override
    public int modifyDropWeight(String itemName, int baseWeight) {
        if (REPAIR_KIT_NAME.equals(itemName)) {
            return baseWeight * 2;
        }
        return baseWeight;
    }
    
    @Override
    public Role getRole() {
        return Role.ENGINEER;
    }
}
