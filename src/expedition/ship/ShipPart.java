/*
 * ShipPart
 * The five parts of the ship shown on the ship health screen. Each one has its own
 * percentage, and damage from the climb back up lands on one of them at a time.
 */
package expedition.ship;

public enum ShipPart {
    HULL("Hull"),
    ENGINE("Engine"),
    EXHAUST("Exhaust"),
    PRESSURE("Pressure"),
    WINDOWS("Windows");

    // how the part is written on screen and in the hazard report
    private final String label;

    ShipPart(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
