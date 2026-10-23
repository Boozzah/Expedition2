/*
 * LevelResult
 * What one drill level turned up: the oil, the points it was worth and any items found.
 */
package expedition.ui.screen;

import java.util.List;

public record LevelResult(int depth, int oil, int points, List<String> itemsFound) {

    public LevelResult {
        itemsFound = List.copyOf(itemsFound);
    }
}
