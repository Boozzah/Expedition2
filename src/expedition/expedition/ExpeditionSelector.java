/*
 * ExpeditionSelector
 * The crew selection screen: shows the roster with each member's status,
 * takes the player's 2-3 picks, and charges the stamina cost of going down
 * returns null when too few crew are fit and the planet cannot be drilled at all
*/

package expedition.expedition;

import expedition.crew.CrewMember;                                                    
import expedition.engine.GameConfig;
import expedition.engine.RunState;
import expedition.exception.QuitGameException;
import expedition.ui.ConsoleUI;
import expedition.ui.InputValidator;                                                  

import java.util.ArrayList;
import java.util.List;



public class ExpeditionSelector {
    private final static int MIN_CREW = 2;
    private final static int MAX_CREW = 3;
    
    // TODO: these duplicate CrewMember.isEligible()'s thresholds. better would be for
    // crewMember to report its own reason for being ineligible
    private static final int MIN_HEALTH = 30;
    private static final int MIN_STAMINA = 25;
    private static final int MIN_COMPOSURE = 25;
    
    private final ConsoleUI ui;
    private final InputValidator input;
    private final GameConfig config;
    
    public ExpeditionSelector(ConsoleUI ui, InputValidator input, GameConfig config) {
        this.ui = ui;
        this.input = input;
        this.config = config;
    }
    
    // returns the chosen expedition, or null if this planet cannot be drilled due to not enough crew
    public Expedition select(RunState state) throws QuitGameException {
        showRoster(state);
        
        List<CrewMember> available = state.eligibleCrew();
        
        if (available.size() < MIN_CREW) {
            ui.print("");
            if (available.isEmpty()) {
                ui.print("Nobody is fit to deploy.");
            } else {
                ui.print("Only 1 crew member is fit to deploy.");
            }
            ui.print("This planet cannot be drilled. You have to jump on the fuel you have.");
            return null;
        }
        
        // chosen crew
        List<CrewMember> chosen = new ArrayList<>();
        
        while (chosen.size() < MAX_CREW) {
            List<CrewMember> remaining = new ArrayList<>(available);
            remaining.removeAll(chosen);
            
            if (remaining.isEmpty()){
                break;
            }
            
            listNumbered(remaining);
            
            int pick;
            if (chosen.size() >= MIN_CREW) {
                // enough aboard to launch, so 0 becomes real answer.
                pick = input.readInt("Choose crewmember " + (chosen.size() + 1) + ", or 0 to launch with " + chosen.size() + ": ", 0, remaining.size());
            } else {
                pick = input.readInt("Choose crewmember " + (chosen.size() + 1) + " of " + MAX_CREW + ": ", 1, remaining.size());
                
            }
            
            
            // "B" undoes the last pick rather than abandoning the screen
            if (pick == InputValidator.BACK) {
                if(chosen.isEmpty()) {
                    ui.print("Nothing to go back to.");
                } else {
                    CrewMember removed = chosen.remove(chosen.size() -1);
                    ui.print(removed.getName() + " stood down");
                }
                continue;
            }
            
            if (pick == 0) {
                break;
            }
            
            CrewMember member = remaining.get(pick - 1);
            chosen.add(member);
            ui.print(" " + member.getName() + " assigned.");
        
            // a disabled ability due to low stamina must be stated at selection
            if (member.isFatigued()) {
                ui.print(" " + member.getName() + " is FATIGUED. They will be too tired to use their ability");
            }
        }
        
        // The cost of going down
        for (CrewMember member : chosen) {
            member.loseStamina(config.staminaExpeditionCost());
        }
        
        return new Expedition(chosen);
    }
    
    // ineligible crew are listed with the reason they cannot go down
    
    private void showRoster(RunState state) {
        ui.print("");
        ui.print("--- Crew ---");
        for (CrewMember member : state.crew()) {
            ui.print(String.format("  %-12s %-10s Health:%-4d Stamina:%-4d Composure:%-4d %s", 
                    member.getName(),
                    member.getClass().getSimpleName(),
                    member.getHealth(),
                    member.getStamina(),
                    member.getComposure(),
                    statusOf(member)));
        }
    }
    
    // first failing reason winss, so one clear reason is reported rather than multiple
    private String statusOf(CrewMember member) {
        if (!member.isAlive()) {
            return "DEAD";
        }
        if (member.getHealth() < MIN_HEALTH) {
            return "Health " + member.getHealth() + ", cannot deploy";
        }
        if (member.getStamina() < MIN_STAMINA) {
            return "Stamina " + member.getStamina() + ", cannot deploy.";
        }
        if (member.getComposure() < MIN_COMPOSURE) {
              return "Composure " + member.getComposure() + ", cannot deploy";
        }
        if (member.isFatigued()) {
            return "FATIGUED, ability unavailable";
        }
        return "READY";
    }
    
    private void listNumbered(List<CrewMember> members) {
        ui.print("");
        ui.print("Select the associated number of the crewmember you want to leave your main ship and descend to the planet surface.");
        for(int i = 0; i < members.size(); i++) {
            CrewMember member = members.get(i);
            ui.print(" " + (i + 1) + ". " + member.getName() + "  " + statusOf(member));
        }
    }
}
