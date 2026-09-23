/*
 * ItemFinder
 * Hook implemented by crew who change item drop chance or item
 * weighting (Surveyor, Engineer). Not wired in for project 1, part of project 2
 */
package expedition.crew.hooks;

public interface ItemFinder {
    double modifyDropChance(double base);
    int modifyDropWeight(String itemName, int baseWeight);
}
