/*
 * ItemDefinition
 * One line of items.txt (or one row of the ITEMS table) - what an item is
 * and how often it drops. It is a recipe, not the item itself: every time one
 * is found, ItemFactory makes a fresh Item from it.
 */
package expedition.item;

// amount is the main value (health, stamina, composure, hull % or fuel depending on kind)
// bonus is only used by the Stimulant, for White Monster's extra composure - 0 for everything else
public record ItemDefinition(String name, ItemKind kind, int amount, int bonus, int dropWeight, String description) {

}
