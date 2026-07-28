package foreverlive.modid.politics.builder.services;

import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.builder.SettlementStyle;
import foreverlive.modid.politics.builder.WorldBuildQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildingPlacer {

    public record BuildingPlot(BlockPos origin, int width, int depth, Direction facing) {}

    public static void populateSettlement(ServerLevel world, Settlement settlement, List<List<BlockPos>> allRoads, SettlementStyle style) {
        Set<BlockPos> occupiedPositions = new HashSet<>();

        // 1. Помечаем все блоки дорог и стен как ЗАНЯТЫЕ, чтобы не строить на них
        for (List<BlockPos> road : allRoads) {
            for (BlockPos p : road) {
                markAreaOccupied(occupiedPositions, p, 2); // Запас 2 блока от дороги
            }
        }

        // 2. Строим ЦЕНТРАЛЬНУЮ ПЛОЩАДЬ и Главное Здание
        buildCentralSquare(world, settlement.origin, style, occupiedPositions);

        // 3. Сканируем дороги и размещаем жилые дома вдоль них
        for (List<BlockPos> road : allRoads) {
            // Шагаем по дороге с интервалом в 6-8 блоков, чтобы дома не слипались
            for (int i = 3; i < road.size() - 3; i += 7) {
                BlockPos roadPos = road.get(i);
                BlockPos nextRoadPos = road.get(i + 1);

                // Вычисляем направление дороги
                Direction roadDir = getDirectionFromTo(roadPos, nextRoadPos);
                Direction leftDir = roadDir.getCounterClockWise();
                Direction rightDir = roadDir.getClockWise();

                // Пробуем поставить дом слева и справа от дороги
                tryPlaceHouse(world, settlement, roadPos, leftDir, style, occupiedPositions);
                tryPlaceHouse(world, settlement, roadPos, rightDir, style, occupiedPositions);
            }
        }
    }

    private static void tryPlaceHouse(ServerLevel world, Settlement settlement, BlockPos roadPos, Direction side, SettlementStyle style, Set<BlockPos> occupied) {
        int houseWidth = 5 + (int)(Math.random() * 3); // Длина 5-7 блоков
        int houseDepth = 5 + (int)(Math.random() * 3); // Ширина 5-7 блоков
        int offset = 3; // Отступ от центра дороги

        // Точка переднего левого угла дома
        BlockPos houseOrigin = roadPos.relative(side, offset);
        BlockPos surfaceOrigin = PathfindingService.getSurfacePos(world, houseOrigin);

        // Проверяем: не слишком ли близко к краю плато или за пределами стен
        if (Math.abs(surfaceOrigin.getY() - settlement.origin.getY()) > 6) return;

        // Проверяем свободное место
        if (isAreaFree(surfaceOrigin, houseWidth, houseDepth, occupied)) {
            markAreaOccupied(occupied, surfaceOrigin, houseWidth + 1);

            // 1. Возводим каменный фундамент под рельеф
            buildFoundation(world, surfaceOrigin, houseWidth, houseDepth, style.wallBlock());

            // 2. Генерируем сам корпус дома
            buildSimpleMedievalHouse(world, surfaceOrigin, houseWidth, houseDepth, side.getOpposite(), style);
        }
    }

    /**
     * Заполняет пустоту под домом фундаментальными блоками до первого твердого блока
     */
    private static void buildFoundation(ServerLevel world, BlockPos origin, int width, int depth, BlockState foundationBlock) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockPos top = origin.offset(x, 0, z);

                // Спускаемся вниз на 10 блоков, заделывая обрывы
                for (int y = 0; y <= 10; y++) {
                    BlockPos check = top.below(y);
                    if (world.getBlockState(check).isSolidRender()) {
                        break; // Дошли до земли
                    }
                    WorldBuildQueue.enqueue(check, foundationBlock);
                }
            }
        }
    }

    /**
     * Каркас классического средневекового фахверкового дома (Procedural Box)
     */
    private static void buildSimpleMedievalHouse(ServerLevel world, BlockPos origin, int width, int depth, Direction doorFacing, SettlementStyle style) {
        int height = 4;

        // Стены и угловые бревна (Log Pillar + Planks/Plaster)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean isCorner = (x == 0 || x == width - 1) && (z == 0 || z == depth - 1);
                boolean isEdge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;

                if (isEdge) {
                    for (int y = 1; y <= height; y++) {
                        BlockPos p = origin.offset(x, y, z);
                        if (isCorner) {
                            WorldBuildQueue.enqueue(p, Blocks.STRIPPED_OAK_LOG.defaultBlockState());
                        } else {
                            WorldBuildQueue.enqueue(p, Blocks.WHITE_CONCRETE.defaultBlockState()); // Фахверк / Трамбованная глина
                        }
                    }
                } else {
                    // Пол внутри дома
                    WorldBuildQueue.enqueue(origin.offset(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }

        // Простая двухскатная крыша (Stairs/Slabs)
        for (int y = 0; y <= width / 2; y++) {
            for (int z = -1; z <= depth; z++) {
                WorldBuildQueue.enqueue(origin.offset(y, height + 1 + y, z), Blocks.DARK_OAK_STAIRS.defaultBlockState());
                WorldBuildQueue.enqueue(origin.offset(width - 1 - y, height + 1 + y, z), Blocks.DARK_OAK_STAIRS.defaultBlockState());
            }
        }
    }

    private static void buildCentralSquare(ServerLevel world, BlockPos origin, SettlementStyle style, Set<BlockPos> occupied) {
        BlockPos surfaceOrigin = PathfindingService.getSurfacePos(world, origin);
        int radius = 6;

        // Мостим площадь каменной брусчаткой
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    BlockPos p = PathfindingService.getSurfacePos(world, surfaceOrigin.offset(x, 0, z));
                    WorldBuildQueue.enqueue(p, Blocks.COBBLESTONE.defaultBlockState());
                    occupied.add(p);
                }
            }
        }
    }

    private static boolean isAreaFree(BlockPos pos, int w, int d, Set<BlockPos> occupied) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (occupied.contains(pos.offset(x, 0, z))) return false;
            }
        }
        return true;
    }

    private static void markAreaOccupied(Set<BlockPos> occupied, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                occupied.add(center.offset(x, 0, z));
            }
        }
    }

    private static Direction getDirectionFromTo(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
}