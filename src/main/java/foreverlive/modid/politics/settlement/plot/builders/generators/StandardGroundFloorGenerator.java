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

public class StandardGroundFloorGenerator extends AbstractFloorGenerator {

    @Override
    public void generate(BuildingPlot plot, FloorLayout floorLayout, List<Integer> pillarPositionsX, List<Integer> pillarPositionsZ, int yOffset) {
        int widthX = plot.getWidthX();
        int lengthZ = plot.getLengthZ();
        int height = floorLayout.getConfig().height();
        Direction mainFacing = plot.getFacing();

        int spanX = widthX - 2;
        int spanZ = lengthZ - 2;

        // 4 Угла (CORNER)
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(0, yOffset, lengthZ - 1), 1, height, Direction.SOUTH));
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(0, yOffset, 0), 1, height, Direction.WEST));
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(widthX - 1, yOffset, 0), 1, height, Direction.NORTH));
        floorLayout.add(new PlacedElement(ModuleCategory.CORNER, new BlockPos(widthX - 1, yOffset, lengthZ - 1), 1, height, Direction.EAST));

        // Стены
        buildWallSpan(floorLayout, new BlockPos(1, yOffset, 0), spanX, height, Direction.EAST, Direction.NORTH, mainFacing == Direction.NORTH, pillarPositionsX);
        buildWallSpan(floorLayout, new BlockPos(1, yOffset, lengthZ - 1), spanX, height, Direction.EAST, Direction.SOUTH, mainFacing == Direction.SOUTH, pillarPositionsX);
        buildWallSpan(floorLayout, new BlockPos(0, yOffset, 1), spanZ, height, Direction.SOUTH, Direction.WEST, mainFacing == Direction.WEST, pillarPositionsZ);
        buildWallSpan(floorLayout, new BlockPos(widthX - 1, yOffset, 1), spanZ, height, Direction.SOUTH, Direction.EAST, mainFacing == Direction.EAST, pillarPositionsZ);
    }

    @Override
    protected ModuleCategory selectCategoryForGap(int gapLength, boolean isFacade, int currentIdx, int totalSpan, FloorConfig config) {
        boolean isCenterGap = Math.abs((currentIdx + gapLength / 2.0) - (totalSpan / 2.0)) <= 2.0;

        if (isFacade && isCenterGap && config.allowDoors()) {
            return ModuleCategory.DOOR;
        }
        return gapLength >= 2 ? ModuleCategory.WINDOW : ModuleCategory.WALL;
    }
}