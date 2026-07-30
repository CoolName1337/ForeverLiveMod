package foreverlive.modid.politics.settlement.plot.interfaces;

import foreverlive.modid.politics.settlement.PlotTask;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.UUID;

public interface IPlotBehavior {
    void tick(ServerLevel level, BuildingPlot plot);
    void onWorkerAssigned(BuildingPlot plot, UUID workerId);
    List<PlotTask> generateDailyTasks(BuildingPlot plot);
}