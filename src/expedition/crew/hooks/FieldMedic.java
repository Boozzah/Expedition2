/*
 * FieldMedic
 * Hook implemented by crew who can treat an injured crewmate
 * mid-expedition (Doctor). DrillSession resets it at the start of each
 * drill session and asks it to treat someone after a hazard hurts the team.
 */
package expedition.crew.hooks;
import expedition.crew.CrewMember;

public interface FieldMedic {
    // true if the heal hasn't been used yet this session
    boolean canTreat();

    // heals the target, only works once per session
    void treat(CrewMember target);

    // makes the ability available again, called at the start of every drill session
    void resetForSession();

    // so the game can say who did the treating. CrewMember already has this
    // method, so a crew member that implements this hook gets it for free
    String getName();
}
