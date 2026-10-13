/*
 * ItemFinder
 * Hook implemented by crew who change item drop chance or item
 * weighting (Surveyor, Engineer). Used by DrillSession when it rolls for an item.
 */
package expedition.crew.hooks;

public interface ItemFinder {
    double modifyDropChance(double base);
    int modifyDropWeight(String itemName, int baseWeight);
}
