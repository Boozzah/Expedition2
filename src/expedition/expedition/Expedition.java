/*
 * Expedition
 * The 1-3 crew members sent down on one drilling trip.  the five
 * ability lookups below are how every other system finds crew abilities
 * nothing outside this class checks a crew member's role,
 * so adding a seventh role changes no calling code
 *
*/
package expedition.expedition;

import expedition.crew.CrewMember;
import expedition.crew.hooks.AscentModifier;
import expedition.crew.hooks.DrillModifier;
import expedition.crew.hooks.FieldMedic;
import expedition.crew.hooks.ItemFinder;
import expedition.crew.hooks.VeinInsight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Expedition {
    
    private final List<CrewMember> members;
    
    public Expedition(List<CrewMember> members) {
        // copy list of crew members at start of expedition.
        this.members = new ArrayList<>(members);
    }
    
    // list of members in a given expedition
    public List<CrewMember> members() {
        return new ArrayList<>(members);
    }
    
    // number of members in an expedition
    public int size() {
        return members.size();
    }

    // Everyone on this expedition who is still alive.
    public List<CrewMember> livingMembers() {
        List<CrewMember> alive = new ArrayList<>();
        for (CrewMember member : members) {
            if (member.isAlive()) {
                alive.add(member);
            }
        }
        return alive;
    }

    /*
     * n distinct living members of this expedition, chosen at random.
     * Returns fewer than n (or none) if not enough are alive, so callers
     * loop over the result rather than assuming its size.
     *
     * Filters on isAlive() only, NOT on fatigue - unlike the ability
     * lookups below. A fatigued crew member's ability does not happen, but
     * they are still down the hole and can still be hurt by a hazard.
     *
     * rng is the run's single seeded Random, passed in rather
     * than created here, so the same seed always injures the same people.
     */
    public List<CrewMember> randomMembers(int n, Random rng) {
        List<CrewMember> pool = livingMembers();
        List<CrewMember> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (pool.isEmpty()) {
                break;
            }
            // removed from the pool as they're picked, so nobody is chosen twice
            result.add(pool.remove(rng.nextInt(pool.size())));
        }
        return result;
    }
    
    /* ---- ability lookups -------
    * One method per hook interface. Each returns only the living, non fatigued members that
    * implement it so no calling method has to remember to check. An empty list means nobody
    * aboard has that ability. - the Miner, Geologist and Doctor are wired in, the rest are
    * still to come in project 2
    */
    
    public List<DrillModifier> drillModifiers() { return withAbility(DrillModifier.class);}
    public List<AscentModifier> ascentModifiers() { return withAbility(AscentModifier.class);}
    public List<ItemFinder> itemFinders() { return withAbility(ItemFinder.class);}
    public List<VeinInsight> veinInsights() { return withAbility(VeinInsight.class);}
    public List<FieldMedic> fieldMedics() { return withAbility(FieldMedic.class);}
    
    
    // The one that return a list of members that have this ability and have enough stamina to use it
    private <T> List<T> withAbility(Class<T> type) {
        List<T> found = new ArrayList<>();
        for(CrewMember member : members) {
            // asks the crew member if they're fatigued rather than checking a number here,
            // so the fatigue rule only lives in one place (CrewMember)
            if (member.isAlive() && type.isInstance(member) && !member.isFatigued()) {
                found.add(type.cast(member));
            }
        }
        return found;
    }
}
