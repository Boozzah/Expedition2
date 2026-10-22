/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package expedition.util;

/**
 *
 * @author nadiameriouli
 */

// wraps long strings onto multiple lines using word boundaries, so flavour text doesnt run off the screen or go to a new line mid word because you changed your settings to not 
// let text run off screen
public final class TextWrapper 
{
    // AI assisted - Claude suggested one LINE_WIDTH constant instead of passing 100 everywhere
    // how wide the game's text is, in characters
    public static final int LINE_WIDTH = 100;

    private TextWrapper() { };

    public static String wrap(String text)
    {
        return wrap(text, LINE_WIDTH);
    }

    public static String wrap(String text, int maxWidth)
    {
        // makes an array of all the words by noticing when there is a space. 
        String[] words = text.split(" "); 
        // result holds finished - when it notices its overflowed 100 characters
        // currentLine holds the one that is being built until its is full and moves onto the result 
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder(); 
        
        // going through the words 
        for (String word : words)
        {
            // the + 1 accounts for the space before the word. This is what is  checking if the line is full 
            if(currentLine.length() + word.length() + 1 > maxWidth)
            {
                result.append(currentLine).append("\n");
                currentLine = new StringBuilder(); 
            }
            
            // if the line isnt empty adds a space before the world, if you didnt check if it was empty and just did a space every line would be indented 
            if(currentLine.length() > 0)
            {
                currentLine.append(" "); 
            }
            currentLine.append(word);
        }
        
        // this flushes the currentLine into result  
        result.append(currentLine);
        // the last bit wont overflow so will not be added to result and so is manually added with a toString 
        return result.toString(); 
    }
}
