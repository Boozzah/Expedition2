/*
 * GameView
 *
 * Everything the game needs from a user interface, show a line of text,
 * and ask the player for an input. GameEngine, ExpeditionSelector, DrillSession and RunLogWriter
 * only ever talk to this interface.
 *
 * The screens of a run (arriving at a planet, picking the crew, drilling, jumping...) each
 * have a method that is handed everything the screen shows and returns what the player
 * picked. Each one has a text version here built on print and the read methods, which is
 * what the console uses. SwingView overrides them to draw Nadia's screens instead, so the
 * engine never knows which one it is talking to.
*/
package expedition.ui;

import expedition.engine.HighScore;
import expedition.exception.QuitGameException;
import expedition.io.SaveSlot;
import expedition.ship.ShipPart;
import expedition.ui.screen.Choices;
import expedition.ui.screen.CrewCard;
import expedition.ui.screen.DrillStatus;
import expedition.ui.screen.EndingInfo;
import expedition.ui.screen.ItemInfo;
import expedition.ui.screen.LevelResult;
import expedition.ui.screen.PlanetInfo;
import expedition.ui.screen.Popup;
import expedition.ui.screen.ShipStatus;
import expedition.ui.screen.SiteInfo;
import expedition.util.TextWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface GameView {
    // returned by readInt when the player asks to go back a step
    int BACK = Integer.MIN_VALUE;

    void print(String line);
    int readInt(String prompt, int min, int max) throws QuitGameException;
    String readChoice(String prompt, List<String> options) throws QuitGameException;
    boolean readYesNo(String prompt) throws QuitGameException;

    // Shows the main menu and waits for the player to pick something. The console
    // prints the text menu, the window shows menu screen. canResume is false when
    // there is no saved run, so Resume can't be picked
    MenuChoice showMainMenu(boolean canResume) throws QuitGameException;

    // Shows the best finished runs, best first. The window waits for Back, the console
    // just prints the table and carries on to the menu
    void showHighScores(List<HighScore> scores) throws QuitGameException;

    // Called once when the program is finished, so a window can close itself
    void close();

    // Shows the saved runs, the most recently played first, and returns the one the player
    // picks, or null to go back to the menu. A numbered list unless a view draws its own
    default SaveSlot chooseSave(List<SaveSlot> slots) throws QuitGameException {
        print("");
        print("--- SAVED RUNS ---");
        for (int i = 0; i < slots.size(); i++) {
            SaveSlot slot = slots.get(i);
            print(String.format("  %d) %-20s planet %d   last played %s",
                    i + 1, slot.name(), slot.planetsReached(), slot.lastPlayedText()));
        }
        int pick = readInt("Choose a save: ", 1, slots.size());
        return (pick == BACK) ? null : slots.get(pick - 1);
    }

    // --- outside a run ---

    // the story at the start of a new run
    default void showLore(String title, String text) throws QuitGameException {
        print("");
        print("=== " + title + " ===");
        print(TextWrapper.wrap(text));
    }

    // the pages of How to Play, one after another in the console
    default void showHowToPlay(List<String> pages) throws QuitGameException {
        print("");
        print("=== HOW TO PLAY ===");
        for (String page : pages) {
            print(TextWrapper.wrap(page));
            print("");
        }
    }

    // A run has started or finished. The window's in-game menu uses the controls to show the
    // score and save the game, the console has no in-game menu so it ignores them
    default void runStarted(RunControls controls) {
    }

    default void runEnded() {
    }

    // --- the screens of a run ---

    // Arriving at a planet: go down, stay aboard, or look at the ship first
    default Choices.Arrival arrive(PlanetInfo planet, ShipStatus ship) throws QuitGameException {
        print("");
        print("=== Jump " + planet.jumpNumber() + " of " + planet.routeLength() + " ===");
        String nextJump = "Next jump: " + ship.nextJumpCost() + " fuel";
        if (!ship.canAffordJump()) {
            nextJump += " (" + (ship.nextJumpCost() - ship.fuel()) + " short)";
        }
        print("Fuel: " + ship.fuel() + "/" + ship.tankCapacity() + "   Ship: " + ship.hullPercent() + "%   " + nextJump);
        print("");
        print("=== " + planet.name() + " (" + planet.archetype() + ") ===");
        print(TextWrapper.wrap(planet.flavour()));

        String choice = readChoice("Send an expedition down? Or check the Ship first.", List.of("Yes", "No", "Ship"));
        switch (choice) {
            case "Yes": return Choices.Arrival.DRILL;
            case "No": return Choices.Arrival.STAY_ABOARD;
            default: return Choices.Arrival.SHIP;
        }
    }

    // The ship's health, part by part, with the option to repair one
    default Choices.ShipScreen showShip(ShipStatus ship) throws QuitGameException {
        ShipPart[] parts = ShipPart.values();
        print("");
        print("--- Ship ---");
        for (int i = 0; i < parts.length; i++) {
            print(String.format("  %d) %-9s %3d%%", i + 1, parts[i].label(), ship.parts().get(parts[i])));
        }
        print("  " + (parts.length + 1) + ") Done");

        int pick = readInt("Repair which part? ", 1, parts.length + 1);
        if (pick == BACK || pick == parts.length + 1) {
            return Choices.ShipScreen.DONE;
        }
        return new Choices.ShipScreen(parts[pick - 1]);
    }

    // Picks between min and max of the crew to go down. Returns their positions in the
    // list, or null if the player stands the team down
    default List<Integer> chooseCrew(List<CrewCard> crew, int min, int max) throws QuitGameException {
        print("");
        print("--- Crew ---");
        for (CrewCard c : crew) {
            print(String.format("  %-12s %-10s Health:%-4d Stamina:%-4d Composure:%-4d %s",
                    c.name(), c.role().title(), c.health(), c.stamina(), c.composure(), crewStatus(c)));
        }

        List<Integer> chosen = new ArrayList<>();
        while (chosen.size() < max) {
            List<Integer> remaining = new ArrayList<>();
            for (int i = 0; i < crew.size(); i++) {
                if (crew.get(i).canGoDown() && !chosen.contains(i)) {
                    remaining.add(i);
                }
            }
            if (remaining.isEmpty()) {
                break;
            }

            print("");
            print("Select the associated number of the crewmember you want to leave your main ship and descend to the planet surface.");
            for (int i = 0; i < remaining.size(); i++) {
                CrewCard c = crew.get(remaining.get(i));
                print(" " + (i + 1) + ". " + c.name() + "  " + crewStatus(c));
            }

            int pick;
            if (chosen.size() >= min) {
                // enough aboard to launch, so 0 becomes real answer.
                pick = readInt("Choose crewmember " + (chosen.size() + 1) + ", or 0 to launch with " + chosen.size() + ": ", 0, remaining.size());
            } else {
                pick = readInt("Choose crewmember " + (chosen.size() + 1) + " of " + max + ": ", 1, remaining.size());
            }

            // "B" undoes the last pick, and with nobody picked stands the team down
            if (pick == BACK) {
                if (chosen.isEmpty()) {
                    return null;
                }
                CrewCard removed = crew.get(chosen.remove(chosen.size() - 1));
                print(removed.name() + " stood down");
                continue;
            }
            if (pick == 0) {
                break;
            }

            int index = remaining.get(pick - 1);
            chosen.add(index);
            print(" " + crew.get(index).name() + " assigned.");
            // a disabled ability due to low stamina must be stated at selection
            if (crew.get(index).fatiguedAfterTrip()) {
                print(" " + crew.get(index).name() + " will be FATIGUED on this trip. They will be too tired to use their ability");
            }
        }
        return chosen;
    }

    // the one reason a crew member can't go, or READY
    private static String crewStatus(CrewCard c) {
        if (!c.canGoDown()) {
            return c.ineligibleReason();
        }
        // checked after the trip's stamina cost, since that's when the ability would be used
        if (c.fatiguedAfterTrip()) {
            return "FATIGUED, ability unavailable";
        }
        return "READY";
    }

    // Picks a site to drill. moveCost is the fuel it costs to move to one, 0 for the first
    // site on a planet. Returns its position in the list, or BACK to leave the planet
    default int chooseSite(List<SiteInfo> sites, int moveCost) throws QuitGameException {
        print("");
        print("--- Drill sites ---");
        for (int i = 0; i < sites.size(); i++) {
            SiteInfo s = sites.get(i);
            String detail = s.open()
                    ? "  [" + s.tier() + "]  est. " + s.estimatedOil() + "  hazard " + s.riskStart() + "% rising to " + s.riskDeep() + "%"
                    : "  CLOSED";
            print("  " + (i + 1) + ". " + s.name() + detail);
        }
        if (moveCost > 0) {
            print("Moving the team to a site costs " + moveCost + " fuel. B leaves the planet.");
        } else {
            print("B leaves the planet.");
        }

        while (true) {
            int pick = readInt("Choose a site: ", 1, sites.size());
            if (pick == BACK) {
                return BACK;
            }
            if (sites.get(pick - 1).open()) {
                return pick - 1;
            }
            print("That site is closed.");
        }
    }

    // One turn at the drill site: drill, ascend, use an item or move to another site
    default Choices.Drill drillTurn(DrillStatus status) throws QuitGameException {
        print("");
        print("Next Level: " + status.nextLevel() + "   Current haul: " + status.haul() + " oil"
                + "   Hazard next level: " + status.hazardPercent() + "%"
                + "   Item chance: " + status.itemPercent() + "%");

        String tank = "Tank: " + status.fuel() + "/" + status.tankCapacity();
        if (status.oilAboard() > 0) {
            tank += " + " + status.oilAboard() + " oil on the shuttle";
        }
        int space = Math.max(0, status.tankCapacity() - status.fuel() - status.oilAboard());
        tank += " (" + space + " space)";
        if (status.spill() > 0) {
            tank += " - " + status.spill() + " oil would spill";
        }
        print(tank + "   Next jump: " + status.nextJumpCost() + " fuel");
        for (String insight : status.insights()) {
            print(insight);
        }

        // GDD 10: items can be used between drill levels. Only offered when there
        // is something in the hold, so an empty hold doesnt add a dead option
        boolean hasItems = !status.items().isEmpty();
        print(hasItems ? "1) Drill  2) Ascend  3) Use item  4) Another site" : "1) Drill  2) Ascend  3) Another site");
        int choice = readInt("Choose an option: ", 1, hasItems ? 4 : 3);

        if (choice == 1) {
            return Choices.Drill.of(Choices.DrillAction.DRILL);
        }
        if (hasItems && choice == 3) {
            return new Choices.Drill(Choices.DrillAction.USE_ITEM, Choices.ItemUse.ASK);
        }
        if (choice == (hasItems ? 4 : 3)) {
            return Choices.Drill.of(Choices.DrillAction.OTHER_SITE);
        }
        // 2 and Back both take the team back up to the ship
        return Choices.Drill.of(Choices.DrillAction.ASCEND);
    }

    // what the level just drilled turned up
    default void showLevelResult(LevelResult result) {
        print("Level " + result.depth() + ": " + result.oil() + " oil, +" + result.points() + " points.");
        for (String item : result.itemsFound()) {
            print("  Found: " + item);
        }
    }

    // GDD 10: a full hold turns finding an item into a choice - leave it, or throw
    // something out for it. Returns the name of the item to throw out, or null to leave
    // the new one behind
    default String makeRoom(String found, List<ItemInfo> hold, int capacity) throws QuitGameException {
        print("  The hold is full (" + capacity + " items).");
        print("  1) Leave the " + found + " behind");
        for (int i = 0; i < hold.size(); i++) {
            print("  " + (i + 2) + ") Throw out one " + hold.get(i).name() + " (have " + hold.get(i).count() + ")");
        }

        int choice = readInt("Choose an option: ", 1, hold.size() + 1);
        // 1 and Back both leave the new item, so nothing is thrown away by accident
        if (choice < 2) {
            return null;
        }
        return hold.get(choice - 2).name();
    }

    // a hazard, a jump event or a message, and what it did
    default void showPopup(Popup popup) throws QuitGameException {
        print("");
        if (!popup.title().isEmpty()) {
            print(popup.title());
        }
        if (!popup.text().isEmpty()) {
            print(TextWrapper.wrap(popup.text()));
        }
        for (String line : popup.lines()) {
            print("  " + line);
        }
    }

    // The jump prompt. The player can use items first (GDD 10: items can be used before a jump),
    // which matters when the ship is short on fuel - Condensed Oil might be enough to make it
    default Choices.Jump jumpScreen(ShipStatus ship) throws QuitGameException {
        print("");
        if (!ship.canAffordJump()) {
            print("The ship doesn't have enough fuel for the next jump. You require: " + ship.nextJumpCost() + " fuel");
        }
        List<String> options = new ArrayList<>();
        options.add("Jump");
        if (!ship.items().isEmpty()) {
            options.add("Use item");
        }
        String choice = readChoice("The jump costs " + ship.nextJumpCost() + " fuel, you have " + ship.fuel() + ". Type: ", options);
        return choice.equals("Jump") ? Choices.Jump.JUMP : new Choices.Jump(Choices.ItemUse.ASK);
    }

    // The ship ran out of fuel part way through the jump. Returns true to go back in time to
    // the last planet, which is only offered when there is still oil to drill there
    default boolean outOfFuel(int fuel, int jumpCost, boolean canGoBack, int pointsCost) throws QuitGameException {
        print("");
        print("You ran out of fuel... the jump needed " + jumpCost + " and the tank only had " + fuel + ".");
        if (!canGoBack) {
            return false;
        }
        String choice = readChoice("Go back in time to the last planet for " + pointsCost + " points, or end the game?",
                List.of("Back in time", "End game"));
        return choice.equals("Back in time");
    }

    // how the run ended, who made it, and where the score came from
    default void showEnding(EndingInfo ending) throws QuitGameException {
        print("");
        print(TextWrapper.wrap(ending.story()));
        print("");
        print("Planets reached: " + ending.planetsReached());
        print("Oil collected: " + ending.oilCollected());
        if (ending.anyoneSurvived()) {
            print("Survivors: " + namesOrNone(ending.survivors()));
            print("Lost: " + namesOrNone(ending.lost()));
        } else {
            print("Lost on the expedition: " + namesOrNone(ending.lost()));
            // dies on the ship
            print("Lost with the ship: " + namesOrNone(ending.survivors()));
        }
        print("");
        for (Map.Entry<String, Integer> line : ending.breakdown().entrySet()) {
            String sign = (line.getValue() > 0) ? "+" : "";
            print(String.format("  %-20s %s%d", line.getKey(), sign, line.getValue()));
        }
        print("Score: " + ending.score());
    }

    // helper method - "nobody" rather than an empty line
    private static String namesOrNone(List<String> names) {
        return names.isEmpty() ? "nobody" : String.join(", ", names);
    }
}
