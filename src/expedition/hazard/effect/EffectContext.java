package expedition.hazard.effect;
import expedition.engine.RunState;
import expedition.expedition.Expedition;
import expedition.expedition.DrillSession;


/*
 * EffectContext
 * The toolbox handed to an Effect when it fires — gives it access to
 * everything it might need to change.
 *
 */

public record EffectContext(RunState state, Expedition team, DrillSession session) 
{
    public expedition.ship.Ship ship()
    {
        return state.ship(); 
    }
}
