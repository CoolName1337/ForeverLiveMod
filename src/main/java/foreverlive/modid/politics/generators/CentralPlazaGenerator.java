package foreverlive.modid.politics.generators;

import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.settlement.SettlementStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class CentralPlazaGenerator {

    public static void generateCapitalCenter(ServerLevel world, Settlement settlement, SettlementStyle style) {
        BlockPos center = settlement.origin;
        int plazaRadius = 22; // Замощаем круг/квадрат в центре под площадь
//
//        // 1. Выравниваем и замощаем центральную площадь каменной плиткой
//        for (int x = -plazaRadius; x <= plazaRadius; x++) {
//            for (int z = -plazaRadius; z <= plazaRadius; z++) {
//                if (Math.hypot(x, z) <= plazaRadius) {
//                    BlockPos pos = PathfindingService.getSurfacePos(world, center.offset(x, 0, z));
//                    WorldBuildQueue.enqueue(pos, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
//                }
//            }
//        }
//
//        // 2. Размечаем главный фундамент под Замок / Ратушу (например, 20x20 прямо в центре)
//        int buildingSize = 10;
//        for (int x = -buildingSize; x <= buildingSize; x++) {
//            WorldBuildQueue.enqueue(PathfindingService.getSurfacePos(world, center.offset(x, 0, -buildingSize)), Blocks.GOLD_BLOCK.defaultBlockState());
//            WorldBuildQueue.enqueue(PathfindingService.getSurfacePos(world, center.offset(x, 0, buildingSize)), Blocks.GOLD_BLOCK.defaultBlockState());
//        }
//        for (int z = -buildingSize; z <= buildingSize; z++) {
//            WorldBuildQueue.enqueue(PathfindingService.getSurfacePos(world, center.offset(-buildingSize, 0, z)), Blocks.GOLD_BLOCK.defaultBlockState());
//            WorldBuildQueue.enqueue(PathfindingService.getSurfacePos(world, center.offset(buildingSize, 0, z)), Blocks.GOLD_BLOCK.defaultBlockState());
//        }
    }
}