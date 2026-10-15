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

    // Getting further is worth the most, then getting home at all, then bringing people
    // home with you. Crew who died on a planet cost points, so a reckless run that
    // loses half the crew can score below a careful one, or even go negative
    private static final int PER_PLANET = 100;
    private static final int OIL_PER_POINT = 10;
    private static final int HOME_BONUS = 500;
    private static final int PER_SURVIVOR = 100;
    private static final int PER_DEATH = 50;

    // e.g. 14/10/26 15:05, the same as Nadia's sketch of the screen
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    public static HighScore from(Ending ending, LocalDateTime playedAt) {
        // only a run that gets home has survivors, see Ending.anyoneSurvived
        int survivors = ending.anyoneSurvived() ? ending.survivorNames().size() : 0;
        int lost = ending.deadNames().size();

        int score = ending.planetsReached() * PER_PLANET
                  + ending.totalOilCollected() / OIL_PER_POINT
                  + survivors * PER_SURVIVOR
                  - lost * PER_DEATH;
        if (ending.reason() == EndingReason.HOME) {
            score += HOME_BONUS;
        }

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
