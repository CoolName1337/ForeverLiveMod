package foreverlive.modid.politics.settlement.plot.builders;

import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import foreverlive.modid.politics.settlement.plot.layout.FloorConfig;
import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;
import foreverlive.modid.politics.settlement.plot.layout.PlacedElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFloorGenerator {

    /**
     * Основной метод генерации этажа
     */
    public abstract void generate(
            BuildingPlot plot,
            FloorLayout floorLayout,
            List<Integer> pillarPositionsX,
            List<Integer> pillarPositionsZ,
            int yOffset
    );

    /**
     * Построение стены (общий метод для всех типов этажей)
     */
    protected void buildWallSpan(
            FloorLayout layout,
            BlockPos startRelPos,
            int spanLength,
            int height,
            Direction stepVector,
            Direction wallFacing,
            boolean isFacade,
            List<Integer> pillarPositions
    ) {
        if (spanLength <= 0) return;

        // 1. Ставим столбы (PILLAR) по скелету
        for (int pillarPos : pillarPositions) {
            BlockPos pos = startRelPos.relative(stepVector, pillarPos);
            layout.add(new PlacedElement(ModuleCategory.PILLAR, pos, 1, height, wallFacing));
        }

        // 2. Заполняем проемы
        int currentIdx = 0;
        int pillarPointer = 0;

        while (currentIdx < spanLength) {
            int nextPillarIdx = (pillarPointer < pillarPositions.size())
                    ? pillarPositions.get(pillarPointer)
                    : spanLength;

            int gapLength = nextPillarIdx - currentIdx;

            if (gapLength > 0) {
                BlockPos gapStartPos = startRelPos.relative(stepVector, currentIdx);

                // Вызываем абстрактный метод (полиморфизм в деле!)
                ModuleCategory category = selectCategoryForGap(gapLength, isFacade, currentIdx, spanLength, layout.getConfig());

                layout.add(new PlacedElement(category, gapStartPos, gapLength, height, wallFacing));
            }

            currentIdx = nextPillarIdx + 1;
            pillarPointer++;
        }
    }

    /**
     * Каждый тип этажа сам определяет, какие категории модулей ставить в проемы
     */
    protected abstract ModuleCategory selectCategoryForGap(
            int gapLength, boolean isFacade, int currentIdx, int totalSpan, FloorConfig config
    );

    /**
     * Расчет скелета столбов
     */
    public static List<Integer> calculatePillarPositions(int spanLength, int maxGapSize) {
        List<Integer> pillars = new ArrayList<>();
        if (spanLength <= maxGapSize) return pillars;

        int numGaps = (int) Math.ceil((double) spanLength / maxGapSize);
        int numPillars = numGaps - 1;

        int remainingForGaps = spanLength - numPillars;
        int baseGapSize = remainingForGaps / numGaps;
        int extraBlocks = remainingForGaps % numGaps;

        int currentPos = 0;
        for (int i = 0; i < numPillars; i++) {
            int gap = baseGapSize + (i < extraBlocks ? 1 : 0);
            currentPos += gap;
            pillars.add(currentPos);
            currentPos += 1;
        }

        return pillars;
    }
}