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
import java.util.Random;
import java.util.Set;

public class RoadGenerator {

    public record RoadResult(
            List<List<BlockPos>> spokes,
            List<List<BlockPos>> ringRoads,
            List<List<BlockPos>> alleys,
            List<List<BlockPos>> allRoads
    ) {}

    // Допустимый перепад от уровня плато (6 блока = края плато и обрывы)
    private static final int MAX_PLATEAU_DELTA = 6;

    public static RoadResult generateRoadNetwork(ServerLevel world, Settlement settlement, SettlementStyle style, int plateauY) {
        if (settlement == null || settlement.origin == null) {
            System.err.println("[RoadGenerator] Ошибка: settlement или settlement.origin равен null!");
            return new RoadResult(List.of(), List.of(), List.of(), List.of());
        }

        int seed = (settlement.getName() != null) ? settlement.getName().hashCode() : 42;
        Random random = new Random(seed);

        BlockPos center = new BlockPos(settlement.origin.getX(), plateauY, settlement.origin.getZ());

        List<List<BlockPos>> spokes = new ArrayList<>();
        List<List<BlockPos>> ringRoads = new ArrayList<>();
        List<List<BlockPos>> alleys = new ArrayList<>();

        int numSpokes = Math.max(4, style.spokeCount());
        double angleStep = (2 * Math.PI) / numSpokes;
        int maxRadius = style.maxRadius();

        // 1. РАДИАЛЬНЫЕ ДОРОГИ (Spokes)
        for (int i = 0; i < numSpokes; i++) {
            double baseAngle = i * angleStep + (random.nextDouble() - 0.5) * 0.15;

            List<BlockPos> spokeWaypoints = generateCurvedWaypoints(world, center, baseAngle, maxRadius, plateauY, random);
            List<BlockPos> fullSpokePath = buildPathThroughWaypoints(world, spokeWaypoints, plateauY);

            if (!fullSpokePath.isEmpty()) {
                spokes.add(fullSpokePath);
            }
        }

        // 2. КОЛЬЦЕВЫЕ ДОРОГИ (Ring Roads)
        int[] radii = style.ringRadii();
        if (radii != null && radii.length > 0 && !spokes.isEmpty()) {
            for (int radius : radii) {
                if (radius >= maxRadius) continue;

                List<BlockPos> fullRing = new ArrayList<>();

                for (int i = 0; i < spokes.size(); i++) {
                    List<BlockPos> currentSpoke = spokes.get(i);
                    List<BlockPos> nextSpoke = spokes.get((i + 1) % spokes.size());

                    BlockPos p1 = findPointAtRadius(currentSpoke, center, radius);
                    BlockPos p2 = findPointAtRadius(nextSpoke, center, radius);

                    if (p1 != null && p2 != null) {
                        List<BlockPos> arc = generateRingArc(world, center, p1, p2, radius, plateauY, random);
                        if (!arc.isEmpty()) {
                            if (!fullRing.isEmpty() && fullRing.get(fullRing.size() - 1).equals(arc.get(0))) {
                                arc.remove(0);
                            }
                            fullRing.addAll(arc);
                        }
                    }
                }

                if (!fullRing.isEmpty()) {
                    if (!fullRing.get(fullRing.size() - 1).equals(fullRing.get(0))) {
                        fullRing.add(fullRing.get(0));
                    }
                    ringRoads.add(fullRing);
                }
            }
        }

        // 3. ПЕРЕУЛКИ (Alleys)
        if (radii != null && radii.length > 0 && !spokes.isEmpty()) {
            int innerR = radii[0];
            int targetOuterR = (int) (maxRadius * 0.85);

            for (int i = 0; i < numSpokes; i++) {
                double midAngle = (i + 0.5) * angleStep;

                int startX = (int) (center.getX() + Math.cos(midAngle) * innerR);
                int startZ = (int) (center.getZ() + Math.sin(midAngle) * innerR);

                if (!PathfindingService.isTerrainSuitable(world, startX, startZ, plateauY, MAX_PLATEAU_DELTA)) {
                    continue;
                }

                int validOuterR = innerR;
                for (int r = innerR + 4; r <= targetOuterR; r += 4) {
                    int cx = (int) (center.getX() + Math.cos(midAngle) * r);
                    int cz = (int) (center.getZ() + Math.sin(midAngle) * r);

                    if (!PathfindingService.isTerrainSuitable(world, cx, cz, plateauY, MAX_PLATEAU_DELTA)) {
                        break;
                    }
                    validOuterR = r;
                }

                if (validOuterR - innerR < 10) {
                    continue;
                }

                BlockPos alleyStart = PathfindingService.getCleanSurfacePos(world, startX, startZ, plateauY);

                int endX = (int) (center.getX() + Math.cos(midAngle) * validOuterR);
                int endZ = (int) (center.getZ() + Math.sin(midAngle) * validOuterR);
                BlockPos alleyEnd = PathfindingService.getCleanSurfacePos(world, endX, endZ, plateauY);

                if (alleyStart != null && alleyEnd != null) {
                    List<BlockPos> alleyWaypoints = generateCurvedWaypointsBetween(alleyStart, alleyEnd, random);
                    List<BlockPos> alleyPath = buildPathThroughWaypoints(world, alleyWaypoints, plateauY);

                    if (!alleyPath.isEmpty()) {
                        alleys.add(alleyPath);
                    }
                }
            }
        }

        // 4. ОПТИМИЗИРОВАННАЯ ОТРИСОВКА
        List<List<BlockPos>> allRoads = new ArrayList<>();
        allRoads.addAll(spokes);
        allRoads.addAll(ringRoads);
        allRoads.addAll(alleys);

        int totalBlocksPlaced = 0;

        // Spokes (Радиальные)
        for (List<BlockPos> spoke : spokes) {
            int width = style.roadWidth();
            totalBlocksPlaced += renderRoadFast(world, spoke, width, style.mainRoadBlock(), plateauY);
            settlement.getRoads().add(new RoadSegment(spoke, width, RoadSegment.RoadType.SPOKE));
        }

        // Ring Roads (Кольцевые)
        for (List<BlockPos> ring : ringRoads) {
            int width = Math.max(2, style.roadWidth() - 1);
            totalBlocksPlaced += renderRoadFast(world, ring, width, style.mainRoadBlock(), plateauY);
            settlement.getRoads().add(new RoadSegment(ring, width, RoadSegment.RoadType.RING));
        }

        // Alleys (Переулки)
        for (List<BlockPos> alley : alleys) {
            int width = 2;
            totalBlocksPlaced += renderRoadFast(world, alley, width, style.mainRoadBlock(), plateauY);
            settlement.getRoads().add(new RoadSegment(alley, width, RoadSegment.RoadType.ALLEY));
        }
        System.out.println("[RoadGenerator] Генерация завершена! Сегментов: " + allRoads.size() + ", Блоков: " + totalBlocksPlaced);

        return new RoadResult(spokes, ringRoads, alleys, allRoads);
    }

    private static List<BlockPos> generateCurvedWaypoints(ServerLevel world, BlockPos center, double angle, int maxRadius, int plateauY, Random random) {
        List<BlockPos> waypoints = new ArrayList<>();
        waypoints.add(center);

        int stepSize = 4;
        double perpAngle = angle + Math.PI / 2;
        int validRadius = maxRadius;

        for (int dist = stepSize; dist <= maxRadius; dist += stepSize) {
            double progress = (double) dist / maxRadius;
            double curveOffset = Math.sin(progress * Math.PI) * (6.0 + random.nextDouble() * 4.0) * (random.nextBoolean() ? 1 : -1);

            int checkX = (int) Math.round(center.getX() + Math.cos(angle) * dist + Math.cos(perpAngle) * curveOffset);
            int checkZ = (int) Math.round(center.getZ() + Math.sin(angle) * dist + Math.sin(perpAngle) * curveOffset);

            if (!PathfindingService.isTerrainSuitable(world, checkX, checkZ, plateauY, MAX_PLATEAU_DELTA)) {
                validRadius = Math.max(stepSize, dist - stepSize);
                break;
            }
        }

        int segmentCount = Math.max(2, validRadius / 10);
        for (int i = 1; i <= segmentCount; i++) {
            double progress = (double) i / segmentCount;
            double dist = validRadius * progress;

            double curveOffset = Math.sin(progress * Math.PI) * (6.0 + random.nextDouble() * 4.0) * (random.nextBoolean() ? 1 : -1);

            int x = (int) Math.round(center.getX() + Math.cos(angle) * dist + Math.cos(perpAngle) * curveOffset);
            int z = (int) Math.round(center.getZ() + Math.sin(angle) * dist + Math.sin(perpAngle) * curveOffset);

            waypoints.add(new BlockPos(x, center.getY(), z));
        }

        return waypoints;
    }

    private static List<BlockPos> generateCurvedWaypointsBetween(BlockPos start, BlockPos end, Random random) {
        List<BlockPos> waypoints = new ArrayList<>();
        waypoints.add(start);

        double dx = end.getX() - start.getX();
        double dz = end.getZ() - start.getZ();
        double dist = Math.hypot(dx, dz);

        if (dist > 0.001) {
            double nx = -dz / dist;
            double nz = dx / dist;

            double offset = (4.0 + random.nextDouble() * 4.0) * (random.nextBoolean() ? 1 : -1);

            int midX = (int) (start.getX() + dx * 0.5 + nx * offset);
            int midZ = (int) (start.getZ() + dz * 0.5 + nz * offset);

            waypoints.add(new BlockPos(midX, start.getY(), midZ));
        }

        waypoints.add(end);
        return waypoints;
    }

    private static List<BlockPos> buildPathThroughWaypoints(ServerLevel world, List<BlockPos> waypoints, int plateauY) {
        List<BlockPos> fullPath = new ArrayList<>();
        for (int i = 0; i < waypoints.size() - 1; i++) {
            List<BlockPos> segment = PathfindingService.traceSurfacePath(world, waypoints.get(i), waypoints.get(i + 1), plateauY);
            if (segment.isEmpty()) continue;

            if (!fullPath.isEmpty()) {
                segment.remove(0);
            }
            fullPath.addAll(segment);
        }
        return fullPath;
    }

    private static List<BlockPos> generateRingArc(ServerLevel world, BlockPos center, BlockPos p1, BlockPos p2, int radius, int plateauY, Random random) {
        List<BlockPos> arc = new ArrayList<>();

        double a1 = Math.atan2(p1.getZ() - center.getZ(), p1.getX() - center.getX());
        double a2 = Math.atan2(p2.getZ() - center.getZ(), p2.getX() - center.getX());

        double diff = a2 - a1;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;

        int steps = Math.max(6, (int) Math.round(Math.abs(radius * diff) / 2.5));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double curAngle = a1 + diff * t;

            double rNoise = (Math.sin(t * Math.PI) * 1.5) * (random.nextDouble() - 0.5);
            double targetR = radius + rNoise;

            int attempts = 0;
            while (targetR > 10 && attempts < 5) {
                int x = (int) Math.round(center.getX() + Math.cos(curAngle) * targetR);
                int z = (int) Math.round(center.getZ() + Math.sin(curAngle) * targetR);

                if (PathfindingService.isTerrainSuitable(world, x, z, plateauY, MAX_PLATEAU_DELTA)) {
                    BlockPos pos = PathfindingService.getCleanSurfacePos(world, x, z, plateauY);
                    if (pos != null && (arc.isEmpty() || !arc.get(arc.size() - 1).equals(pos))) {
                        arc.add(pos);
                    }
                    break;
                }

                targetR -= 2.0;
                attempts++;
            }
        }

        return arc;
    }

    private static BlockPos findPointAtRadius(List<BlockPos> path, BlockPos center, int radius) {
        if (path == null || path.isEmpty()) return null;

        BlockPos best = null;
        double minDiff = Double.MAX_VALUE;

        for (BlockPos pos : path) {
            double dist = Math.hypot(pos.getX() - center.getX(), pos.getZ() - center.getZ());
            double diff = Math.abs(dist - radius);
            if (diff < minDiff) {
                minDiff = diff;
                best = pos;
            }
        }

        return (minDiff <= 15.0) ? best : null;
    }

    private static int renderRoadFast(ServerLevel world, List<BlockPos> path, int width, BlockState roadBlock, int plateauY) {
        if (path == null || path.isEmpty()) return 0;

        int radius = width / 2;
        Set<Long> uniqueXZ = new HashSet<>(path.size() * (radius * 2 + 1) * (radius * 2 + 1));

        for (BlockPos pos : path) {
            if (pos == null) continue;
            for (int rx = -radius; rx <= radius; rx++) {
                for (int rz = -radius; rz <= radius; rz++) {
                    uniqueXZ.add(pack2D(pos.getX() + rx, pos.getZ() + rz));
                }
            }
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        int count = 0;

        for (long packed : uniqueXZ) {
            int x = unpackX(packed);
            int z = unpackZ(packed);

            BlockPos surface = PathfindingService.getCleanSurfacePos(world, x, z, plateauY);
            if (surface == null) continue;

            // Если рельеф упал ниже допустимого плато, подтягиваем высоту дороги
            int roadY = Math.max(surface.getY(), plateauY - MAX_PLATEAU_DELTA);
            BlockPos roadPos = new BlockPos(x, roadY, z);

            // 1. Убираем мелкие ямы прямо под покрытием дороги (до 3 блоков вниз)
            for (int depth = 1; depth <= 3; depth++) {
                BlockPos under = roadPos.below(depth);
                BlockState state = world.getBlockState(under);
                if (state.isAir() || !state.isSolidRender()) {
                    WorldBuildQueue.enqueue(under, dirt);
                } else {
                    break;
                }
            }

            // 2. Ставим покрытие дороги
            WorldBuildQueue.enqueue(roadPos, roadBlock);

            // 3. Расчищаем проход по высоте (3 блока воздуха), чтобы мобы/игрок свободно проходили
            WorldBuildQueue.enqueue(roadPos.above(1), air);
            WorldBuildQueue.enqueue(roadPos.above(2), air);
            WorldBuildQueue.enqueue(roadPos.above(3), air);

            count++;
        }
        return count;
    }

    private static long pack2D(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static int unpackX(long packed) {
        return (int) packed;
    }

    private static int unpackZ(long packed) {
        return (int) (packed >>> 32);
    }
}