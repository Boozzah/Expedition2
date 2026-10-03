package expedition;

import expedition.ui.ConsoleUI;
import expedition.ui.InputValidator;
import expedition.exception.QuitGameException;
import expedition.exception.DataLoadException;
import expedition.io.GameDataSource;
import expedition.io.TextFileDataSource;
import expedition.engine.GameConfig;
import expedition.engine.GameEngine;
import expedition.expedition.ExpeditionSelector;
import expedition.io.RunLogWriter;
import expedition.io.db.DatabaseInitialiser;
import expedition.exception.DatabaseException;

import expedition.io.db.DerbyDataSource;
import expedition.io.db.DBManager;

public class Main {
    public static void main(String[] args) {
        
        // UI (console input)
        ConsoleUI ui = new ConsoleUI(System.in, System.out);
        
        // Object to validate user input
        InputValidator input = new InputValidator(ui);
 
        // Game configuration 
        GameConfig config = GameConfig.load();
        
        // Game content comes from the database. GameEngine only sees GameDataSource,
        // so it doesn't know or care that this used to be the text files
        GameDataSource data = new DerbyDataSource();
        
        
        // Build the database tables if this is the first run. Without them there
        // is no game to start, so a failure here stops the program.
        try {
            new DatabaseInitialiser(new TextFileDataSource()).initialise();
        } catch (DatabaseException | DataLoadException e) {
            ui.print("Could not start the game - the database could not be set up:");
            ui.print(" " + e.getMessage());
            return;
        }
        
        // Selctor object for user to choose which crewmembers to take down on the expedition
        ExpeditionSelector selector = new ExpeditionSelector(ui, input, config);
        
        // Object to write a run log of stats to a txt file
        RunLogWriter log = new RunLogWriter(ui);
        
        // seed for a set random run
        Long fixedSeed = parseSeed(args, ui);
        
        // Main Game engine
        GameEngine engine = new GameEngine(ui, input, config, data, selector, log, fixedSeed);
    
        
        try {
            engine.run();
        } catch (QuitGameException e) {
            // player typed Q. a deliberate action - exception handles the rest
        } catch (DataLoadException e) {
            // if there is an error loading game data from database
            ui.print("Could not start the game - a data file could not be read: ");
            ui.print(" " + e.getMessage());
            ui.print("Fix the file and try again.");
        }
        // closes the database cleanly so Derby doesn't have to recover it on the next launch
        DBManager.shutdown();
        ui.print("Goodbye.");
    }
    
    
    // Seed makes a run reproducable: same route, same sites, same hazards, same rolls
    // returns null when no seed given, which means each run is random
    
    
    private static Long parseSeed(String[] args, ConsoleUI ui) {
        for (int i = 0; i < args.length-1; i++) {
            if (args[i].equals("--seed")) {
                try {
                    long seed = Long.parseLong(args[i+1]);
                    ui.print("Seed " + seed + " = this run is reproducable.");
                    return seed;
                } catch (NumberFormatException e) {
                    ui.print("Ignoring --seed: \'" + args[i + 1] + "\' is not a number.");
                    return null;
                }
            }
        }
        return null;
    }
}

