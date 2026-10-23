/*
 * CrewCard
 * One crew member as the crew selection screen and the drill screen's ID cards show them.
 * ineligibleReason is null when they can go down, otherwise the one reason they can't.
 * fatiguedAfterTrip warns that going down would leave them too tired to use their ability.
 */
package expedition.ui.screen;

import expedition.crew.CrewMember;
import expedition.crew.Role;
import expedition.engine.GameConfig;
import java.util.ArrayList;
import java.util.List;

public record CrewCard(String name, Role role, int health, int stamina, int composure, boolean alive,
                       String ineligibleReason, boolean fatiguedAfterTrip) {

    public static CrewCard of(CrewMember member, GameConfig config) {
        return new CrewCard(member.getName(), member.getRole(), member.getHealth(), member.getStamina(),
                member.getComposure(), member.isAlive(), member.ineligibleReason(),
                member.fatiguedAfter(config.staminaExpeditionCost()));
    }

    public static List<CrewCard> of(List<CrewMember> members, GameConfig config) {
        List<CrewCard> cards = new ArrayList<>();
        for (CrewMember m : members) {
            cards.add(of(m, config));
        }
        return cards;
    }

    public boolean canGoDown() {
        return ineligibleReason == null;
    }

    // the stat card shows fatigue, which goes up as stamina goes down
    public int fatigue() {
        return 100 - stamina;
    }
}
