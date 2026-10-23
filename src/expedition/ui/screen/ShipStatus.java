/*
 * ShipStatus
 * What the ship screens need to draw: the fuel gauge, each part's health, the cost of
 * the next jump, what is in the hold and who aboard can be given an item.
 */
package expedition.ui.screen;

import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.ship.FuelEconomy;
import expedition.ship.Ship;
import expedition.ship.ShipPart;
import java.util.List;
import java.util.Map;

public record ShipStatus(int fuel, int tankCapacity, Map<ShipPart, Integer> parts, int hullPercent,
                         int nextJumpCost, List<ItemInfo> items, int holdCapacity, List<CrewCard> crew) {

    public ShipStatus {
        parts = Map.copyOf(parts);
        items = List.copyOf(items);
        crew = List.copyOf(crew);
    }

    public static ShipStatus of(RunState state, GameConfig config) {
        Ship ship = state.ship();
        int cost = FuelEconomy.jumpCost(state.jumpNumber(), ship.hullPercent(), config);
        return new ShipStatus(ship.fuel(), config.tankCapacity(), ship.parts(), ship.hullPercent(),
                cost, ItemInfo.of(ship.inventory()), ship.inventory().capacity(), CrewCard.of(state.livingCrew(), config));
    }

    public boolean canAffordJump() {
        return fuel >= nextJumpCost;
    }

    public boolean hasItem(String name) {
        for (ItemInfo item : items) {
            if (item.name().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
