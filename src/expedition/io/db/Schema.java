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
    
    // The autosave. There is only ever one saved run, in SLOT 1, but keeping a slot number
    // means SAVED_CREW can point at it the same way HAZARD_OUTCOMES points at HAZARDS
    static final String SAVED_RUN = 
            "CREATE TABLE SAVED_RUN ("
            + "SLOT INTEGER NOT NULL PRIMARY KEY, "
            // BIGINT because the seeds are Java longs, too big for an INTEGER
            + "SEED BIGINT NOT NULL, "
            + "RNG_SEED BIGINT NOT NULL, "
            + "PLANET_INDEX INTEGER NOT NULL, "
            + "TOTAL_OIL INTEGER NOT NULL, "
            + "FUEL INTEGER NOT NULL, "
            // one column per ship part, see ShipPart
            + "HULL INTEGER NOT NULL, "
            + "ENGINE INTEGER NOT NULL, "
            + "EXHAUST INTEGER NOT NULL, "
            + "PRESSURE INTEGER NOT NULL, "
            + "WINDOWS INTEGER NOT NULL, "
            // the planet names in route order, and the item names in inventory order,
            // each seperated by ';' like the site names in PLANETS
            + "ROUTE VARCHAR(500) NOT NULL, "
            + "ITEMS VARCHAR(500) NOT NULL, "
            // the score so far as REASON=points pairs, e.g. OIL=12;PLANETS=300
            + "SCORE VARCHAR(500) NOT NULL, "
            + "OIL_DRILLED INTEGER NOT NULL)";
    
    // One row per crew member in the save, dead ones included. POSITION keeps them in the
    // order they were in, which the seeded random crew picks depend on
    static final String SAVED_CREW = 
            "CREATE TABLE SAVED_CREW ("
            + "SLOT INTEGER NOT NULL REFERENCES SAVED_RUN(SLOT), "
            + "POSITION INTEGER NOT NULL, "
            + "NAME VARCHAR(40) NOT NULL, "
            + "ROLE VARCHAR(20) NOT NULL, "
            + "HEALTH INTEGER NOT NULL, "
            + "STAMINA INTEGER NOT NULL, "
            + "COMPOSURE INTEGER NOT NULL, "
            + "ALIVE BOOLEAN NOT NULL, "
            + "PRIMARY KEY (SLOT, POSITION))";
    
    // Every finished run, for the high-score screen. Rows are only ever added
    static final String SCORES = 
            "CREATE TABLE SCORES ("
            + "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, "
            + "PLAYED_AT TIMESTAMP NOT NULL, "
            + "ENDING VARCHAR(20) NOT NULL, "
            + "PLANETS INTEGER NOT NULL, "
            + "OIL INTEGER NOT NULL, "
            + "SURVIVORS INTEGER NOT NULL, "
            + "LOST INTEGER NOT NULL, "
            + "SCORE INTEGER NOT NULL)";
    
    private Schema() {
    }
}
