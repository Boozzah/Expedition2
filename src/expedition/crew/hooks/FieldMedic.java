/*
 * FieldMedic
 * Hook implemented by crew who can treat an injured crewmate
 * mid-expedition (Doctor). Not wired in for project 1, will be apart of project 2
 */
package expedition.crew.hooks;
import expedition.crew.CrewMember;

public interface FieldMedic {
    boolean canTreat();
    void treat(CrewMember target);
}
