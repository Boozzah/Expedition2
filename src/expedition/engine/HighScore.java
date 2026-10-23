/*
 * HighScore
 * One finished run on the high-score table: when it was played, how it ended and
 * what it scored. Made from the run's Ending when the run is over.
*/
package expedition.engine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record HighScore(LocalDateTime playedAt, EndingReason reason, int planetsReached,
                        int oilCollected, int survivors, int lost, int score) {

    // e.g. 14/10/26 15:05, the same as Nadia's sketch of the screen
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    // the score itself is added up by the run's ScoreSheet as it goes
    public static HighScore from(Ending ending, int score, LocalDateTime playedAt) {
        // only a run that gets home has survivors, see Ending.anyoneSurvived
        int survivors = ending.anyoneSurvived() ? ending.survivorNames().size() : 0;
        int lost = ending.deadNames().size();
        return new HighScore(playedAt, ending.reason(), ending.planetsReached(),
                ending.totalOilCollected(), survivors, lost, score);
    }

    // the console and the window both show the date and ending this way
    public String when() {
        return playedAt.format(WHEN);
    }

    // HULL_FAILURE -> HULL FAILURE
    public String ending() {
        return reason.name().replace('_', ' ');
    }
}
