package expedition.hazard.effect;
import expedition.engine.RunState;
import expedition.expedition.Expedition;
import expedition.expedition.HaulHolder;


/*
 * EffectContext
 * The toolbox handed to an Effect when it fires — gives it access to
 * everything it might need to change.
 * session is whatever is carrying the oil, the DrillSession or the AscentPhase
 *
 */

public record EffectContext(RunState state, Expedition team, HaulHolder session) 
{
    public expedition.ship.Ship ship()
    {
        return state.ship(); 
    }
}
