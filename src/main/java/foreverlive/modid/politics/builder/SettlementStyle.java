package foreverlive.modid.politics.builder;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SettlementStyle {
    private final int maxRadius;
    private final int spokeCount;
    private final int roadWidth;
    private final int[] ringRadii;

    private final boolean hasWalls;
    private final int wallLayers;
    private final int wallHeight;
    private final int innerWallRadius; // Отдельный радиус для внутренней стены!

    private final BlockState mainRoadBlock;
    private final BlockState wallBlock;
    private final BlockState wallCapBlock;
    private final BlockState gateBlock;

    private final int centralPlazaRadius;
    private final int outerWallMargin;
    private final int innerWallBufferRadius;
    private final int plotScanStep;
    private final int plotRoadMargin;
    private final int roadCheckIgnoreRadius;

    public SettlementStyle(int maxRadius, int spokeCount, int roadWidth, int[] ringRadii,
                           boolean hasWalls, int wallLayers, int wallHeight, int innerWallRadius,
                           BlockState mainRoadBlock, BlockState wallBlock,
                           BlockState wallCapBlock, BlockState gateBlock,
                           int centralPlazaRadius, int outerWallMargin, int innerWallBufferRadius,
                           int plotScanStep, int plotRoadMargin, int roadCheckIgnoreRadius) {
        this.maxRadius = maxRadius;
        this.spokeCount = spokeCount;
        this.roadWidth = roadWidth;
        this.ringRadii = ringRadii;
        this.hasWalls = hasWalls;
        this.wallLayers = wallLayers;
        this.wallHeight = wallHeight;
        this.innerWallRadius = innerWallRadius;
        this.mainRoadBlock = mainRoadBlock;
        this.wallBlock = wallBlock;
        this.wallCapBlock = wallCapBlock;
        this.gateBlock = gateBlock;

        this.centralPlazaRadius = centralPlazaRadius;
        this.outerWallMargin = outerWallMargin;
        this.innerWallBufferRadius = innerWallBufferRadius;
        this.plotScanStep = plotScanStep;
        this.plotRoadMargin = plotRoadMargin;
        this.roadCheckIgnoreRadius = roadCheckIgnoreRadius;
    }

    public static SettlementStyle createHamlet() {
        return new SettlementStyle(
                30, 4, 1, new int[]{}, false, 0, 0, 0,
                Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.AIR.defaultBlockState(), null, Blocks.AIR.defaultBlockState(),
                0, 2, 0, 4, 1, 5
        );
    }

    public static SettlementStyle createVillage() {
        return new SettlementStyle(
                50, 4, 2, new int[]{28}, true, 1, 3, 0,
                Blocks.DIRT_PATH.defaultBlockState(),
                Blocks.OAK_WOOD.defaultBlockState(), null, Blocks.AIR.defaultBlockState(),
                8, 4, 4, 4, 2, 6
        );
    }

    public static SettlementStyle createTown() {
        return new SettlementStyle(
                120, 4, 3, new int[]{55}, true, 1, 5, 0,
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                16, 6, 6, 5, 2, 7
        );
    }

    // Внутреннее кольцо на 65, а стена между 1 и 2 кольцом на 100
    public static SettlementStyle createCity() {
        return new SettlementStyle(
                200, 6, 4, new int[]{60, 140}, true, 2, 7, 100,
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.STONE_BRICK_SLAB.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                24, 6, 8, 5, 2, 8
        );
    }

    // Кольца на 60, 150, 230. Внутренняя стена аккуратно на 105 (между 1 и 2 кольцом!)
    public static SettlementStyle createCapital() {
        return new SettlementStyle(
                300, 8, 5, new int[]{60, 150, 230}, true, 3, 9, 105,
                Blocks.SMOOTH_STONE.defaultBlockState(),
                Blocks.STONE_BRICKS.defaultBlockState(),
                Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState(),
                Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                35, 8, 10, 5, 2, 8
        );
    }

    public int maxRadius() { return maxRadius; }
    public int spokeCount() { return spokeCount; }
    public int roadWidth() { return roadWidth; }
    public int[] ringRadii() { return ringRadii; }
    public boolean hasWalls() { return hasWalls; }
    public int wallLayers() { return wallLayers; }
    public int wallHeight() { return wallHeight; }
    public BlockState mainRoadBlock() { return mainRoadBlock; }
    public BlockState wallBlock() { return wallBlock; }
    public BlockState wallCapBlock() { return wallCapBlock; }
    public BlockState gateBlock() { return gateBlock; }

    public int centralPlazaRadius() { return centralPlazaRadius; }
    public int outerWallMargin() { return outerWallMargin; }
    public int innerWallBufferRadius() { return innerWallBufferRadius; }
    public int plotScanStep() { return plotScanStep; }
    public int plotRoadMargin() { return plotRoadMargin; }
    public int roadCheckIgnoreRadius() { return roadCheckIgnoreRadius; }

    public boolean hasInnerWall() {
        return hasWalls && wallLayers > 1 && innerWallRadius > 0;
    }

    public int innerWallRadius() {
        return innerWallRadius;
    }
}