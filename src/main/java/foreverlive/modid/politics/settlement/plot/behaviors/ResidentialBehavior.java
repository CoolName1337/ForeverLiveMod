package foreverlive.modid.politics.settlement.plot.behaviors;

import foreverlive.modid.politics.settlement.plot.interfaces.IPlotBehavior  ;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.PlotTask;
import foreverlive.modid.politics.settlement.enums.RoleTag;
import foreverlive.modid.politics.settlement.enums.TaskType;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResidentialBehavior implements IPlotBehavior {

    @Override
    public void tick(ServerLevel level, BuildingPlot plot) {
        // Каждый игровой день (24000 тиков) можно проверять состояние жителей
        if (level.getGameTime() % 24000 == 0) {
            // Логика: восстанавливать здоровье жителей, начислять счастье и т.д.
        }
    }

    @Override
    public void onWorkerAssigned(BuildingPlot plot, UUID workerId) {
        // В жилом доме "рабочие" — это жильцы // ну не сказал бы
        plot.getAssignedResidents().add(workerId);
    }

    @Override
    public List<PlotTask> generateDailyTasks(BuildingPlot plot) {
        List<PlotTask> tasks = new ArrayList<>();

        // Если дом поврежден, создаем задачу на ремонт
        if (!plot.getDamagedBlocks().isEmpty()) {
            tasks.add(new PlotTask(
                    TaskType.REPAIR_STRUCTURE,
                    plot.getAnchorPos(),
                    RoleTag.BUILDER,
                    80
            ));
        }

        return tasks;
    }
}