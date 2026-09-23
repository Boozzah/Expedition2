/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package TestVein;

/**
 *
 * @author nadiameriouli
 */
import expedition.world.Vein; 
import java.util.List; 
import java.util.ArrayList; 

public class veinTest 
{
    public static void main(String[] args)
    {
        List<Integer> testLevels = new ArrayList<>(); 
        testLevels.add(20); 
        testLevels.add(0); 
        testLevels.add(30); 
        testLevels.add(0); 
        testLevels.add(40); 
        
        Vein vein = new Vein(testLevels); 
        
        for(int i = 0; i < 6; i++)
        {
            System.out.println("drillNext() call " + (i+1) + " -> " + vein.drillNext()); 
        }
        
        System.out.println("pastBottom: " + vein.pastBottom()); 
        System.out.println("levelsRemaining " + vein.levelsRemaining());
    }
}
