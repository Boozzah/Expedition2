/*
 * EffectParser
 * Parses an effect-code string from a data file (e.g.
 * "OIL_PCT:-30;CLOSE_SITE") into real Effect objects, once, at load
 * time. This is the only class in the program that splits effect
 * text — everywhere else calls effect.apply(ctx) on a built object.
 * Unknown or malformed codes fail loudly with the offending line.
 */
package expedition.hazard.effect;

import expedition.exception.DataLoadException;
import java.util.ArrayList;
import java.util.List;

public final class EffectParser {

    // One constant rather than a literal in the loop, because the choice
    // between ';' and '|' is still open - see README 4.2.
    private static final String EFFECT_SEPARATOR = ";";

    // Static helpers only; there is no state worth instantiating.
    private EffectParser() {}

    /*
     * fileName and lineNumber are carried purely so a malformed code can
     * report exactly which file and line to go and fix.
     */
    public static List<Effect> parse(String codes, String fileName, int lineNumber)
            throws DataLoadException {

        List<Effect> effects = new ArrayList<>();

        // An empty EFFECTS field is legal - low-severity outcomes are
        // often pure flavour text with nothing attached.
        if (codes == null || codes.isBlank()) {
            return effects;
        }

        for (String token : codes.split(EFFECT_SEPARATOR)) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue; // tolerate a doubled or trailing separator
            }
            effects.add(parseOne(trimmed, fileName, lineNumber));
        }
        return effects;
    }

    // Turns one code ("HULL:-10") into one Effect.
    private static Effect parseOne(String token, String fileName, int lineNumber)
            throws DataLoadException {

        String[] parts = token.split(":");
        String head = parts[0].trim();

        switch (head) {
            case "FUEL":
                // require() runs before parts[1] is touched, so a bare
                // "FUEL" reports as a bad line rather than crashing on
                // the index.
                require(parts, 2, token, fileName, lineNumber);
                return new FuelEffect(number(parts[1], token, fileName, lineNumber));

            case "HULL":
                require(parts, 2, token, fileName, lineNumber);
                return new HullEffect(number(parts[1], token, fileName, lineNumber));

            case "OIL_PCT":
                require(parts, 2, token, fileName, lineNumber);
                return new OilPercentEffect(number(parts[1], token, fileName, lineNumber));

            case "DEATH":
                require(parts, 2, token, fileName, lineNumber);
                return new CrewDeathEffect(number(parts[1], token, fileName, lineNumber));

            case "ITEM":
                require(parts, 2, token, fileName, lineNumber);
                return new ItemGrantEffect(number(parts[1], token, fileName, lineNumber));

            case "CLOSE_SITE":
                // Demanding exactly 1 part means "CLOSE_SITE:5" is
                // rejected as nonsense rather than quietly ignored.
                require(parts, 1, token, fileName, lineNumber);
                return new SiteCloseEffect();

            case "STAT":
                require(parts, 5, token, fileName, lineNumber);
                return new CrewStatEffect(
                        stat(parts[1], token, fileName, lineNumber),
                        number(parts[2], token, fileName, lineNumber),
                        target(parts[3], token, fileName, lineNumber),
                        number(parts[4], token, fileName, lineNumber));

            default:
                // Fail at startup rather than silently doing nothing every
                // time that hazard fires for the rest of the project.
                throw new DataLoadException(fileName, lineNumber,
                        "unknown effect code \"" + head + "\"");
        }
        // Every case returns or throws, so there are no breaks - one after
        // a return would be unreachable and wouldn't compile.
    }

    // --- validation helpers ---
    // Each checks one thing and, if it fails, throws an error naming the
    // file, the line and the offending token.

    private static void require(String[] parts, int expected, String token,
            String fileName, int lineNumber) throws DataLoadException {
        if (parts.length != expected) {
            throw new DataLoadException(fileName, lineNumber,
                    "effect \"" + token + "\" needs " + expected
                    + " colon-separated parts, found " + parts.length);
        }
    }

    private static int number(String text, String token,
            String fileName, int lineNumber) throws DataLoadException {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            // NumberFormatException is unchecked and would escape main() as
            // a raw stack trace. DataLoadException is checked, so the caller
            // is forced to handle it, and it can name the bad line.
            throw new DataLoadException(fileName, lineNumber,
                    "effect \"" + token + "\" has a non-numeric value \"" + text + "\"");
        }
    }

    // toUpperCase() on both enum lookups means lowercase in the data file
    // works too.
    private static CrewStatEffect.Stat stat(String text, String token,
            String fileName, int lineNumber) throws DataLoadException {
        try {
            return CrewStatEffect.Stat.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataLoadException(fileName, lineNumber,
                    "effect \"" + token + "\" has unknown stat \"" + text
                    + "\" (expected HEALTH, STAMINA or COMPOSURE)");
        }
    }

    private static CrewStatEffect.Target target(String text, String token,
            String fileName, int lineNumber) throws DataLoadException {
        try {
            return CrewStatEffect.Target.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataLoadException(fileName, lineNumber,
                    "effect \"" + token + "\" has unknown target \"" + text
                    + "\" (expected RANDOM or ALL)");
        }
    }
}
