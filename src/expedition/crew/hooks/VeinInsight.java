/*
 * VeinInsight
 * Hook implemented by crew who can see how much of a vein is left
 * and auto-eject once it's exhausted (Geologist). DrillSession shows the
 * reading before every level and checks for the auto-eject after drilling one.
 */
package expedition.crew.hooks;

public interface VeinInsight {
    // the line shown to the player saying how many levels are left
    String describeRemaining(int levelsLeft);

    // true when the vein has run out and the team should head back up
    boolean shouldAutoEject(int levelsLeft);

    // so the game can say who called it. CrewMember already has this
    // method, so a crew member that implements this hook gets it for free
    String getName();
}
