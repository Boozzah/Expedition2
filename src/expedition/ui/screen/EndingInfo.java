/*
 * EndingInfo
 * The end of a run: the story for how it ended, who made it, and the score's breakdown.
 * breakdown is label -> points, in the order the ScoreSheet lists them.
 */
package expedition.ui.screen;

import expedition.engine.EndingReason;
import java.util.List;
import java.util.Map;

public record EndingInfo(EndingReason reason, String story, int planetsReached, int oilCollected,
                         List<String> survivors, List<String> lost, boolean anyoneSurvived,
                         Map<String, Integer> breakdown, int score) {

    public EndingInfo {
        survivors = List.copyOf(survivors);
        lost = List.copyOf(lost);
    }
}
