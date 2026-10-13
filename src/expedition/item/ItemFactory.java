/*
 * ItemFactory
 * Turns an ItemDefinition into the matching Item subclass. Works the same way
 * as CrewFactory so the text file and the database build items the same way.
 */
package expedition.item;

public final class ItemFactory {

    private ItemFactory() {
    }

    // the one place that knows MEDKIT means the MedKit class, everything else
    // just holds an Item and calls use()
    public static Item create(ItemDefinition d) {
        return switch (d.kind()) {
            case MEDKIT -> new MedKit(d.name(), d.description(), d.amount());
            case STIMULANT -> new Stimulant(d.name(), d.description(), d.amount(), d.bonus());
            case COMFORT -> new ComfortItem(d.name(), d.description(), d.amount());
            case REPAIR -> new RepairKit(d.name(), d.description(), d.amount());
            case FUEL -> new FuelItem(d.name(), d.description(), d.amount());
        };
    }
}
