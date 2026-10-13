/*
 * GameDataSource
 * Dependency-inversion boundary for all game content: planets, crew,
 * hazards, jump events, items and endings. TextFileDataSource
 * implements this now; a database-backed source could later without
 * GameEngine changing.
 */
package expedition.io;

import expedition.crew.CrewMember;
import expedition.exception.DataLoadException;
import expedition.world.Planet;
import expedition.hazard.Hazard;
import expedition.item.ItemDefinition;

import java.util.List;

public interface GameDataSource {
    List<CrewMember> loadCrew() throws DataLoadException;
    List<Planet> loadPlanets() throws DataLoadException;
    List<Hazard> loadHazards() throws DataLoadException;
    List<ItemDefinition> loadItems() throws DataLoadException;
    
}
