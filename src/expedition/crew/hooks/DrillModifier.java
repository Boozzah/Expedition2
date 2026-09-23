/*
 * DrillModifier
 * Hook implemented by crew who change oil yield while drilling
 * (Miner). Only hook wired in for project 1
 */
package expedition.crew.hooks;

public interface DrillModifier {
    
    int modifyYield(int baseYield, int depth);
}
