/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author nadiameriouli
 */

import expedition.world.SiteTier;

public class SiteTierTest
{
    public static void main(String[] args) 
    {
        
        for (SiteTier tier : SiteTier.values())
        {
            for (int depth = 0; depth <= tier.getMaxDepth() + 2; depth++)
            {
                double chance = tier.hazardChance(depth);
                System.out.println("You are now at depth " +depth);                    // string literal
                System.out.println(tier);                           // prints tier.toString() — enums print their name for free, e.g. "LOW"
                System.out.printf("depth %d -> %.2f%n", depth, chance);
            }
        }
        
    }
}
