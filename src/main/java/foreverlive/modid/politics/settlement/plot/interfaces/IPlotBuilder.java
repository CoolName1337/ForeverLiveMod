package foreverlive.modid.politics.settlement.plot.interfaces;

import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import net.minecraft.server.level.ServerLevel;

public interface IPlotBuilder {
    void build(ServerLevel level, BuildingPlot plot, SettlementStyle style);
    void upgrade(ServerLevel level, BuildingPlot plot, SettlementStyle style, int newTier);
}