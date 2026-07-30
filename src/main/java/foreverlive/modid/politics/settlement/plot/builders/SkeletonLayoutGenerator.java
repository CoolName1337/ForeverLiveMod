package foreverlive.modid.politics.settlement.plot.builders;

import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import foreverlive.modid.politics.settlement.plot.layout.BuildingLayout;
import foreverlive.modid.politics.settlement.plot.layout.PlacedElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class SkeletonLayoutGenerator {

    /**
     * Генерирует пролет стены через Скелетную модель (Node-First)
     */
    public static void buildWallSpan(
            BuildingLayout layout,
            BlockPos startRelPos,
            int spanLength,
            int height,
            Direction stepVector,
            Direction wallFacing,
            boolean isFacade,
            List<Integer> pillarPositions
    ) {
        if (spanLength <= 0) return;

        // 1. Ставим Пиллары строго по переданному скелету
        for (int pillarPos : pillarPositions) {
            BlockPos pos = startRelPos.relative(stepVector, pillarPos);
            layout.add(new PlacedElement(ModuleCategory.PILLAR, pos, 1, height, wallFacing));
        }

        // 2. Заполняем проемы МЕЖДУ столбами
        int currentIdx = 0;
        int pillarPointer = 0;

        while (currentIdx < spanLength) {
            int nextPillarIdx = (pillarPointer < pillarPositions.size())
                    ? pillarPositions.get(pillarPointer)
                    : spanLength;

            int gapLength = nextPillarIdx - currentIdx;

            if (gapLength > 0) {
                BlockPos gapStartPos = startRelPos.relative(stepVector, currentIdx);
                ModuleCategory category = selectCategoryForGap(gapLength, isFacade, currentIdx, spanLength);

                layout.add(new PlacedElement(category, gapStartPos, gapLength, height, wallFacing));
            }

            currentIdx = nextPillarIdx + 1;
            pillarPointer++;
        }
    }

    /**
     * Алгоритм расстановки столбов: гарантирует, что расстояние между столбами <= 3
     */
    public static List<Integer> calculatePillarPositions(int spanLength, int maxGapSize) {
        List<Integer> pillars = new ArrayList<>();
        if (spanLength <= maxGapSize) {
            return pillars;
        }

        // 1. Считаем количество проемов и столбов
        int numGaps = (int) Math.ceil((double) spanLength / maxGapSize);
        int numPillars = numGaps - 1;

        // 2. Рассчитываем суммарную ширину проемов за вычетом самих столбов (шириной 1)
        int remainingForGaps = spanLength - numPillars;
        int baseGapSize = remainingForGaps / numGaps;
        int extraBlocks = remainingForGaps % numGaps; // Остаток, который распределяем по первым проемам

        // 3. Точно расставляем позиции столбов
        int currentPos = 0;
        for (int i = 0; i < numPillars; i++) {
            int gap = baseGapSize + (i < extraBlocks ? 1 : 0);
            currentPos += gap;
            pillars.add(currentPos);
            currentPos += 1; // Пропускаем сам столб
        }

        return pillars;
    }

    /**
     * Выбор типа модуля в зависимости от ширины проема и фасада
     */
    private static ModuleCategory selectCategoryForGap(int gapLength, boolean isFacade, int currentIdx, int totalSpan) {
        // Если это фасад и проем находится ближе к центру — ставим Дверь
        boolean isCenterGap = Math.abs((currentIdx + gapLength / 2.0) - (totalSpan / 2.0)) <= 2.0;

        if (isFacade && isCenterGap) {
            return ModuleCategory.DOOR;
        }

        // Если проем широкий (2 или 3) — можно чередовать Окна и Стены
        if (gapLength >= 2 && !isFacade) {
            return ModuleCategory.WINDOW;
        }

        return ModuleCategory.WALL;
    }
}