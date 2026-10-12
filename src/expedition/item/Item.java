/*
 * Item
 * Base type for all carried items: name, description, and the
 * polymorphic use() effect when consumed.
 */
package expedition.item;

import expedition.hazard.effect.EffectContext;
import expedition.crew.CrewMember; 

public abstract class Item 
{
    private final String name; 
    private final String description; 
    
    protected Item(String name, String description)
    {
        this.name = name;
        this.description = description; 
    }
    
    public String name()
    {
        return name; 
    }
    
    public String description()
    {
        return description; 
    }
    
    public abstract void use(EffectContext ctx, CrewMember target); 
    
    public abstract boolean needsCrewTarget(); 
    
    
}
