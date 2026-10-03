/*
 * CrewFactory
 * Turns a role into the matching CrewMember subclass. Shared by both data
 * sources so the text file and the database both build crew the same way
*/

package expedition.crew;

// AI assisted - Claude suggested moving createCrew out of TextFileDataSource so
// DerbyDataSource can use it too
public final class CrewFactory {
    
    private CrewFactory() {
    }
    
    // turns a role into the matching subclass
    // this is the one place that knows MINER means the Miner class
    // everywhere else just holds a CrewMember and lets the subclass decide what its
    // ability does
    
    // a switch expression returns a value rather than a statement
    public static CrewMember create(Role role, String name, int health, int stamina, int composure) {
        return switch (role) {
            case MINER -> new Miner(name, health, stamina, composure);
            case DOCTOR -> new Doctor(name, health, stamina, composure);
            case PILOT -> new Pilot(name, health, stamina, composure);
            case SURVEYOR -> new Surveyor(name, health, stamina, composure);
            case ENGINEER -> new Engineer(name, health, stamina, composure);
            case GEOLOGIST -> new Geologist(name, health, stamina, composure);
        };
    }
            
}
