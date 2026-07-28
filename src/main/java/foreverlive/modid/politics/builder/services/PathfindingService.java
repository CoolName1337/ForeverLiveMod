package foreverlive.modid.politics.builder.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;
public class PathfindingService {

    // Максимально допустимый перепад высоты от центра города
    private static final int MAX_PLATEAU_DROP = 6;
    public static List<BlockPos> findReachableOrganicPath(ServerLevel world, BlockPos start, BlockPos target, int maxClimb) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<BlockPos, Double> gScores = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        // Получаем РЕАЛЬНЫЕ позиции на поверхности земли
        BlockPos realStart = getSurfacePos(world, start);
        BlockPos realTarget = getSurfacePos(world, target);

        Node bestNode = new Node(realStart, 0, heuristic(realStart, realTarget), null);
        openSet.add(bestNode);
        gScores.put(realStart, 0.0);

        int maxIterations = 3000;

        // ВАЖНО: Добавляем 8 направлений (прямые + диагонали)
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}, // Прямые
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // Диагонали
        };

        while (!openSet.isEmpty() && maxIterations-- > 0) {
            Node current = openSet.poll();

            if (current.h < bestNode.h) bestNode = current;

            // ИСПРАВЛЕНО: Для 8-стороннего движения используем обычную дистанцию, а не Манхэттенскую
            if (Math.hypot(current.pos.getX() - realTarget.getX(), current.pos.getZ() - realTarget.getZ()) <= 2) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            // Перебираем все 8 направлений
            for (int[] dir : directions) {
                BlockPos neighborSurface = getSurfacePos(world, current.pos.offset(dir[0], 0, dir[1]));
                if (closedSet.contains(neighborSurface)) continue;

                if (Math.abs(neighborSurface.getY() - realStart.getY()) > MAX_PLATEAU_DROP) continue;

                int deltaY = Math.abs(neighborSurface.getY() - current.pos.getY());
                if (deltaY > maxClimb) continue;

                // ИСПРАВЛЕНО: Диагональный шаг геометрически длиннее (корень из 2 = 1.414).
                // Это важно, чтобы A* правильно считал кратчайший путь.
                double baseCost = (dir[0] != 0 && dir[1] != 0) ? 1.414 : 1.0;
                double stepCost = baseCost + (deltaY * deltaY * 4.0);

                double tentativeG = current.g + stepCost;

                if (tentativeG < gScores.getOrDefault(neighborSurface, Double.MAX_VALUE)) {
                    gScores.put(neighborSurface, tentativeG);
                    openSet.add(new Node(neighborSurface, tentativeG, heuristic(neighborSurface, realTarget), current));
                }
            }
        }
        return reconstructPath(bestNode);
    }
    public static List<BlockPos> findWallSegmentPath(ServerLevel world, BlockPos start, BlockPos target) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<BlockPos, Double> gScores = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        BlockPos realStart = getSurfacePos(world, start);
        BlockPos realTarget = getSurfacePos(world, target);

        openSet.add(new Node(realStart, 0, heuristic(realStart, realTarget), null));
        gScores.put(realStart, 0.0);

        int maxIterations = 1500;

        while (!openSet.isEmpty() && maxIterations-- > 0) {
            Node current = openSet.poll();

            if (current.pos.distManhattan(realTarget) <= 1) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighborSurface = getSurfacePos(world, current.pos.relative(dir));
                if (closedSet.contains(neighborSurface)) continue;

                // ИСПРАВЛЕНО: сравниваем с realStart.getY()
                if (Math.abs(neighborSurface.getY() - realStart.getY()) > MAX_PLATEAU_DROP) continue;

                int deltaY = Math.abs(neighborSurface.getY() - current.pos.getY());
                if (deltaY > 2) continue;

                double stepCost = 1.0 + deltaY * 3.0;

                double tentativeG = current.g + stepCost;

                if (tentativeG < gScores.getOrDefault(neighborSurface, Double.MAX_VALUE)) {
                    gScores.put(neighborSurface, tentativeG);
                    openSet.add(new Node(neighborSurface, tentativeG, heuristic(neighborSurface, realTarget), current));
                }
            }
        }

        return generateContinuousSurfaceLine(world, realStart, realTarget);
    }

    public static List<BlockPos> calculateArcWaypoints(ServerLevel world, BlockPos center, double angleA, double angleB, double radius, int segments) {
        List<BlockPos> waypoints = new ArrayList<>();

        double diff = angleB - angleA;
        while (diff < 0) diff += 2 * Math.PI;

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double currentAngle = angleA + diff * t;

            int x = center.getX() + (int) Math.round(Math.cos(currentAngle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(currentAngle) * radius);

            BlockPos rawWaypoint = getSurfacePos(world, new BlockPos(x, center.getY(), z));

            BlockPos clampedWaypoint = clampToPlateau(world, center, rawWaypoint);
            waypoints.add(clampedWaypoint);
        }
        return waypoints;
    }

    public static BlockPos clampToPlateau(ServerLevel world, BlockPos center, BlockPos target) {
        BlockPos current = target;
        // ИСПРАВЛЕНО: берем точную высоту поверхности для центра города
        int centerY = getSurfacePos(world, center).getY();

        if (Math.abs(current.getY() - centerY) > MAX_PLATEAU_DROP) {
            double dx = center.getX() - target.getX();
            double dz = center.getZ() - target.getZ();
            double distance = Math.hypot(dx, dz);

            for (double d = 0; d < distance; d += 2.0) {
                double ratio = d / distance;
                int checkX = (int) Math.round(target.getX() + dx * ratio);
                int checkZ = (int) Math.round(target.getZ() + dz * ratio);

                BlockPos checkPos = getSurfacePos(world, new BlockPos(checkX, centerY, checkZ));

                if (Math.abs(checkPos.getY() - centerY) <= MAX_PLATEAU_DROP) {
                    return checkPos;
                }
            }
        }
        return current;
    }

    private static List<BlockPos> generateContinuousSurfaceLine(ServerLevel world, BlockPos start, BlockPos target) {
        List<BlockPos> path = new ArrayList<>();
        int dx = Math.abs(target.getX() - start.getX());
        int dz = Math.abs(target.getZ() - start.getZ());
        int steps = Math.max(dx, dz);

        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 1.0 : (double) i / steps;
            int x = (int) Math.round(start.getX() + t * (target.getX() - start.getX()));
            int z = (int) Math.round(start.getZ() + t * (target.getZ() - start.getZ()));

            path.add(getSurfacePos(world, new BlockPos(x, start.getY(), z)));
        }
        return path;
    }

    public static BlockPos getSurfacePos(ServerLevel world, BlockPos pos) {
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    private static double heuristic(BlockPos a, BlockPos b) { return Math.sqrt(a.distSqr(b)); }

    private static List<BlockPos> reconstructPath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) { path.add(curr.pos); curr = curr.parent; }
        Collections.reverse(path);
        return path;
    }

    private record Node(BlockPos pos, double g, double h, Node parent) implements Comparable<Node> {
        public double f() { return g + h; }
        @Override public int compareTo(Node o) { return Double.compare(this.f(), o.f()); }
    }
}