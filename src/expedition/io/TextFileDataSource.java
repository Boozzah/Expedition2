/*
 * TextFileDataSource
 * Reads all game content from pipe-delimited text files in data/.
 * One public load method per file, all sharing readRows() - so opening and
 * splitting files happens in one place instead of being scattered. any bad line throws
 * DataLoadException naming the file and line number
 */
package expedition.io;

import expedition.crew.CrewMember;                                                                                                           
import expedition.crew.Role;
import expedition.exception.DataLoadException;
import expedition.world.Planet;
import expedition.world.PlanetArchetype;
import expedition.hazard.Hazard;
import expedition.hazard.HazardOutcome;
import expedition.hazard.HazardSet;
import expedition.hazard.effect.Effect;
import expedition.hazard.effect.EffectParser;
import expedition.crew.CrewFactory;

import java.io.BufferedReader;                                                        
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;


public class TextFileDataSource implements GameDataSource {
    
    // where each file lives. Named as constants so a path change is only one edit and more readable
    private static final String CREW_FILE = "data/crew.txt";
    private static final String PLANET_FILE = "data/planets.txt";
    private static final String HAZARD_FILE = "data/hazards.txt";
    
    // split() treats its arguement as a regex, and in a regex a | means or - it would split every character
    // two backslashes means escape it and its a literal pipe
    private static final String FIELD_SEPERATOR = "\\|";
    
    // seperates a list inside one field, such as planets site names
    // ; rather than , because flavour text is full of commas
    private static final String LIST_SEPERATOR = ";";
    
    // crew count and min site names
    private static final int EXPECTED_CREW = 6;
    private static final int MIN_SITE_NAMES = 3;
    
    // number of severity levels
    private static final int SEVERITY_LEVELS = 6;
    
    // one row of a data file paired with where it came from so an error
    // can say which row it came from
    
    // One row of a data file, with its line number carried along for error throwing
    private static record Row(int lineNumber, String[] fields) {
        
    }
    
    // this is the only place in this class that opens a file. Skips comments and blank lines
    // splits on the field seperator and throws an error with any row that has the wrong number of fields.
    private ArrayList<Row> readRows(String fileName, int expectedFields) throws DataLoadException {
        ArrayList<Row> rows = new ArrayList<>();
        int lineNumber = 0;
        
        try (BufferedReader reader  = new BufferedReader(new FileReader(fileName))) {
            String line;
            
            // readLine() returns null at end of file
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                line = line.trim();
                
                // allows for comment lines in the text file
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // The -1 limit keeps trailing empty fields. Without it, split()
                // discards them, so a row ending in '|' - a hazard with no
                // effects, which is allowed - would come back one field short.
                String[] fields = line.split(FIELD_SEPERATOR, -1);
                
                // wrong number of pipes
                if (fields.length != expectedFields) {
                    throw new DataLoadException(fileName, lineNumber, "expected " + expectedFields + " fields, found " + fields.length);
                }
                
                rows.add(new Row(lineNumber, fields));
            }
        } catch(IOException e) {
                // covers the file being missing, unreadable or failing mid read
                throw new DataLoadException(fileName, 0, "could not read file: " + e.getMessage());
        }
        
        return rows;
    }
    
    // crew.txt: NAME|ROLE|HEALTH|STAMINA|COMPOSURE
    
    // builds the six crew members the ship starts with. The role in the file is text
    // so createCrew() below turns it into the correct subclass
    @Override
    public List<CrewMember> loadCrew() throws DataLoadException {
        List<CrewMember> crew = new ArrayList<>();
        
        // 5 is the number of fields a crew line must have
        for (Row row : readRows(CREW_FILE, 5)) {
            String name = row.fields()[0].trim();
            Role role = roleFrom(row.fields()[1], CREW_FILE, row.lineNumber());
            int health = number(row.fields()[2], CREW_FILE, row.lineNumber());
            int stamina = number(row.fields()[3], CREW_FILE, row.lineNumber());
            int composure = number(row.fields()[4], CREW_FILE, row.lineNumber());

            crew.add(CrewFactory.create(role, name, health, stamina, composure));
            
        }
        
        // A file with two Miners and no Pilot would otherwise load happily,a nd break the pilots ability
        if (crew.size() != EXPECTED_CREW) {
            throw new DataLoadException(CREW_FILE, 0, "expected " + EXPECTED_CREW + " crew, found " + crew.size());
        }
        return crew;
    }
    
    // planets.txt: NAME|ARCHETYPE|FLAVOUR|SITE1;SITE2;SITE3
    
    // builds the pool of planets. Note these are only the planets that exists
    // choosing which ones a run visits happens elsewhere, because that draw
    // has to use the one seeded Random owned by RunState
    @Override
    public List<Planet> loadPlanets() throws DataLoadException {
        List<Planet> planets = new ArrayList<>();
        
        // 4 is the number of fields a planet line must have
        for (Row row : readRows(PLANET_FILE, 4)) {
            String name = row.fields()[0].trim();
            PlanetArchetype archetype = archetypeFrom(row.fields()[1], PLANET_FILE, row.lineNumber());
            String flavour = row.fields()[2].trim();
            
            // the last field is itself a list, so it gets split again on ';'
            List<String> siteNames = new ArrayList<>();
            for (String part : row.fields()[3].split(LIST_SEPERATOR)) {
                part = part.trim();
                if (!part.isEmpty()) {
                    siteNames.add(part);
                }
            
            }
            
            // Planet.generateSites() reads the firsst three by index, so fewer than three
            // would be an IndexOutOfBoundsException later
            if (siteNames.size() < MIN_SITE_NAMES) {
                throw new DataLoadException(PLANET_FILE, row.lineNumber(), "needs at least " + MIN_SITE_NAMES + " site names, found" + siteNames.size());
            }
            
            planets.add(new Planet(name, archetype, flavour, siteNames));
        }
        return planets;
    }
    
    // --- hazards.txt: NAME|SET|SEVERITY|TEXT|EFFECTS---
    
    // unlike crew.txt and planets.txt where one line makes one object, here six lines
    // make one hazard - one per severity level. So the rows have to be collected first
    // and the Hazard objects built afterwards, once every row has been seen.
    // That is why this method has 2 loops
    
    // Holds one hazard's rows while they are being collected. Which set it belongs to and the outcomes
    // found so far keyed by severity.
    private record HazardRows(HazardSet set, Map<Integer, HazardOutcome> outcomes) { }
    
    @Override
    public List<Hazard> loadHazards() throws DataLoadException {
       
        // Hazard name -> its rows so far
        // LinkedHashMap, not a hashmap because it needs to be kept in insertion order,
        // so hazards always come out in file order. that matters for a seeded run as it
        // has to pick the same hazard every time it is replayed - a HashMap's order can differ
        // between runs
        Map<String, HazardRows> byName = new LinkedHashMap<>();
        
        // --- loop one: read every row and file it under its hazard name ---
        for (Row row : readRows(HAZARD_FILE, 5)) {
            String name = row.fields()[0].trim();
            HazardSet set = hazardSetFrom(row.fields()[1], HAZARD_FILE, row.lineNumber());
            int severity = number(row.fields()[2], HAZARD_FILE, row.lineNumber());
            String text = row.fields()[3].trim();
            
            // turns "HULL:-10;DEATH:1" into real Effect objects. This happens only once
            // here at startup
            List<Effect> effects = EffectParser.parse(row.fields()[4], HAZARD_FILE, row.lineNumber());
            
            
            // caught here so a mistyped 7 instead of 6 reports as a bad line, rather than silently working
            if (severity < 1 || severity > SEVERITY_LEVELS) {
                throw new DataLoadException(HAZARD_FILE, row.lineNumber(), "severity must be 1-" + SEVERITY_LEVELS + ", found " + severity);
            }
            
            
            // computeIfAbsent means "get this hazard's entry, creating an empty one if this is the first row
            // it replaces a get, a null check, and another get. The new entry takes its set from whichever row was first
            byName.computeIfAbsent(name, k -> new HazardRows(set, new LinkedHashMap<>()))
                    .outcomes()
                    .put(severity, new HazardOutcome(severity, text, effects, row.fields()[4].trim()));
        }
        
        // --- loop 2: check each hazard is complete then build the objects -----
        List<Hazard> hazards = new ArrayList<>();
        for (Map.Entry<String, HazardRows> entry : byName.entrySet()) {
            HazardRows rows = entry.getValue();
            
            // A hazard is missing a severity row
            if (rows.outcomes().size() != SEVERITY_LEVELS) {
                throw new DataLoadException(HAZARD_FILE, 0, 
                    entry.getKey() + " has " + rows.outcomes().size()
                    + " severity rows, needs " + SEVERITY_LEVELS);
            }
            
            hazards.add(new Hazard(entry.getKey(), rows.set(), rows.outcomes()));
        }
        return hazards;
    } 
    
    // Helper methods
    // each converts an unchecked exception into checked, so typos result in a file error rather than crash
    
    // valueOf is an enum lookup
    // toUpperCase() means lowercase works too
    private Role roleFrom(String text, String fileName, int lineNumber) throws DataLoadException {
        try {
            return Role.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DataLoadException(fileName, lineNumber, "unknown role \"" + text.trim() + "\"");
        }
    }
    
    // same as above, for planet archetype
    private PlanetArchetype archetypeFrom(String text, String fileName, int lineNumber)
              throws DataLoadException {
          try {
              return PlanetArchetype.valueOf(text.trim().toUpperCase());
          } catch (IllegalArgumentException e) {
              throw new DataLoadException(fileName, lineNumber,
                      "unknown archetype \"" + text.trim() + "\"");
          }
      }
    
      // converts number field string to int
      private int number(String text, String fileName, int lineNumber) throws DataLoadException {
          try {
              return Integer.parseInt(text.trim());
          } catch (NumberFormatException e) {
              throw new DataLoadException(fileName, lineNumber,
                      "not a number: \"" + text.trim() + "\"");
          }
      }
      
      //
      private HazardSet hazardSetFrom(String text, String fileName, int lineNumber)
              throws DataLoadException {
          try {
              return HazardSet.valueOf(text.trim().toUpperCase());
          } catch (IllegalArgumentException e) {
              throw new DataLoadException(fileName, lineNumber,
                      "unknown hazard set \"" + text.trim() + "\"");
          }
      }
}
