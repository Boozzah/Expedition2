/*
 * AscentModifier
 * Hook implemented by crew who can modify a HazardOutcome rolled
 * during ascent (Pilot). Not wired for project 1, part of project 2
 */
package expedition.crew.hooks;
import expedition.hazard.HazardOutcome;

public interface AscentModifier {
    HazardOutcome modifyAscent(HazardOutcome base);
}
