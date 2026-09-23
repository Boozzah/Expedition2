/*
 * Stimulant
 * Item. Restores a crew member's stamina (Caffeine, White Monster,
 * etc — same class, different values).
 * TO BE IMPLEMENTED IN PROJECT 2
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember; 

public class Stimulant extends Item
{
    private final int staminaAmount; 
    private final int composureAmount; 
    
    public Stimulant(String name, String description, int staminaAmount, int composureAmount)
    {
        // caffeine doesnt increase any compusure but 0 can be passed through here so thats not an issue
        super(name, description); 
        this.staminaAmount = staminaAmount;
        this.composureAmount = composureAmount; 
    }
    
    @Override
    public void use(EffectContext ctx, CrewMember target)
    {
        target.gainStamina(staminaAmount);
        target.gainComposure(composureAmount);
    }
    
    // so it knows it doesnt apply to all crew and its only the targeted crew member
    @Override 
    public boolean needsCrewTarget()
    {
        return true; 
    }
}
