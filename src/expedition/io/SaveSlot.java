/*
 * SaveSlot
 * One row of the Load Game screen: a save's name, when it was last played and how far
 * the run had got. Doesn't hold the run itself, that is only read once the player picks it.
 */
package expedition.io;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record SaveSlot(int id, String name, LocalDateTime lastPlayed, boolean autosave, int planetsReached) {

    // like Nadia's load screen: just the time for a save from today, otherwise the day
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter EARLIER = DateTimeFormatter.ofPattern("d/M");

    public String lastPlayedText() {
        boolean today = lastPlayed.toLocalDate().equals(LocalDate.now());
        return lastPlayed.format(today ? TODAY : EARLIER);
    }
}
