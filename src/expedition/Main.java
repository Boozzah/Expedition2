package expedition;

import expedition.ui.ConsoleView;
import expedition.ui.GameView;
import expedition.ui.swing.SwingView;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
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
import expedition.io.db.DerbySaveStore;
import expedition.io.db.DerbyScoreStore;
import expedition.io.db.DBManager;

public class Main {
    public static void main(String[] args) {
        
        // The UI. Everything below only knows it has a GameView. The window is the
        // default, --console keeps the text version for debugging
        boolean console = Arrays.asList(args).contains("--console") || GraphicsEnvironment.isHeadless();
        GameView view = console ? new ConsoleView(System.in, System.out) : new SwingView();
 
        // Game configuration 
        GameConfig config = GameConfig.load();
        
        // Game content comes from the database. GameEngine only sees GameDataSource,
        // so it doesn't know or care that this used to be the text files
        GameDataSource data = new DerbyDataSource();
        
        
        // Build the database tables if this is the first run. Without them there
        // is no game to start, so a failure here stops the program.
        try {
            // AI assisted - Claude Code found the database was being seeded from itself (zero crew), so it seeds from the text files
            new DatabaseInitialiser(new TextFileDataSource()).initialise();
        } catch (DatabaseException | DataLoadException e) {
            view.print("Could not start the game - the database could not be set up:");
            view.print(" " + e.getMessage());
            return;
        }
        
        // Selctor object for user to choose which crewmembers to take down on the expedition
        ExpeditionSelector selector = new ExpeditionSelector(view, config);
        
        // Object to write a run log of stats to a txt file
        RunLogWriter log = new RunLogWriter(view);
        
        // seed for a set random run
        Long fixedSeed = parseSeed(args, view);
        
        // Main Game engine
        // the autosave and the high scores, kept in the database next to the game content
        GameEngine engine = new GameEngine(view, config, data, selector, log,
                new DerbySaveStore(), new DerbyScoreStore(), fixedSeed);
    
        
        try {
            engine.run();
        } catch (QuitGameException e) {
            // player typed Q. a deliberate action - exception handles the rest
        } catch (DataLoadException e) {
            // if there is an error loading game data from database
            view.print("Could not start the game - a data file could not be read: ");
            view.print(" " + e.getMessage());
            view.print("Fix the file and try again.");
        }
        // closes the database cleanly so Derby doesn't have to recover it on the next launch
        DBManager.shutdown();
        view.print("Goodbye.");
        // closes the window, once it's gone the program ends
        view.close();
    }
    
    
    // Seed makes a run reproducable: same route, same sites, same hazards, same rolls
    // returns null when no seed given, which means each run is random
    
    
    private static Long parseSeed(String[] args, GameView view) {
        for (int i = 0; i < args.length-1; i++) {
            if (args[i].equals("--seed")) {
                try {
                    long seed = Long.parseLong(args[i+1]);
                    view.print("Seed " + seed + " = this run is reproducable.");
                    return seed;
                } catch (NumberFormatException e) {
                    view.print("Ignoring --seed: \'" + args[i + 1] + "\' is not a number.");
                    return null;
                }
            }
        }
        return null;
    }
}

