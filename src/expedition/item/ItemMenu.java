/*
 * ItemMenu
 * The use-item screen. Lists what is in the hold, asks which item to use and,
 * if it needs one, which crew member to use it on, then says what it did.
 * Shared by the drill menu and the jump prompt so items work the same in both.
 */
package expedition.item;

import expedition.crew.CrewMember;
import expedition.engine.RunState;
import expedition.expedition.Expedition;
import expedition.hazard.effect.EffectContext;
import expedition.ship.Inventory;
import expedition.ship.ShipPart;
import expedition.ui.GameView;
import expedition.ui.screen.Choices;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemMenu {

    private final RunState state;
    private final GameView view;

    public ItemMenu(RunState state, GameView view) {
        this.state = state;
        this.view = view;
    }

    // uses one item. team is the expedition when this is opened down on the surface,
    // so only the people down there can be given something. null means aboard the
    // ship, where anyone still alive can be
    public void open(Expedition team) {
        Inventory bag = state.ship().inventory();
        if (bag.isEmpty()) {
            view.print("The hold is empty.");
            return;
        }

        // one line per kind of item, so 3 Caffeine is one option not three
        Map<String, Integer> stacks = bag.stacked();
        List<String> names = new ArrayList<>(stacks.keySet());

        view.print("");
        view.print("--- Hold (" + bag.size() + "/" + bag.capacity() + ") ---");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            view.print("  " + (i + 1) + ") " + name + " x" + stacks.get(name) + " - " + firstNamed(bag, name).description());
        }
        view.print("  " + (names.size() + 1) + ") Back");

        int pick = view.readInt("Use which item? ", 1, names.size() + 1);
        if (pick == GameView.BACK || pick == names.size() + 1) {
            return;
        }

        String name = names.get(pick - 1);
        Item item = firstNamed(bag, name);

        CrewMember target = null;
        if (item.needsCrewTarget()) {
            List<CrewMember> crew = (team == null) ? state.livingCrew() : team.livingMembers();
            target = chooseTarget(item, crew);
            // backed out, nothing has been taken out of the hold yet so the item is kept
            if (target == null) {
                return;
            }
        }

        use(name, target, team);
    }

    // What the player picked on screen. ASK means the view couldn't pick one itself (the
    // console), so the numbered menu asks instead
    public void use(Choices.ItemUse choice, Expedition team) {
        if (choice.ask()) {
            open(team);
            return;
        }
        CrewMember target = null;
        for (CrewMember m : state.livingCrew()) {
            if (m.getName().equals(choice.crewName())) {
                target = m;
            }
        }
        use(choice.itemName(), target, team);
    }

    // Uses one of the named item on target (null for an item that fixes the ship), and says
    // what it did. This is the part the window calls straight away, it picks the item and the
    // crew member itself
    public void use(String name, CrewMember target, Expedition team) {
        Inventory bag = state.ship().inventory();
        Item item = firstNamed(bag, name);
        if (item == null) {
            return;
        }
        if (item.needsCrewTarget() && target == null) {
            view.print("  Choose who to give the " + name + " to.");
            return;
        }

        // only taken out now it is definitely being used
        bag.take(name);

        // everything an item can change, before it is used
        int fuel = state.ship().fuel();
        Map<ShipPart, Integer> parts = state.ship().parts();
        int health = (target == null) ? 0 : target.getHealth();
        int stamina = (target == null) ? 0 : target.getStamina();
        int composure = (target == null) ? 0 : target.getComposure();

        // session is null, items don't touch the oil being carried
        item.use(new EffectContext(state, team, null), target);

        String changes = "";
        changes += change(changes, "fuel", state.ship().fuel() - fuel);
        for (ShipPart part : ShipPart.values()) {
            changes += change(changes, part.label().toLowerCase(), state.ship().part(part) - parts.get(part));
        }
        if (target != null) {
            changes += change(changes, "health", target.getHealth() - health);
            changes += change(changes, "stamina", target.getStamina() - stamina);
            changes += change(changes, "composure", target.getComposure() - composure);
        }

        // a First Aid Kit on someone at full health would just be wasted, so it goes back
        if (changes.isEmpty()) {
            bag.add(item);
            view.print("  That would do nothing right now. The " + name + " goes back in the hold.");
            return;
        }

        String on = (target == null) ? "" : " on " + target.getName();
        view.print("  Used the " + name + on + ": " + changes);
    }

    // numbered list of who the item can go to, with their stats so the player can see who needs it.
    // returns null if the player goes back
    private CrewMember chooseTarget(Item item, List<CrewMember> crew) {
        view.print("");
        view.print("Use the " + item.name() + " on who?");
        for (int i = 0; i < crew.size(); i++) {
            CrewMember m = crew.get(i);
            view.print("  " + (i + 1) + ") " + m.getName()
                    + "  Health:" + m.getHealth()
                    + "  Stamina:" + m.getStamina()
                    + "  Composure:" + m.getComposure());
        }
        view.print("  " + (crew.size() + 1) + ") Back");

        int pick = view.readInt("Choose a crew member: ", 1, crew.size() + 1);
        if (pick == GameView.BACK || pick == crew.size() + 1) {
            return null;
        }
        return crew.get(pick - 1);
    }

    // every item with the same name is the same, so the first one stands in for the stack
    private Item firstNamed(Inventory bag, String name) {
        for (Item item : bag.items()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }

    // same format as the hazard report, "stamina +20, composure +15"
    private String change(String soFar, String label, int change) {
        if (change == 0) {
            return "";
        }
        String sep = soFar.isEmpty() ? "" : ", ";
        String sign = (change > 0) ? "+" : "";
        return sep + label + " " + sign + change;
    }
}
