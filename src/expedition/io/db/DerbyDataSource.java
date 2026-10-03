/*
   * DerbyDataSource
   * The database version of GameDataSource. Hands each request to the right DAO and
   * turns any DatabaseException into the DataLoadException GameEngine already expects,
   * so the engine never knows the content came from a database instead of text files
 */
package expedition.io.db;

import expedition.crew.CrewMember;
import expedition.exception.DataLoadException;
import expedition.exception.DatabaseException;
import expedition.hazard.Hazard;
import expedition.io.GameDataSource;
import expedition.world.Planet;

import java.util.List;

public class DerbyDataSource implements GameDataSource {

    @Override
    public List<CrewMember> loadCrew() throws DataLoadException {
        try {
            return new CrewDao().findAll();
        } catch (DatabaseException e) {
            throw new DataLoadException("CREW", e.getMessage(), e);
        }
    }

    @Override
    public List<Planet> loadPlanets() throws DataLoadException {
        try {
            return new PlanetDao().findAll();
        } catch (DatabaseException e) {
            throw new DataLoadException("PLANETS", e.getMessage(), e);
        }
    }

    @Override
    public List<Hazard> loadHazards() throws DataLoadException {
        try {
            return new HazardDao().findAll();
        } catch (DatabaseException e) {
            throw new DataLoadException("HAZARDS", e.getMessage(), e);
        }
    }
}
