/*
 * ScoreSheet
 * The run's score, added up as the run goes instead of only at the end. Each way of
 * earning or losing points has its own line, so the ending can show a breakdown of
 * where the score came from, and the in-game menu can show the score so far.
 *
 * Every method hands back the points it just added, so a popup can say "+2 points"
 * straight after a drill. Hazards don't cost points, the damage they do is enough.
*/
package expedition.engine;

import java.util.EnumMap;
import java.util.Map;

public class ScoreSheet {

    // the lines of the breakdown, in the order the ending lists them
    public enum Reason {
        OIL("Oil drilled"),
        PLANETS("Planets reached"),
        ITEMS("Items collected"),
        REPAIRS("Ship repairs"),
        DEEP_DRILLING("Deep drilling"),
        SITES_DRILLED_DRY("Sites drilled dry"),
        JUMP_EVENTS("Jump events"),
        HOME("Made it home"),
        SURVIVORS("Crew brought home"),
        DEATHS("Crew lost"),
        BACK_IN_TIME("Back in time");

        private final String label;

        Reason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    // Getting further is worth the most, then getting home at all, then bringing people
    // home with you. Crew who died cost points, so a reckless run that loses half the
    // crew can score below a careful one, or even go negative
    private static final int OIL_PER_POINT = 10;
    private static final int PER_PLANET = 100;
    private static final int PER_ITEM = 10;
    private static final int PER_REPAIR = 25;
    // every level past this one at a single site is worth a bonus
    private static final int DEEP_LEVEL_START = 5;
    private static final int PER_DEEP_LEVEL = 5;
    private static final int SITE_DRILLED_DRY = 50;
    private static final int HOME_BONUS = 500;
    private static final int PER_SURVIVOR = 100;
    private static final int PER_DEATH = 50;
    // the price of going back to the last planet after running out of fuel on a jump
    public static final int BACK_IN_TIME_COST = 1000;

    private final Map<Reason, Integer> points = new EnumMap<>(Reason.class);

    // oil drilled over the whole run, so the part of a haul under 10 isn't lost.
    // 27 oil then 13 oil is 40 oil, 4 points, not 2 + 1
    private int oilDrilled = 0;

    public ScoreSheet() {
    }

    // for resuming a saved run, the score carries on from where it was
    public ScoreSheet(Map<Reason, Integer> saved, int oilDrilled) {
        points.putAll(saved);
        this.oilDrilled = oilDrilled;
    }

    public int oilDrilled(int oil) {
        int before = oilDrilled / OIL_PER_POINT;
        oilDrilled += oil;
        return add(Reason.OIL, oilDrilled / OIL_PER_POINT - before);
    }

    public int planetReached() {
        return add(Reason.PLANETS, PER_PLANET);
    }

    public int itemCollected() {
        return add(Reason.ITEMS, PER_ITEM);
    }

    public int partRepaired() {
        return add(Reason.REPAIRS, PER_REPAIR);
    }

    // depth is the level just drilled, the bonus starts on the level after DEEP_LEVEL_START
    public int levelDrilled(int depth) {
        if (depth <= DEEP_LEVEL_START) {
            return 0;
        }
        return add(Reason.DEEP_DRILLING, PER_DEEP_LEVEL);
    }

    public int siteDrilledDry() {
        return add(Reason.SITES_DRILLED_DRY, SITE_DRILLED_DRY);
    }

    // a jump event says how many points it is worth, good or bad
    public int jumpEvent(int eventPoints) {
        return add(Reason.JUMP_EVENTS, eventPoints);
    }

    public int wentBackInTime() {
        return add(Reason.BACK_IN_TIME, -BACK_IN_TIME_COST);
    }

    // The points that depend on how the run ended. Only a run that gets home has
    // survivors, see Ending.anyoneSurvived
    public void finish(Ending ending) {
        if (ending.anyoneSurvived()) {
            add(Reason.HOME, HOME_BONUS);
            add(Reason.SURVIVORS, ending.survivorNames().size() * PER_SURVIVOR);
        }
        add(Reason.DEATHS, -ending.deadNames().size() * PER_DEATH);
    }

    public int total() {
        int total = 0;
        for (int p : points.values()) {
            total += p;
        }
        return total;
    }

    // only the lines that earned or lost something, in Reason order
    public Map<Reason, Integer> breakdown() {
        Map<Reason, Integer> lines = new EnumMap<>(Reason.class);
        for (Map.Entry<Reason, Integer> e : points.entrySet()) {
            if (e.getValue() != 0) {
                lines.put(e.getKey(), e.getValue());
            }
        }
        return lines;
    }

    public int oilDrilledSoFar() {
        return oilDrilled;
    }

    private int add(Reason reason, int amount) {
        points.merge(reason, amount, Integer::sum);
        return amount;
    }
}
