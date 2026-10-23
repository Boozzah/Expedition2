/*
 * Popup
 * A box of text over the screen: a hazard, a jump event, or just something the player
 * needs to know. lines are what it did, "Mikela: health -20", shown under the story.
 */
package expedition.ui.screen;

import java.util.List;

public record Popup(Kind kind, String title, String text, List<String> lines) {

    // HAZARD is drawn with the red outline from Nadia's sketch
    public enum Kind { HAZARD, EVENT, INFO }

    public Popup {
        lines = List.copyOf(lines);
    }

    public static Popup info(String text) {
        return new Popup(Kind.INFO, "", text, List.of());
    }
}
