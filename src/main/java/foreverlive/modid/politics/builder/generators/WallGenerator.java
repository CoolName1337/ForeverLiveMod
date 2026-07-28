package foreverlive.modid.politics.builder.generators;

import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.builder.SettlementStyle;
import foreverlive.modid.politics.builder.WorldBuildQueue;
import foreverlive.modid.politics.builder.services.PathfindingService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WallGenerator {
    public static void generateWallsAndGates(ServerLevel world, Settlement settlement, List<List<BlockPos>> mainRoads, SettlementStyle style) {
        if (!style.hasWalls() || style.wallLayers() <= 0 || mainRoads.isEmpty()) return;

        BlockPos center = settlement.origin;

        // 1. Генерируем ВНЕШНИЙ контур стен (по самым крайним точкам дорог)
        buildWallLayer(world, center, mainRoads, style, -1, style.wallHeight());

        // 2. Генерируем ВНУТРЕННИЙ контур стены (используем правильный innerWallRadius!)
        if (style.hasInnerWall()) {
            // Внутренние стены делаем чуть ниже внешних (на 2 блока)
            int innerHeight = Math.max(3, style.wallHeight() - 2);
            // Строим стену строго по безопасному радиусу, согласованному с PlotScanner
            buildWallLayer(world, center, mainRoads, style, style.innerWallRadius(), innerHeight);
        }
    }
    /**
     * Постройка одного кольца стен с воротами/арками на пересечении с дорогами
     */
    private static void buildWallLayer(ServerLevel world, BlockPos center, List<List<BlockPos>> mainRoads, SettlementStyle style, int targetRadius, int wallHeight) {
        List<GateData> gates = new ArrayList<>();

        // Если targetRadius < 0, значит это внешняя стена — берем style.maxRadius()
        int actualRadius = (targetRadius < 0) ? style.maxRadius() : targetRadius;

        for (List<BlockPos> road : mainRoads) {
            if (road.isEmpty()) continue;

            // 1. Вычисляем угол дороги относительно центра
            BlockPos lastRoadPos = road.get(road.size() - 1);
            double angle = Math.atan2(lastRoadPos.getZ() - center.getZ(), lastRoadPos.getX() - center.getX());

            // 2. Ищем точку на дороге около нужного радиуса
            BlockPos rawGatePos = findPointAtRadius(road, center, actualRadius);

            // Если дорога оказалась слишком короткой и не дошла до радиуса,
            // насильно проецируем точку ворот на нужный радиус по углу!
            if (rawGatePos == null) {
                int targetX = center.getX() + (int) Math.round(Math.cos(angle) * actualRadius);
                int targetZ = center.getZ() + (int) Math.round(Math.sin(angle) * actualRadius);
                rawGatePos = PathfindingService.getSurfacePos(world, new BlockPos(targetX, center.getY(), targetZ));
            }

            // Подтягиваем позицию ворот к плато (чтобы не висели над обрывом)
            BlockPos gatePos = PathfindingService.clampToPlateau(world, center, rawGatePos);

            if (angle < 0) angle += 2 * Math.PI;

            double tx = -Math.sin(angle);
            double tz = Math.cos(angle);

            gates.add(new GateData(gatePos, angle, (int) Math.round(tx), (int) Math.round(tz)));
        }

        if (gates.size() < 2) return;

        // Сортируем ворота по часовой стрелке
        gates.sort(Comparator.comparingDouble(g -> g.angle));

        // Строим ворота и соединяем дугами
        for (GateData gate : gates) {
            buildGateStructure(world, gate.pos, gate.tangentX, gate.tangentZ, style, wallHeight);
        }

        int gateCount = gates.size();
        for (int i = 0; i < gateCount; i++) {
            GateData currentGate = gates.get(i);
            GateData nextGate = gates.get((i + 1) % gateCount);

            BlockPos startAttach = currentGate.getExitAttachment();
            BlockPos endAttach = nextGate.getEntryAttachment();

            double radius = Math.hypot(startAttach.getX() - center.getX(), startAttach.getZ() - center.getZ());

            List<BlockPos> arcWaypoints = PathfindingService.calculateArcWaypoints(
                    world, center, currentGate.angle, nextGate.angle, radius, 3
            );

            BlockPos currentPoint = startAttach;
            for (BlockPos nextWaypoint : arcWaypoints) {
                BlockPos targetPoint = nextWaypoint.equals(arcWaypoints.get(arcWaypoints.size() - 1)) ? endAttach : nextWaypoint;

                List<BlockPos> wallSegment = PathfindingService.findWallSegmentPath(world, currentPoint, targetPoint);

                for (BlockPos pos : wallSegment) {
                    buildWallColumn(world, pos, style, wallHeight);
                }
                if (!wallSegment.isEmpty()) {
                    currentPoint = wallSegment.get(wallSegment.size() - 1);
                }
            }
        }
    }

    private static void buildGateStructure(ServerLevel world, BlockPos center, int tx, int tz, SettlementStyle style, int wallHeight) {
        int height = wallHeight + 2;
        int halfWidth = Math.max(2, style.roadWidth() / 2 + 1); // Адаптируем ширину ворот под ширину дороги!

        // Башни по бокам ворот
        for (int h = 0; h <= height; h++) {
            WorldBuildQueue.enqueue(center.offset(tx * halfWidth, h, tz * halfWidth), style.gateBlock());
            WorldBuildQueue.enqueue(center.offset(-tx * halfWidth, h, -tz * halfWidth), style.gateBlock());
        }

        // Арка над проездом
        for (int w = -halfWidth; w <= halfWidth; w++) {
            WorldBuildQueue.enqueue(center.offset(tx * w, height, tz * w), style.gateBlock());
        }
    }

    private static void buildWallColumn(ServerLevel world, BlockPos surfacePos, SettlementStyle style, int height) {
        // Заполняем столб стены вверх
        for (int h = 0; h <= height; h++) {
            WorldBuildQueue.enqueue(surfacePos.above(h), style.wallBlock());
        }
        // Ставим крышу/плиту
        if (style.wallCapBlock() != null) {
            WorldBuildQueue.enqueue(surfacePos.above(height + 1), style.wallCapBlock());
        }
    }
    private static BlockPos findPointAtRadius(List<BlockPos> road, BlockPos center, int radius) {
        for (BlockPos pos : road) {
            // Считаем строго 2D дистанцию, игнорируя высоту (Y)
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            double dist = Math.hypot(dx, dz);

            if (Math.abs(dist - radius) <= 3) {
                return pos;
            }
        }
        return null;
    }

    private record GateData(BlockPos pos, double angle, int tangentX, int tangentZ) {
        public BlockPos getEntryAttachment() {
            return pos.offset(-tangentX * 2, 0, -tangentZ * 2);
        }
        public BlockPos getExitAttachment() {
            return pos.offset(tangentX * 2, 0, tangentZ * 2);
        }
    }
}