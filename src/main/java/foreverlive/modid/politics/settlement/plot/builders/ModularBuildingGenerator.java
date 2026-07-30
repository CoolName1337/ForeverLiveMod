package foreverlive.modid.politics.settlement.plot.builders;

import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.plot.builders.generators.StandardGroundFloorGenerator;
import foreverlive.modid.politics.settlement.plot.builders.generators.StandardUpperFloorGenerator;
import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;

import java.util.List;

public class ModularBuildingGenerator {

    private static final AbstractFloorGenerator GROUND_GEN = new StandardGroundFloorGenerator();
    private static final AbstractFloorGenerator UPPER_GEN = new StandardUpperFloorGenerator();

    public static void generateBuilding(BuildingPlot plot) {
        int spanX = plot.getWidthX() - 2;
        int spanZ = plot.getLengthZ() - 2;

        // 1. Скелет рассчитывается ЕДИНОЖДЫ для обеспечения вертикальной соосности
        List<Integer> pillarPositionsX = AbstractFloorGenerator.calculatePillarPositions(spanX, 3);
        List<Integer> pillarPositionsZ = AbstractFloorGenerator.calculatePillarPositions(spanZ, 3);

        int currentYOffset = 0;
        List<FloorLayout> floors = plot.getFloors();

        // 2. Итерируемся по этажам плота
        for (int i = 0; i < floors.size(); i++) {
            FloorLayout floorLayout = floors.get(i);

            // Подбираем нужную стратегию генерации
            AbstractFloorGenerator generator = (i == 0) ? GROUND_GEN : UPPER_GEN;

            // Генерируем текущий этаж
            generator.generate(plot, floorLayout, pillarPositionsX, pillarPositionsZ, currentYOffset);

            // Динамически увеличиваем Y-смещение на высоту ТЕКУЩЕГО этажа
            currentYOffset += floorLayout.getConfig().height();
        }
    }
}