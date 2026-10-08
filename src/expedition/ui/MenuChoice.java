/*
 * MenuChoice
 *
 * What the player picked on the main menu. GameView.showMainMenu returns one of
 * these, so GameEngine doesn't need to know if it came from a typed letter or a
 * clicked button.
 */
package expedition.ui;

public enum MenuChoice {
    NEW_GAME,
    HOW_TO_PLAY,
    QUIT
}
