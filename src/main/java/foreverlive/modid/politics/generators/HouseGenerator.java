package foreverlive.modid.politics.generators;

import foreverlive.modid.politics.services.WorldBuildQueue;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.settlement.enums.PlotType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class HouseGenerator {

    public static void buildPlotStructure(ServerLevel world, BuildingPlot plot, SettlementStyle style) {
        BlockPos min = plot.getMinPos();
        BlockPos max = plot.getMaxPos();

        int minX = min.getX();
        int maxX = max.getX();
        int minZ = min.getZ();
        int maxZ = max.getZ();

        int groundY = plot.getAnchorPos().getY();
        int wallHeight = (plot.getType() == PlotType.TOWN_HALL || plot.getType() == PlotType.TAVERN) ? 5 : 4;
        int topY = groundY + wallHeight;

        // Палитра материалов в зависимости от типа участка
        BlockState foundationBlock = getFoundationBlock(plot.getType());
        BlockState wallBlock = getWallBlock(plot.getType(), style);
        BlockState floorBlock = getFloorBlock(plot.getType());

        // 1. Фундамент, пол и очистка воздуха
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                WorldBuildQueue.enqueue(new BlockPos(x, groundY - 1, z), foundationBlock);
                WorldBuildQueue.enqueue(new BlockPos(x, groundY, z), floorBlock);

                for (int y = groundY + 1; y <= topY + 3; y++) {
                    WorldBuildQueue.enqueue(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // 2. Стены и угловые столбы
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isEdge = (x == minX || x == maxX || z == minZ || z == maxZ);
                if (isEdge) {
                    for (int y = groundY + 1; y <= topY; y++) {
                        boolean isCorner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
                        BlockState logBlock = Blocks.OAK_LOG.defaultBlockState();
                        WorldBuildQueue.enqueue(new BlockPos(x, y, z), isCorner ? logBlock : wallBlock);
                    }
                }
            }
        }

        // 3. Скатная крыша (вместо полублоков)
        buildRoof(minX, maxX, minZ, maxZ, topY);

        // 4. Дверь
        BlockPos doorPos = plot.getAnchorPos();
        Direction facing = Direction.NORTH; // Направление к дороге

        WorldBuildQueue.enqueue(doorPos.above(1), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        WorldBuildQueue.enqueue(doorPos.above(2), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // 5. Функциональное наполнение интерьера под задачи НПС
        buildInterior(plot, minX, maxX, minZ, maxZ, groundY, facing);
    }

    /**
     * Заполнение здания рабочими станциями, кроватью и складами
     */
    private static void buildInterior(BuildingPlot plot, int minX, int maxX, int minZ, int maxZ, int groundY, Direction facing) {
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int y = groundY + 1;

        switch (plot.getType()) {
            case RESIDENTIAL:
                // Спальные места (3 кровати вдоль стен)
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, minZ + 1), Blocks.RED_BED.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(maxX - 1, y, minZ + 1), Blocks.RED_BED.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, maxZ - 1), Blocks.RED_BED.defaultBlockState());
                // Сундук для личных вещей
                WorldBuildQueue.enqueue(new BlockPos(centerX, y, maxZ - 1), Blocks.CHEST.defaultBlockState());
                break;

            case FORGE:
                // Рабочая зона кузнеца: Плавильня, Наковальня, Верстак, Сундук
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, minZ + 1), Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH));
                WorldBuildQueue.enqueue(new BlockPos(minX + 2, y, minZ + 1), Blocks.ANVIL.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, minZ + 2), Blocks.CRAFTING_TABLE.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(maxX - 1, y, maxZ - 1), Blocks.BARREL.defaultBlockState());
                break;

            case FARM:
                // Ферма: Грядки внутри/снаружи, Вода, Сундук с семенами
                WorldBuildQueue.enqueue(new BlockPos(centerX, groundY, centerZ), Blocks.WATER.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(centerX + 1, y, centerZ), Blocks.FARMLAND.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(centerX - 1, y, centerZ), Blocks.FARMLAND.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, minZ + 1), Blocks.COMPOSTER.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(maxX - 1, y, maxZ - 1), Blocks.CHEST.defaultBlockState());
                break;

            case TAVERN:
                // Трактир: Бочки, Стойка, Столы
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, minZ + 1), Blocks.BREWING_STAND.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(minX + 1, y, minZ + 2), Blocks.BARREL.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(centerX, y, centerZ), Blocks.OAK_WALL_SIGN.defaultBlockState()); // Если есть блочные столы или забор+нажимная плита
                break;

            case TOWN_HALL:
                // Ратуша: Главный сундук казны города, Кафедра
                WorldBuildQueue.enqueue(new BlockPos(centerX, y, maxZ - 1), Blocks.LECTERN.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(centerX - 1, y, maxZ - 1), Blocks.CHEST.defaultBlockState());
                WorldBuildQueue.enqueue(new BlockPos(centerX + 1, y, maxZ - 1), Blocks.CHEST.defaultBlockState());
                break;
        }

        // Освещение (Факелы)
        WorldBuildQueue.enqueue(new BlockPos(centerX, groundY + 3, centerZ), Blocks.LANTERN.defaultBlockState());
    }

    private static void buildRoof(int minX, int maxX, int minZ, int maxZ, int topY) {
        // Коньковая скатная крыша из ступеней
//        int height = 0;
//        for (int x = minX; x <= maxX; x++) {
//            for (int z = minZ; z <= maxZ; z++) {
//                WorldBuildQueue.enqueue(new BlockPos(x, topY + 1, z), Blocks.OAK_PLANKS.defaultBlockState());
//            }
//        }
    }

    private static BlockState getFoundationBlock(PlotType type) {
        return type == PlotType.TOWN_HALL ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
    }

    private static BlockState getWallBlock(PlotType type, SettlementStyle style) {
        if (type == PlotType.FORGE) return Blocks.BRICKS.defaultBlockState();
        if (type == PlotType.TOWN_HALL) return Blocks.STONE_BRICKS.defaultBlockState();
        return style.wallBlock();
    }

    private static BlockState getFloorBlock(PlotType type) {
        if (type == PlotType.FORGE) return Blocks.COBBLESTONE.defaultBlockState();
        if (type == PlotType.FARM) return Blocks.DIRT.defaultBlockState();
        return Blocks.OAK_PLANKS.defaultBlockState();
    }
}