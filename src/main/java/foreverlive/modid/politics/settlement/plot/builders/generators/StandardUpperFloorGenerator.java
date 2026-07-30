package foreverlive.modid.politics.settlement.plot.builders.generators;

import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import foreverlive.modid.politics.settlement.plot.builders.AbstractFloorGenerator;
import foreverlive.modid.politics.settlement.plot.layout.FloorConfig;
import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;
import foreverlive.modid.politics.settlement.plot.layout.PlacedElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public class StandardUpperFloorGenerator extends AbstractFloorGenerator {

    @Override
    public void generate(BuildingPlot plot, FloorLayout floorLayout, List<Integer> pillarPositionsX, List<Integer> pillarPositionsZ, int yOffset) {
        int widthX = plot.getWidthX();
        int lengthZ = plot.getLengthZ();
        int height = floorLayout.getConfig().height();

        int spanX = widthX - 2;
        int spanZ = lengthZ - 2;

        // 4 Угла
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(0, yOffset, lengthZ - 1), 1, height, Direction.SOUTH));
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(0, yOffset, 0), 1, height, Direction.WEST));
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(widthX - 1, yOffset, 0), 1, height, Direction.NORTH));
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(widthX - 1, yOffset, lengthZ - 1), 1, height, Direction.EAST));

        // Стены (на 2 этаже фасада нет, поэтому isFacade = false)
        buildWallSpan(floorLayout, new BlockPos(1, yOffset, 0), spanX, height, Direction.EAST, Direction.NORTH, false, pillarPositionsX);
        buildWallSpan(floorLayout, new BlockPos(1, yOffset, lengthZ - 1), spanX, height, Direction.EAST, Direction.SOUTH, false, pillarPositionsX);
        buildWallSpan(floorLayout, new BlockPos(0, yOffset, 1), spanZ, height, Direction.SOUTH, Direction.WEST, false, pillarPositionsZ);
        buildWallSpan(floorLayout, new BlockPos(widthX - 1, yOffset, 1), spanZ, height, Direction.SOUTH, Direction.EAST, false, pillarPositionsZ);
    }

    @Override
    protected ModuleCategory selectCategoryForGap(int gapLength, boolean isFacade, int currentIdx, int totalSpan, FloorConfig config) {
        // На верхних этажах дверей нет — только больше окон
        return gapLength >= 2 ? ModuleCategory.WINDOW : ModuleCategory.WALL;
    }
}