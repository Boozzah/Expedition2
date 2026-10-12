/*
 * Schema
 * The CREATE TABLE statements for the game database, kept as constants in one place
 * Only DatabaseInitialiser runs them, nothing else in the program should contain SQL for building tables
*/

package expedition.io.db;

final class Schema {
    static final String CREW = 
            "CREATE TABLE CREW ("
            // crew names are unique, so is used as the PK
            + "NAME VARCHAR(40) NOT NULL PRIMARY KEY, "
            + "ROLE VARCHAR(20) NOT NULL, "
            + "HEALTH INTEGER NOT NULL, "
            + "STAMINA INTEGER NOT NULL, "
            + "COMPOSURE INTEGER NOT NULL)";
    
    static final String PLANETS = 
            "CREATE TABLE PLANETS ("
            // AI assisted Claude suggested an auto-numbered ID column for the PK
            + "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "NAME VARCHAR(40) NOT NULL, "
            + "ARCHETYPE VARCHAR(20) NOT NULL, "
            + "FLAVOUR VARCHAR(500) NOT NULL, "
            // site names stay as a single string seperated by ';' instead of a seperate table
            + "SITES VARCHAR(500) NOT NULL)";
    
    static final String HAZARDS = 
            "CREATE TABLE HAZARDS ("
            + "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "NAME VARCHAR(80) NOT NULL, "
            // HAZARD_SET because SET is a reserved word in sql
            + "HAZARD_SET VARCHAR(20) NOT NULL)";
    
    // The six severity rows for each hazard. split from HAzards so one hazards can own many outcomes
    // (one-to-many relationship)
    static final String HAZARD_OUTCOMES = 
            "CREATE TABLE HAZARD_OUTCOMES ("
            // ties each row to a hazard in the hazards table
            + "HAZARD_ID INTEGER NOT NULL REFERENCES HAZARDS(ID), "
            + "SEVERITY INTEGER NOT NULL, "
            // the longest hazard text in hazards.txt is about 1,100 characters long
            + "TEXT VARCHAR(2000) NOT NULL, "
            // the effect codes are stored exactly the same as what they appear in the txt
            // file so EffectParser can keep reading them the way it already does
            + "EFFECTS VARCHAR(500) NOT NULL, "
            // A hazard can only have one row per severity level - composite PK
            + "PRIMARY KEY (HAZARD_ID, SEVERITY))";
    
    // the item recipes from items.txt. ID keeps them in file order like PLANETS and HAZARDS,
    // so a seeded run picks the same items whether they came from the file or the database
    static final String ITEMS = 
            "CREATE TABLE ITEMS ("
            + "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            // Engineer finds Repair Kits by name, so two items can't share one
            + "NAME VARCHAR(40) NOT NULL UNIQUE, "
            + "KIND VARCHAR(20) NOT NULL, "
            + "AMOUNT INTEGER NOT NULL, "
            + "BONUS INTEGER NOT NULL, "
            + "DROP_WEIGHT INTEGER NOT NULL, "
            + "DESCRIPTION VARCHAR(500) NOT NULL)";
    
    private Schema() {
    }
}
