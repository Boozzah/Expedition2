
package expedition.engine;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
/*
 * GameConfig
 * Holds every tunable game constant as a hardcoded default. load()
 * optionally overrides values from data/config.txt if present; a
 * missing or malformed file must never stop the game from starting.
 */
public class GameConfig {
    
    private final int composureDeathPenalty;
    private final int jumpCostBase; 
    private final int jumpCostIncrement; 
    private final int startingFuel; 
    private final int tankCapacity; 
    private final int staminaExpeditionCost;
    
    // if not going on expidition regain stats:
    private final int healthRestRecovery;
    private final int staminaRestRecovery;
    private final int composureRestRecovery;
    
    // ascent hazard chance, in percent
    private final int ascentTriggerBase;
    private final int ascentTriggerPerExtraTrip;
    
    // how many items the ship can carry, and the percent chance of finding one per drill level
    private final int inventorySlots;
    private final int itemDropBase;
    
    // the percent chance of something happening on a jump
    private final int jumpEventChance;
    
    private GameConfig(int composureDeathPenalty, int jumpCostBase, int jumpCostIncrement, int startingFuel, int tankCapacity, 
                        int healthRestRecovery, int staminaRestRecovery, int composureRestRecovery, int staminaExpeditionCost,
                        int ascentTriggerBase, int ascentTriggerPerExtraTrip, int inventorySlots, int itemDropBase,
                        int jumpEventChance)
    {
        this.composureDeathPenalty = composureDeathPenalty;
        this.jumpCostBase = jumpCostBase; 
        this.jumpCostIncrement = jumpCostIncrement; 
        this.startingFuel = startingFuel; 
        this.tankCapacity = tankCapacity;
        this.healthRestRecovery = healthRestRecovery;
        this.staminaRestRecovery = staminaRestRecovery;
        this.composureRestRecovery = composureRestRecovery;
        this.staminaExpeditionCost = staminaExpeditionCost;
        this.ascentTriggerBase = ascentTriggerBase;
        this.ascentTriggerPerExtraTrip = ascentTriggerPerExtraTrip;
        this.inventorySlots = inventorySlots;
        this.itemDropBase = itemDropBase;
        this.jumpEventChance = jumpEventChance;
    }
    
    // static factory method
    public static GameConfig load() {
        // default hardcoded values, subject to change by entering a change in the config.txt file
        int composureDeathPenalty = 25;
        int jumpCostBase = 100; 
        int jumpCostIncrement = 25; 
        int startingFuel = 225; // GDD 12: about two-thirds of a tank
        int tankCapacity = 350;
        int healthRestRecovery = 10;
        int staminaRestRecovery = 25;
        int composureRestRecovery = 8;
        int staminaExpeditionCost = 35;
        int ascentTriggerBase = 20; // GDD 8: 20% chance of an ascent hazard
        int ascentTriggerPerExtraTrip = 8; // +8% for every extra site drilled at the same planet
        int inventorySlots = 8; // GDD 10: 8 slots, a 9th item means throwing one away
        int itemDropBase = 8; // GDD 5.5: 8% per drill level, before the Surveyor's bonus
        int jumpEventChance = 60; // GDD 9: 60% of jumps have an event
        
        try (BufferedReader reader = new BufferedReader(new FileReader("data/config.txt"))) {
              String line;
              while ((line = reader.readLine()) != null) {
                  line = line.trim();

                  // comment lines and blank lines are legal and mean nothing
                  if (line.isEmpty() || line.startsWith("#")) {
                      continue;                                                         
                  }
  
                  // limit of 2 so a value containing '=' doesn't get chopped up        
                  String[] parts = line.split("=", 2);

                  // a key with no value ("TANK_CAPACITY=") - skip it and keep
                  // the default rather than reading parts[1] and crashing
                  if (parts.length < 2) {
                      continue;
                  }

                  String key = parts[0].trim();
                  String value = parts[1].trim();                                       
                  
                  // assign values based on key
                  try {
                      switch (key) {
                          case "COMPOSURE_DEATH_PENALTY" -> composureDeathPenalty = Integer.parseInt(value);
                          case "JUMP_COST_BASE" -> jumpCostBase = Integer.parseInt(value);
                          case "JUMP_COST_INCREMENT" -> jumpCostIncrement = Integer.parseInt(value);
                          case "STARTING_FUEL" -> startingFuel = Integer.parseInt(value);
                          case "TANK_CAPACITY" -> tankCapacity = Integer.parseInt(value);
                          case "HEALTH_REST_RECOVERY" -> healthRestRecovery = Integer.parseInt(value);
                          case "STAMINA_REST_RECOVERY" -> staminaRestRecovery = Integer.parseInt(value);
                          case "COMPOSURE_REST_RECOVERY" -> composureRestRecovery = Integer.parseInt(value);
                          case "STAMINA_EXPEDITION_COST" -> staminaExpeditionCost = Integer.parseInt(value);
                          case "ASCENT_TRIGGER_BASE" -> ascentTriggerBase = Integer.parseInt(value);
                          case "ASCENT_TRIGGER_PER_EXTRA_TRIP" -> ascentTriggerPerExtraTrip = Integer.parseInt(value);
                          case "INVENTORY_SLOTS" -> inventorySlots = Integer.parseInt(value);
                          case "ITEM_DROP_BASE" -> itemDropBase = Integer.parseInt(value);
                          case "JUMP_EVENT_CHANCE" -> jumpEventChance = Integer.parseInt(value);
                          default -> { }   // unknown key - ignore it, the file may be from a later version
                      }
                  } catch (NumberFormatException e) {
                      // a non-numeric value ("TANK_CAPACITY=lots") keeps the default.
                      // Deliberately silent: this class must never stop the game starting.
                  }
              }
          } catch (IOException e) {
              // covers the file being missing or unreadable - defaults apply
        }
        
        return new GameConfig(composureDeathPenalty, jumpCostBase, jumpCostIncrement, 
                              startingFuel, tankCapacity, healthRestRecovery, 
                              staminaRestRecovery, composureRestRecovery, staminaExpeditionCost,
                              ascentTriggerBase, ascentTriggerPerExtraTrip, inventorySlots, itemDropBase,
                              jumpEventChance);
    }
    
    // Getters
    public int composureDeathPenalty() { return composureDeathPenalty; }
    public int jumpCostBase() { return jumpCostBase; }
    public int jumpCostIncrement() { return jumpCostIncrement; }
    public int startingFuel() { return startingFuel; } 
    public int tankCapacity() { return tankCapacity; }
    public int healthRestRecovery() {return healthRestRecovery;}
    public int staminaRestRecovery() { return staminaRestRecovery;}
    public int composureRestRecovery() { return composureRestRecovery;}
    public int staminaExpeditionCost() { return staminaExpeditionCost; }
    public int ascentTriggerBase() { return ascentTriggerBase; }
    public int ascentTriggerPerExtraTrip() { return ascentTriggerPerExtraTrip; }
    public int inventorySlots() { return inventorySlots; }
    public int itemDropBase() { return itemDropBase; }
    public int jumpEventChance() { return jumpEventChance; }
}
