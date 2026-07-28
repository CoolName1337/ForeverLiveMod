package foreverlive.modid.politics.builder.generators;

import foreverlive.modid.politics.POJO.RoadSegment;
import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.builder.SettlementStyle;
import foreverlive.modid.politics.builder.WorldBuildQueue;
import foreverlive.modid.politics.builder.services.PathfindingService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class RoadGenerator {

    public record RoadResult(List<List<BlockPos>> spokes, List<List<BlockPos>> allRoads) {}

    public static RoadResult generateRoadNetwork(ServerLevel world, Settlement settlement, SettlementStyle style) {
        List<List<BlockPos>> spokes = new ArrayList<>();
        BlockPos center = settlement.origin;

        // 1. Радиальные лучи (СТРОГО одинаковой длины для ровных стен)
        double angleStep = (2 * Math.PI) / style.spokeCount();
        for (int i = 0; i < style.spokeCount(); i++) {
            double angle = i * angleStep;
            // Убираем Math.random() из радиуса, чтобы периметр стены был идеальным
            List<BlockPos> path = generateOrganicSpokePath(world, center, angle, style.maxRadius());
            if (!path.isEmpty()) {
                spokes.add(path);
            }
        }

        // 2. Кольцевые проспекты (ровно 2 кольца: 35% и 70% радиуса для нормальной глубины участков)
        List<List<BlockPos>> ringRoads = new ArrayList<>();
        int[] ringRadii = new int[]{ (int)(style.maxRadius() * 0.35), (int)(style.maxRadius() * 0.70) };

        for (int radius : ringRadii) {
            generateRingRoadSegments(world, settlement, spokes, radius, ringRoads);
        }

        // 3. Объединяем и строим
        List<List<BlockPos>> allRoads = new ArrayList<>();
        allRoads.addAll(spokes);
        allRoads.addAll(ringRoads);

        for (List<BlockPos> road : allRoads) {
            for (BlockPos p : road) {
                placeWideRoadBlock(world, p, style.roadWidth(), style.mainRoadBlock());
            }
            settlement.getRoads().add(new RoadSegment(road));
        }

        return new RoadResult(spokes, allRoads);
    }
    private static List<BlockPos> generateOrganicSpokePath(ServerLevel world, BlockPos center, double angle, double maxRadius) {
        // 1. Рассчитываем идеальную конечную точку на максимальном радиусе
        int targetX = center.getX() + (int) Math.round(Math.cos(angle) * maxRadius);
        int targetZ = center.getZ() + (int) Math.round(Math.sin(angle) * maxRadius);
        BlockPos targetPos = new BlockPos(targetX, center.getY(), targetZ);

        // 2. Отдаем работу твоему A*.
        // 2 блока - это максимальный подъем (maxClimb), который преодолеет дорога.
        List<BlockPos> path = PathfindingService.findReachableOrganicPath(world, center, targetPos, 2);

        // Если путь не найден (что маловероятно из-за bestNode), возвращаем пустой список
        return path != null ? path : new ArrayList<>();
    }
    private static void placeWideRoadBlock(ServerLevel world, BlockPos center, int width, BlockState blockState) {
        int radius = width / 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos p = PathfindingService.getSurfacePos(world, center.offset(x, 0, z));
                WorldBuildQueue.enqueue(p, blockState);
            }
        }
    }
    private static void generateRingRoadSegments(ServerLevel world, Settlement settlement, List<List<BlockPos>> spokes, int targetRadius, List<List<BlockPos>> outRings) {
        if (spokes.isEmpty()) return;
        int spokeCount = spokes.size();
        BlockPos center = settlement.origin;
        double angleStep = (2 * Math.PI) / spokeCount;

        for (int i = 0; i < spokeCount; i++) {
            List<BlockPos> currentSpoke = spokes.get(i);
            List<BlockPos> nextSpoke = spokes.get((i + 1) % spokeCount);

            // Ищем точку на текущем луче
            BlockPos p1 = findPointAtRadius2D(currentSpoke, center, targetRadius);
            if (p1 == null) {
                // Если луч оборвался раньше, проецируем точку математически
                double angle1 = i * angleStep;
                p1 = PathfindingService.getSurfacePos(world, center.offset((int) Math.round(Math.cos(angle1) * targetRadius), 0, (int) Math.round(Math.sin(angle1) * targetRadius)));
            }

            // Ищем точку на следующем луче
            BlockPos p2 = findPointAtRadius2D(nextSpoke, center, targetRadius);
            if (p2 == null) {
                // Аналогично для следующего луча
                double angle2 = (i + 1) * angleStep;
                p2 = PathfindingService.getSurfacePos(world, center.offset((int) Math.round(Math.cos(angle2) * targetRadius), 0, (int) Math.round(Math.sin(angle2) * targetRadius)));
            }

            // Строим сегмент кольца с помощью A*
            List<BlockPos> ringSegment = PathfindingService.findWallSegmentPath(world, p1, p2);
            if (!ringSegment.isEmpty()) {
                outRings.add(ringSegment);
            }
        }
    }

    private static BlockPos findPointAtRadius2D(List<BlockPos> road, BlockPos center, int radius) {
        for (BlockPos pos : road) {
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            if (Math.hypot(dx, dz) >= radius) {
                return pos;
            }
        }
        // Если дорога оборвалась до достижения радиуса, возвращаем null (а не конец дороги!)
        return null;
    }
}