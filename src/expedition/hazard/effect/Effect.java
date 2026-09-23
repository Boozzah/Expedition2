package expedition.hazard.effect;

/*
 * Effect
 * A single thing a hazard outcome can do to the game state (lose fuel,
 * damage hull, kill crew, etc). One small class per effect type.
 */

public interface Effect {
    void apply(EffectContext ctx);
}
