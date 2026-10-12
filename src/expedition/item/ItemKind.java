/*
 * ItemKind
 * Which Item subclass a line in items.txt turns into. ItemFactory is the
 * only place that knows which kind means which class.
 */
package expedition.item;

public enum ItemKind {
    MEDKIT,
    STIMULANT,
    COMFORT,
    REPAIR,
    FUEL
}
