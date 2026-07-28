package foreverlive.modid.politics.builder;

import foreverlive.modid.politics.POJO.BuildingPlot;
import foreverlive.modid.politics.POJO.Plot;
import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.POJO.enums.PlotType;
import foreverlive.modid.politics.builder.generators.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class SettlementPlanner {
    public static void planAndBuild(ServerLevel world, Settlement settlement, SettlementStyle style) {
        // 1. Генерируем дороги (получаем и лучи, и кольца)
        RoadGenerator.RoadResult roadResult = RoadGenerator.generateRoadNetwork(world, settlement, style);

        // 2. Стены и ворота строите по лучам (spokes)
        WallGenerator.generateWallsAndGates(world, settlement, roadResult.spokes(), style);

        // 3. Сканируем ВСЕ дороги (allRoads) под застройку
        List<Plot> plots = PlotScanner.scanAndAllocatePlots(world, settlement, style, roadResult.allRoads());

        // 4. Установка центра
        CentralPlazaGenerator.generateCapitalCenter(world, settlement, style);

        // 5. Отрисовка разметки
        PlotScanner.debugRenderPlots(plots);

    }
}
