/*
 * RunControls
 * What the in-game menu can do to the run in progress: show its score so far and save it.
 * GameEngine hands one to the view when a run starts, so the view never holds the RunState.
 */
package expedition.ui;

public interface RunControls {

    int score();

    // Saves the run under a name and returns a message saying how it went. The save is
    // the one made when the ship reached this planet, the same point a load carries on from
    String save(String name);

    // the autosave, for Save and Quit
    String saveAndQuit();
}
