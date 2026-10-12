/*
 * AscentModifier
 * Hook implemented by crew who can modify a HazardOutcome rolled
 * during ascent (Pilot). AscentPhase hands every ascent outcome to
 * each one before it is applied.
 */
package expedition.crew.hooks;
import expedition.hazard.HazardOutcome;

public interface AscentModifier {
    // returns the outcome to apply instead, base itself is never changed
    HazardOutcome modifyAscent(HazardOutcome base);

    // so the game can say who flew them through it. CrewMember already has this
    // method, so a crew member that implements this hook gets it for free
    String getName();
}
