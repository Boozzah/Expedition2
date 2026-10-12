/*
 * HaulHolder
 * Whatever is carrying the team's oil when a hazard fires. DrillSession
 * while they are drilling, AscentPhase on the climb back up. OIL_PCT and
 * CLOSE_SITE call these, so the same effects work in both places without
 * knowing which one they are in.
 */
package expedition.expedition;

public interface HaulHolder {
    // takes a percentage of the oil the team is carrying
    void reduceHaulByPercent(int percent);

    // closes the site the team is drilling at and ends the session
    void closeSiteAndEnd();
}
