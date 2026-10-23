/*
 * BackInTimeTest
 * Plays a whole run through the console where the crew never goes down, so the ship runs
 * out of fuel on the third jump. Going back in time should cost points and put the ship
 * back at the same planet, and running out again with End game should leave it adrift.
 * The saves and scores are kept in memory so the real database isn't touched.
 */
package expedition.engine;

import expedition.exception.DataLoadException;
import expedition.exception.QuitGameException;
import expedition.expedition.ExpeditionSelector;
import expedition.io.RunLogWriter;
import expedition.io.SaveSlot;
import expedition.io.SaveStore;
import expedition.io.ScoreStore;
import expedition.io.TextFileDataSource;
import expedition.ui.ConsoleView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BackInTimeTest {

    // only the autosave is used in this run
    private static class MemorySaves implements SaveStore {
        private SavedRun autosave;

        @Override
        public List<SaveSlot> list() {
            return List.of();
        }

        @Override
        public void autosave(SavedRun run) {
            autosave = run;
        }

        @Override
        public void saveAs(String name, SavedRun run) {
        }

        @Override
        public SavedRun load(int slotId) {
            return autosave;
        }

        @Override
        public void deleteAutosave() {
            autosave = null;
        }
    }

    private static class MemoryScores implements ScoreStore {
        private final List<HighScore> added = new ArrayList<>();

        @Override
        public void add(HighScore score) {
            added.add(score);
        }

        @Override
        public List<HighScore> top(int limit) {
            return added;
        }
    }

    @Test
    public void goingBackInTimeCostsPointsAndThenTheRunCanStillEndAdrift() throws DataLoadException
    {
        // new game, stay aboard and jump at planets 1 and 2, then at planet 3 the fuel runs
        // out: back in time, stay aboard again, run out again and end the game, then quit
        String typed = "n\n"
                + "n\nj\n"
                + "n\nj\n"
                + "n\nj\nb\n"
                + "n\nj\ne\n"
                + "q\n";
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        ConsoleView view = new ConsoleView(new ByteArrayInputStream(typed.getBytes()), new PrintStream(printed));
        GameConfig config = GameConfig.load();
        MemoryScores scores = new MemoryScores();

        GameEngine engine = new GameEngine(view, config, new TextFileDataSource(), new ExpeditionSelector(view, config),
                new RunLogWriter(view), new MemorySaves(), scores, 42L);
        // typing q quits the same way it does in Main, by throwing
        try {
            engine.run();
        } catch (QuitGameException e) {
            // the end of the script
        }

        String output = printed.toString();
        assertTrue(output, output.contains("You ran out of fuel"));
        assertTrue(output, output.contains("Back in time"));

        assertEquals("one finished run", 1, scores.added.size());
        HighScore score = scores.added.get(0);
        assertEquals(EndingReason.ADRIFT, score.reason());
        // the run never left planet 3, going back in time doesn't count as reaching another
        assertEquals(3, score.planetsReached());
        assertTrue("the 1000 points for going back should take the score negative", score.score() < 0);
    }
}
