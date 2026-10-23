/*
 * PlanetInfo
 * The planet the ship has just reached, for the arrival screen.
 */
package expedition.ui.screen;

import expedition.world.Planet;
import expedition.world.PlanetArchetype;

public record PlanetInfo(String name, PlanetArchetype archetype, String flavour, int jumpNumber, int routeLength) {

    public static PlanetInfo of(Planet planet, int jumpNumber, int routeLength) {
        return new PlanetInfo(planet.name(), planet.archetype(), planet.introFlavour(), jumpNumber, routeLength);
    }
}
