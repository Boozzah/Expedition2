/*
 * Choices
 * What the player picked on each screen, handed back to the engine.
 *
 * ItemUse is an item and who to give it to. A view that can't pick an item itself
 * (the console) hands back ItemUse.ASK, and the item menu asks with a numbered list.
 */
package expedition.ui.screen;

import expedition.ship.ShipPart;

public final class Choices {

    private Choices() {
    }

    public enum Arrival { DRILL, STAY_ABOARD, SHIP }

    public enum DrillAction { DRILL, ASCEND, USE_ITEM, OTHER_SITE }

    // crewName is null for an item that fixes the ship rather than a person
    public record ItemUse(String itemName, String crewName) {
        public static final ItemUse ASK = new ItemUse(null, null);

        public boolean ask() {
            return itemName == null;
        }
    }

    public record Drill(DrillAction action, ItemUse item) {
        public static Drill of(DrillAction action) {
            return new Drill(action, null);
        }
    }

    // repair is null when the player is done with the ship screen
    public record ShipScreen(ShipPart repair) {
        public static final ShipScreen DONE = new ShipScreen(null);
    }

    // item is null when the player jumps
    public record Jump(ItemUse item) {
        public static final Jump JUMP = new Jump(null);

        public boolean jump() {
            return item == null;
        }
    }
}
