/*
 * RunLogWriter
 * Writes the run log (ship's log flavour text + mechanical record of
 * the run) and appends final scores to scores.txt.
 */
package expedition.io;

import expedition.engine.Ending;
import expedition.ui.GameView;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RunLogWriter {
    // file names
    private static final String LOG_FILE = "data/runlog.txt";
    private static final String SCORES_FILE = "data/scores.txt";
    
    private final GameView view;
    
    public RunLogWriter(GameView view) {
        this.view = view;
    }
    
    // Starts a new log, replacing the previous run's log.
    public void startRun(long seed) {
        write(LOG_FILE, false,
                "=== SHIP'S LOG ===",
                "Departed " + timestamp(),
                "Run seed: " + seed,
                "");
    }
    
    // A resumed run carries on the log it already has instead of starting a new one
    public void resumeRun(long seed) {
        write(LOG_FILE, true,
                "",
                "Resumed " + timestamp() + " (run seed: " + seed + ")",
                "");
    }
    
    // one line of the log, append as run happens
    public void log(String line) {
        write(LOG_FILE, true, line);
    }
    
    // The closing entry: how the run ended and what it cost
    public void logEnding(Ending ending) {
        // only a HOME run has survivors. on every other ending the people alive
        // die on the ship
        String aliveLine;
        String deadLine;
        
        if (ending.anyoneSurvived()) {
            deadLine  = "Lost:               " + namesOrNone(ending.deadNames());
            aliveLine = "Survivors:          " + namesOrNone(ending.survivorNames());
        } else {
            deadLine  = "Lost below:         " + namesOrNone(ending.deadNames());
            aliveLine = "Lost with the ship: " + namesOrNone(ending.survivorNames());
        }
        write(LOG_FILE, true,
                "",
                "=== END OF RUN ===",
                "Outcome:         " + ending.reason(),
                "Planets reached: " + ending.planetsReached(),
                "Oil collected:   " + ending.totalOilCollected(),
                deadLine,
                aliveLine,
                "Closed " + timestamp());
    }
    
    
    // append score, to scores file
     public void appendScore(Ending ending) {
          write(SCORES_FILE, true,
                timestamp()
                + "|" + ending.reason()
                + "|" + ending.planetsReached()
                + "|" + ending.totalOilCollected()
                + "|" + (ending.anyoneSurvived() ? ending.survivorNames().size(): 0)
                + "|" + ending.deadNames().size());
    }
     
     
    // The only place that writes to a file. append=false truncates, true adds to the end
    // String... means it can take multiple string inputs
    private void write(String fileName, boolean append, String... lines) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName, append))) {
            for (String line : lines) {
                out.println(line);
            }
        } catch (IOException e) {
            view.print("The ship's log could not be written: " + e.getMessage() + ")");
        }
    }
    
    // returns time and date
    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    
    // returns "nobody" if no one otherwise returns list of names 
    private String namesOrNone(List<String> names) {
        if (names.isEmpty()) {
            return "nobody";
        }
        return String.join(", ", names);
    }
}
    

