/* 
    HazardSet
    Which group a particular hazard belongs to. UNIVERSAL hazards can fire on any planet
    if you are on a certain planet only certain hazards will happen.
    ASCENT hazards are unique to when you are returning to orbit.
*/
package expedition.hazard;

public enum HazardSet {
    UNIVERSAL, FROZEN, DESERT, VOLCANIC, TOXIC, BARREN, STORM, ASCENT
}

