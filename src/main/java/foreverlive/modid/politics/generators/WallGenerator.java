package foreverlive.modid.politics.generators;

import foreverlive.modid.politics.POJO.RoadSegment;
import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.services.WorldBuildQueue;
import foreverlive.modid.politics.services.PathfindingService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WallGenerator {

    public static void generateWalls(ServerLevel world, Settlement settlement, SettlementStyle style, int plateauY) {
        if (settlement == null || settlement.origin == null) return;

        // Если у стиля нет стен — чистим позиции и выходим
        if (!style.hasWalls()) {
            settlement.getWallPositions().clear();
            return;
        }

        List<RoadSegment> ringSegments = settlement.getRoads().stream()
                .filter(r -> r.getType() == RoadSegment.RoadType.RING)
                .toList();

        if (ringSegments.isEmpty()) return;

        BlockPos center = new BlockPos(settlement.origin.getX(), plateauY, settlement.origin.getZ());
        Set<Long> roadXZPositions = collectRoadPositions2D(settlement.getRoads());
        Set<Long> generatedWallPositions = new HashSet<>();

        int wallHeight = style.wallHeight() > 0 ? style.wallHeight() : 5;
        int wallThickness = style.wallThickness() > 0 ? style.wallThickness() : 3;
        int towerInterval = 20; // Шаг между башнями на стене
        boolean enableTowers = style.hasTowers();

        BlockState wallMaterial = style.wallBlock() != null ? style.wallBlock() : Blocks.STONE_BRICKS.defaultBlockState();
        BlockState towerMaterial = style.gateBlock() != null ? style.gateBlock() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();

        int offsetDistance = (style.roadWidth() / 2) + 3; // Безопасный отступ стены от края дороги

        for (RoadSegment ringSegment : ringSegments) {
            List<BlockPos> ringRoad = ringSegment.getPoints();
            if (ringRoad == null || ringRoad.isEmpty()) continue;

            List<BlockPos> offsetRing = createOffsetWallPath(world, ringRoad, center, offsetDistance, plateauY);
            if (!offsetRing.isEmpty()) {
                List<BlockPos> denseWallPath = interpolatePath(world, offsetRing, plateauY);
                if (!denseWallPath.isEmpty()) {
                    buildThickWallAndTowers(world, denseWallPath, roadXZPositions, wallHeight, wallThickness,
                            towerInterval, enableTowers, style.roadWidth(), wallMaterial, towerMaterial, generatedWallPositions);
                }
            }
        }

        settlement.setWallPositions(generatedWallPositions);
    }

    private static void buildThickWallAndTowers(ServerLevel world, List<BlockPos> path, Set<Long> roadXZPositions,
                                                int height, int thickness, int towerInterval, boolean enableTowers,
                                                int roadWidth, BlockState wallMat, BlockState towerMat, Set<Long> wallPositionsOut) {

        int stepsSinceTower = towerInterval;
        boolean wasInRoad = false;
        int roadThreshold = (roadWidth / 2) + 1;

        for (int i = 0; i < path.size(); i++) {
            BlockPos current = path.get(i);
            if (current == null) continue;

            boolean isRoad = isNearRoad2D(current.getX(), current.getZ(), roadXZPositions, roadThreshold);

            // Пропуск стены над дорогами (создание ворот)
            if (isRoad) {
                if (!wasInRoad) {
                    if (enableTowers && i > 1) {
                        buildTower(world, path.get(i - 1), height, towerMat, wallPositionsOut);
                    }
                    wasInRoad = true;
                }
                continue;
            } else {
                if (wasInRoad) {
                    if (enableTowers && i < path.size()) {
                        buildTower(world, current, height, towerMat, wallPositionsOut);
                    }
                    wasInRoad = false;
                    stepsSinceTower = 0;
                    continue;
                }
            }

            BlockPos next = path.get((i + 1) % path.size());
            double dx = next.getX() - current.getX();
            double dz = next.getZ() - current.getZ();
            double len = Math.hypot(dx, dz);

            double nx = len > 0 ? -dz / len : 0;
            double nz = len > 0 ? dx / len : 0;

            stepsSinceTower++;
            if (enableTowers && stepsSinceTower >= towerInterval) {
                buildTower(world, current, height, towerMat, wallPositionsOut);
                stepsSinceTower = 0;
            } else {
                boolean addCrenel = (i % 2 == 0);
                buildThickWallColumn(world, current, nx, nz, height, thickness, wallMat, addCrenel, wallPositionsOut);
            }
        }
    }

    private static void buildThickWallColumn(ServerLevel world, BlockPos centerPos, double nx, double nz,
                                             int height, int thickness, BlockState material, boolean addCrenel,
                                             Set<Long> wallPositionsOut) {

        int half = thickness / 2;
        for (int t = -half; t <= half; t++) {
            int offsetX = (int) Math.round(nx * t);
            int offsetZ = (int) Math.round(nz * t);
            BlockPos columnPos = centerPos.offset(offsetX, 0, offsetZ);

            wallPositionsOut.add(pack2D(columnPos.getX(), columnPos.getZ()));

            // Фундамент вниз до твердого блока
            for (int down = 1; down <= 6; down++) {
                BlockPos under = columnPos.below(down);
                if (world.getBlockState(under).isSolidRender()) break;
                WorldBuildQueue.enqueue(under, Blocks.COBBLESTONE.defaultBlockState());
            }

            // Основное тело стены
            for (int h = 0; h < height; h++) {
                WorldBuildQueue.enqueue(columnPos.above(h), material);
            }

            // Зубцы стены
            if (addCrenel && (t == -half || t == half)) {
                WorldBuildQueue.enqueue(columnPos.above(height), material);
            }
        }
    }

    private static void buildTower(ServerLevel world, BlockPos centerSurface, int wallHeight, BlockState material, Set<Long> wallPositionsOut) {
        int towerHeight = wallHeight + 3;
        int radius = 2; // Башня 5x5

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = centerSurface.offset(x, 0, z);
                wallPositionsOut.add(pack2D(pos.getX(), pos.getZ()));

                boolean isEdge = (Math.abs(x) == radius || Math.abs(z) == radius);
                if (!isEdge) continue;

                for (int down = 1; down <= 6; down++) {
                    BlockPos under = pos.below(down);
                    if (world.getBlockState(under).isSolidRender()) break;
                    WorldBuildQueue.enqueue(under, Blocks.COBBLESTONE.defaultBlockState());
                }

                for (int h = 0; h < towerHeight; h++) {
                    WorldBuildQueue.enqueue(pos.above(h), material);
                }

                if ((x + z) % 2 == 0) {
                    WorldBuildQueue.enqueue(pos.above(towerHeight), material);
                }
            }
        }
    }

    private static List<BlockPos> createOffsetWallPath(ServerLevel world, List<BlockPos> ringRoad, BlockPos center, int offsetDistance, int plateauY) {
        List<BlockPos> offsetPath = new ArrayList<>();
        for (BlockPos roadPos : ringRoad) {
            if (roadPos == null) continue;

            double dx = roadPos.getX() - center.getX();
            double dz = roadPos.getZ() - center.getZ();
            double dist = Math.hypot(dx, dz);
            if (dist < 0.001) continue;

            int targetX = (int) Math.round(roadPos.getX() + (dx / dist) * offsetDistance);
            int targetZ = (int) Math.round(roadPos.getZ() + (dz / dist) * offsetDistance);

            BlockPos wallSurface = PathfindingService.getCleanSurfacePos(world, targetX, targetZ, plateauY);
            if (wallSurface == null) {
                wallSurface = new BlockPos(targetX, plateauY, targetZ);
            }

            if (offsetPath.isEmpty() || !offsetPath.get(offsetPath.size() - 1).equals(wallSurface)) {
                offsetPath.add(wallSurface);
            }
        }
        return offsetPath;
    }

    private static List<BlockPos> interpolatePath(ServerLevel world, List<BlockPos> sparsePath, int plateauY) {
        List<BlockPos> densePath = new ArrayList<>();
        if (sparsePath == null || sparsePath.isEmpty()) return densePath;

        int size = sparsePath.size();
        for (int i = 0; i < size; i++) {
            BlockPos p1 = sparsePath.get(i);
            BlockPos p2 = sparsePath.get((i + 1) % size);
            if (p1 == null || p2 == null) continue;

            List<BlockPos> line = getBlockLine2D(p1, p2);
            for (BlockPos pos : line) {
                BlockPos surface = PathfindingService.getCleanSurfacePos(world, pos.getX(), pos.getZ(), plateauY);
                if (surface == null) {
                    surface = new BlockPos(pos.getX(), plateauY, pos.getZ());
                }
                if (densePath.isEmpty() || !densePath.get(densePath.size() - 1).equals(surface)) {
                    densePath.add(surface);
                }
            }
        }
        return densePath;
    }

    private static List<BlockPos> getBlockLine2D(BlockPos p1, BlockPos p2) {
        List<BlockPos> line = new ArrayList<>();
        int x1 = p1.getX(), z1 = p1.getZ(), x2 = p2.getX(), z2 = p2.getZ();
        int dx = Math.abs(x2 - x1), dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1, sz = z1 < z2 ? 1 : -1;
        int err = dx - dz, currX = x1, currZ = z1;
        int steps = 0, maxSteps = dx + dz + 2;

        while (steps++ < maxSteps) {
            line.add(new BlockPos(currX, p1.getY(), currZ));
            if (currX == x2 && currZ == z2) break;
            int e2 = 2 * err;
            if (e2 > -dz) { err -= dz; currX += sx; }
            if (e2 < dx) { err += dx; currZ += sz; }
        }
        return line;
    }

    private static Set<Long> collectRoadPositions2D(List<RoadSegment> roads) {
        Set<Long> positions = new HashSet<>();
        if (roads != null) {
            for (RoadSegment segment : roads) {
                if (segment != null && segment.getPoints() != null) {
                    for (BlockPos pos : segment.getPoints()) {
                        positions.add(pack2D(pos.getX(), pos.getZ()));
                    }
                }
            }
        }
        return positions;
    }

    private static boolean isNearRoad2D(int x, int z, Set<Long> roadXZPositions, int threshold) {
        if (roadXZPositions == null || roadXZPositions.isEmpty()) return false;
        for (int dx = -threshold; dx <= threshold; dx++) {
            for (int dz = -threshold; dz <= threshold; dz++) {
                if (roadXZPositions.contains(pack2D(x + dx, z + dz))) return true;
            }
        }
        return false;
    }

    private static long pack2D(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }
}