package foreverlive.modid.politics.services;

import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.generators.HouseGenerator;
import foreverlive.modid.politics.generators.PlotScanner;
import foreverlive.modid.politics.generators.RoadGenerator;
import foreverlive.modid.politics.generators.WallGenerator;
import foreverlive.modid.politics.settlement.SettlementStyle;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class SettlementBuildTask {

    public enum Phase {
        START_TERRAFORM,
        WAITING_TERRAFORM,
        START_INFRASTRUCTURE, // Дороги и стены
        WAITING_INFRASTRUCTURE,
        START_HOUSES,         // Здания
        WAITING_HOUSES,
        FINISHED
    }

    private final ServerLevel world;
    private final Settlement settlement;
    private final SettlementStyle style;
    private final int plateauY;

    private Phase currentPhase = Phase.START_TERRAFORM;

    public SettlementBuildTask(ServerLevel world, Settlement settlement, SettlementStyle style) {
        this.world = world;
        this.settlement = settlement;
        this.style = style;
        this.plateauY = PathfindingService.calculateSmartPlateauY(world, settlement.origin, style.maxRadius());
    }

    /**
     * Вызывается каждый тик сервера из контроллера / TickEvent
     */
    public void tick() {
        switch (currentPhase) {

            case START_TERRAFORM -> {
                System.out.println("[SettlementTask] Фаза 1: Запуск терраформинга...");
                TerraformService.terraformSettlementArea(world, settlement, style, plateauY);
                // Блоки отправлены в WorldBuildQueue
                currentPhase = Phase.WAITING_TERRAFORM;
            }

            case WAITING_TERRAFORM -> {
                // Ждем, пока асинхронная очередь полностью уложит терраформинг
                if (WorldBuildQueue.isEmpty()) {
                    System.out.println("[SettlementTask] Терраформинг уложен! Переход к инфраструктуре.");
                    currentPhase = Phase.START_INFRASTRUCTURE;
                }
            }

            case START_INFRASTRUCTURE -> {
                // ТЕПЕРЬ world.getBlockState() и getHeight() видят НОВУЮ землю!
                System.out.println("[SettlementTask] Фаза 2: Генерация дорог и стен...");

                RoadGenerator.generateRoadNetwork(world, settlement, style, plateauY);

                WallGenerator.generateWalls(world, settlement, style, plateauY);

                currentPhase = Phase.WAITING_INFRASTRUCTURE;
            }

            case WAITING_INFRASTRUCTURE -> {
                if (WorldBuildQueue.isEmpty()) {
                    System.out.println("[SettlementTask] Дороги и стены уложены!");
                    currentPhase = Phase.START_HOUSES;
                }
            }
            case START_HOUSES -> {
                System.out.println("[SettlementTask] Фаза 3: Генерация участков и постройка домов...");

                // 1. Сканируем участки сразу в типы BuildingPlot
                List<BuildingPlot> plots = PlotScanner.scanAndAllocatePlots(world, settlement, style);

                // 2. Строим каждый дом и заносим в поселение
                for (BuildingPlot plot : plots) {
                    HouseGenerator.buildPlotStructure(world, plot, style);
                    settlement.getPlots().add(plot);
                }

                currentPhase = Phase.WAITING_HOUSES;
            }
            case WAITING_HOUSES -> {
                if (WorldBuildQueue.isEmpty()) {
                    System.out.println("[SettlementTask] Постройка поселения полностью завершена!");
                    currentPhase = Phase.FINISHED;
                }
            }

            case FINISHED -> {
                // Задача завершена
            }
        }
    }

    public boolean isFinished() {
        return currentPhase == Phase.FINISHED;
    }
}