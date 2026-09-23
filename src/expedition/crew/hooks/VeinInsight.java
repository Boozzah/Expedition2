/*
 * VeinInsight
 * Hook implemented by crew who can see how much of a vein is left
 * and auto-eject once it's exhausted (Geologist). Not wired in for project 1
 * will be for project 2
 */
package expedition.crew.hooks;

public interface VeinInsight {
    String describeRemaining(int levelsLeft);
    boolean shouldAutoEject(int levelsLeft);
}
