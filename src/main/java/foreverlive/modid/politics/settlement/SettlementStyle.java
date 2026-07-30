package foreverlive.modid.politics.settlement;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SettlementStyle {
    private final int maxRadius;
    private final int spokeCount;
    private final int roadWidth;
    private final int[] ringRadii;

    private final boolean hasWalls;
    private final boolean hasTowers;
    private final int wallThickness;
    private final int wallLayers;
    private final int wallHeight;

    private final BlockState mainRoadBlock;
    private final BlockState wallBlock;
    private final BlockState wallCapBlock;
    private final BlockState gateBlock;

    private final int centralPlazaRadius;
    private final int plotScanStep;
    private final int plotRoadMargin;

    public SettlementStyle(int maxRadius, int spokeCount, int roadWidth, int[] ringRadii,
                           boolean hasWalls, boolean hasTowers, int wallThickness,
                           int wallLayers, int wallHeight,
                           BlockState mainRoadBlock, BlockState wallBlock,
                           BlockState wallCapBlock, BlockState gateBlock,
                           int centralPlazaRadius,
                           int plotScanStep, int plotRoadMargin, int roadCheckIgnoreRadius) {
        this.maxRadius = maxRadius;
        this.spokeCount = spokeCount;
        this.roadWidth = roadWidth;
        this.ringRadii = ringRadii;
        this.hasWalls = hasWalls;
        this.hasTowers = hasTowers;
        this.wallThickness = wallThickness;
        this.wallLayers = wallLayers;
        this.wallHeight = wallHeight;
        this.mainRoadBlock = mainRoadBlock;
        this.wallBlock = wallBlock;
        this.wallCapBlock = wallCapBlock;
        this.gateBlock = gateBlock;

        this.centralPlazaRadius = centralPlazaRadius;
        this.plotScanStep = plotScanStep;
        this.plotRoadMargin = plotRoadMargin;
    }

    public static SettlementStyle createHamlet() {
        return new SettlementStyle(
                30, 4, 1, new int[]{}, false,
                false, 0, 0, 0,
                Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), null, Blocks.AIR.defaultBlockState(),
                0, 4, 1, 5
        );
    }

    public static SettlementStyle createVillage() {
        return new SettlementStyle(
                50, 4, 2, new int[]{28}, true,
                false, 1,1, 3,
                Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.OAK_WOOD.defaultBlockState(), null, Blocks.AIR.defaultBlockState(),
                8, 4, 2, 6
        );
    }

    public static SettlementStyle createTown() {
        return new SettlementStyle(
                120, 4, 3, new int[]{55}, true,
                false, 2, 1, 5,
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                16, 5, 2, 7
        );
    }

    public static SettlementStyle createCity() {
        return new SettlementStyle(
                200, 6, 4, new int[]{60, 140}, true,
                true, 3,2, 7,
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                24,  5, 2, 8
        );
    }

    public static SettlementStyle createCapital() {
        return new SettlementStyle(
                300, 8, 5, new int[]{60, 150, 230}, true,
                true, 4,3, 9,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState(),
                Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                35,  5, 2, 8
        );
    }

    public int maxRadius() { return maxRadius; }
    public int spokeCount() { return spokeCount; }
    public int roadWidth() { return roadWidth; }
    public int[] ringRadii() { return ringRadii; }
    public boolean hasWalls() { return hasWalls; }

    // ИСПРАВЛЕНО: возвращаем поле hasTowers, а не hasWalls
    public boolean hasTowers() { return hasTowers; }

    public int wallThickness() { return wallThickness; }
    public int wallLayers() { return wallLayers; }
    public int wallHeight() { return wallHeight; }
    public BlockState mainRoadBlock() { return mainRoadBlock; }
    public BlockState wallBlock() { return wallBlock; }
    public BlockState wallCapBlock() { return wallCapBlock; }
    public BlockState gateBlock() { return gateBlock; }

    public int centralPlazaRadius() { return centralPlazaRadius; }
    public int plotScanStep() { return plotScanStep; }
    public int plotRoadMargin() { return plotRoadMargin; }
}