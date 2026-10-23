/*
 * DrillStatus
 * Everything the drill screen shows between levels. levelsLeft is only known when a
 * Geologist is down there, otherwise it is -1 and the screen just shows the level.
 * spill is how much oil would be thrown away because the tank is too full to take it.
 */
package expedition.ui.screen;

import java.util.List;

public record DrillStatus(String siteName, int nextLevel, int levelsLeft, int haul, int oilAboard,
                          int fuel, int tankCapacity, int spill, int hazardPercent, int itemPercent,
                          int nextJumpCost, List<CrewCard> team, List<ItemInfo> items, List<String> insights) {

    public static final int UNKNOWN = -1;

    public DrillStatus {
        team = List.copyOf(team);
        items = List.copyOf(items);
        insights = List.copyOf(insights);
    }
}
