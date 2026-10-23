/*
 * SiteInfo
 * One drill site on the planet's site map: its risk at the top and bottom of the vein,
 * the oil it should hold, and whether it can still be drilled.
 */
package expedition.ui.screen;

import expedition.world.DrillSite;
import java.util.ArrayList;
import java.util.List;

public record SiteInfo(String name, String tier, String estimatedOil, int riskStart, int riskDeep, boolean open) {

    public static SiteInfo of(DrillSite site) {
        int riskStart = (int) Math.round(site.tier().hazardChance(1) * 100);
        int riskDeep = (int) Math.round(site.tier().hazardChance(site.tier().getMaxDepth()) * 100);
        boolean open = !site.isClosed() && !site.isExhausted();
        return new SiteInfo(site.name(), site.tier().name(), site.estimatedOil(), riskStart, riskDeep, open);
    }

    public static List<SiteInfo> of(List<DrillSite> sites) {
        List<SiteInfo> infos = new ArrayList<>();
        for (DrillSite s : sites) {
            infos.add(of(s));
        }
        return infos;
    }
}
