package foreverlive.modid.politics.settlement.plot.builders;

import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import foreverlive.modid.politics.settlement.plot.layout.BuildingLayout;
import foreverlive.modid.politics.settlement.plot.layout.PlacedElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;


public class MultiFloorGenerator {

    public static BuildingLayout generate(BuildingPlot plot) {
        BuildingLayout fullLayout = new BuildingLayout();
        int floorsCount = plot.getFloorsCount(); // Например, 2 или 3
        int floorHeight = plot.getHeightPerFloor(); // Например, 4 блока

        // 1. Сначала рассчитываем ЕДИНЫЙ скелет столбов для всего дома
        int spanX = plot.getWidthX() - 2;
        int spanZ = plot.getLengthZ() - 2;

        // Получаем сетку столбов один раз, чтобы она совпадала на всех этажах
        List<Integer> pillarPositionsX = SkeletonLayoutGenerator.calculatePillarPositions(spanX);
        List<Integer> pillarPositionsZ = SkeletonLayoutGenerator.calculatePillarPositions(spanZ);

        // 2. Генерируем этаж за этажом
        for (int floor = 0; floor < floorsCount; floor++) {
            int yOffset = floor * floorHeight;
            boolean isGroundFloor = (floor == 0);
            boolean isTopFloor = (floor == floorsCount - 1);

            // Передаем зафиксированные позиции столбов в генератор стены
            buildFloorWalls(
                    fullLayout,
                    plot,
                    yOffset,
                    floor,
                    pillarPositionsX,
                    pillarPositionsZ
            );

            // 3. Добавляем межэтажное перекрытие (Пол) + проем под лестницу
            buildFloorSlab(fullLayout, plot, yOffset, isGroundFloor);
        }

        // 4. Поверх последнего этажа накидываем крышу
        RoofGenerator.generate(fullLayout, plot, floorsCount * floorHeight);

        return fullLayout;
    }

    private static void buildFloorWalls(
            BuildingLayout layout,
            BuildingPlot plot,
            int yOffset,
            int floorIndex,
            List<Integer> pillarPositionsX,
            List<Integer> pillarPositionsZ
    ) {
        int widthX = plot.getWidthX();
        int lengthZ = plot.getLengthZ();
        int height = plot.getHeightPerFloor();
        Direction mainFacing = plot.getFacing();

        int spanX = widthX - 2;
        int spanZ = lengthZ - 2;
        boolean isGroundFloor = (floorIndex == 0);

        // 1. Ставим 4 Угла (CORNER)
        layout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(0, yOffset, lengthZ - 1), 1, height, Direction.SOUTH));
        layout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(0, yOffset, 0), 1, height, Direction.WEST));
        layout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(widthX - 1, yOffset, 0), 1, height, Direction.NORTH));
        layout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(widthX - 1, yOffset, lengthZ - 1), 1, height, Direction.EAST));

        // 2. Генерируем 4 пролета стен
        // Север (Z = 0)
        SkeletonLayoutGenerator.buildWallSpan(layout, new BlockPos(1, yOffset, 0), spanX, height,
                Direction.EAST, Direction.NORTH, (isGroundFloor && mainFacing == Direction.NORTH), pillarPositionsX);

        // Юг (Z = lengthZ - 1)
        SkeletonLayoutGenerator.buildWallSpan(layout, new BlockPos(1, yOffset, lengthZ - 1), spanX, height,
                Direction.EAST, Direction.SOUTH, (isGroundFloor && mainFacing == Direction.SOUTH), pillarPositionsX);

        // Запад (X = 0)
        SkeletonLayoutGenerator.buildWallSpan(layout, new BlockPos(0, yOffset, 1), spanZ, height,
                Direction.SOUTH, Direction.WEST, (isGroundFloor && mainFacing == Direction.WEST), pillarPositionsZ);

        // Восток (X = widthX - 1)
        SkeletonLayoutGenerator.buildWallSpan(layout, new BlockPos(widthX - 1, yOffset, 1), spanZ, height,
                Direction.SOUTH, Direction.EAST, (isGroundFloor && mainFacing == Direction.EAST), pillarPositionsZ);
    }
}