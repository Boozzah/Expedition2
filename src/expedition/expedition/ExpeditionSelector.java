/*
 * ExpeditionSelector
 * The crew selection: hands the roster to the view's crew screen, checks the player's
 * 2-3 picks, and charges the stamina cost of going down
 * returns null when too few crew are fit and the planet cannot be drilled at all
*/

package expedition.expedition;

import expedition.crew.CrewMember;                                                    
import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.exception.QuitGameException;
import expedition.ui.GameView;
import expedition.ui.screen.CrewCard;
import expedition.ui.screen.Popup;

import java.util.ArrayList;
import java.util.List;



public class ExpeditionSelector {
    private final static int MIN_CREW = 2;
    private final static int MAX_CREW = 3;

    // AI assisted - Claude Code gave the step by step plan to move this class over to GameView
    private final GameView view;
    private final GameConfig config;
    
    public ExpeditionSelector(GameView view, GameConfig config) {
        this.view = view;
        this.config = config;
    }
    
    // returns the chosen expedition, or null if this planet cannot be drilled due to not enough crew,
    // or the player stood the team down
    public Expedition select(RunState state) throws QuitGameException {
        List<CrewMember> available = state.eligibleCrew();
        
        if (available.size() < MIN_CREW) {
            String who = available.isEmpty() ? "Nobody is fit to deploy." : "Only 1 crew member is fit to deploy.";
            view.showPopup(Popup.info(who + " This planet cannot be drilled. You have to jump on the fuel you have."));
            return null;
        }
        
        // the view shows the whole crew, dead and unfit ones included, so the picks are
        // positions in state.crew()
        List<Integer> picks = view.chooseCrew(CrewCard.of(state.crew(), config), MIN_CREW, MAX_CREW);
        if (picks == null) {
            return null;
        }
        
        // checked again here so a view can't send down someone who isn't fit to go
        List<CrewMember> chosen = new ArrayList<>();
        for (int i : picks) {
            CrewMember member = state.crew().get(i);
            if (available.contains(member) && !chosen.contains(member)) {
                chosen.add(member);
            }
        }
        if (chosen.size() < MIN_CREW || chosen.size() > MAX_CREW) {
            return null;
        }
        
        // The cost of going down
        for (CrewMember member : chosen) {
            member.loseStamina(config.staminaExpeditionCost());
        }
        
        return new Expedition(chosen);
    }
}
