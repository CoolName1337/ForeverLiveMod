package foreverlive.modid.politics.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public class PathfindingService {

    private static final int MAX_DELTA_Y = 10; // Максимальный перепад высоты от плато

    /**
     * Поиск пути по рельефу с помощью алгоритма A* (обходит ямы, воду и обрывы).
     */
    public static List<BlockPos> traceSurfacePath(ServerLevel world, BlockPos start, BlockPos end, int plateauY) {
        List<BlockPos> path = findAStarPath(world, start, end, plateauY);
        if (!path.isEmpty()) {
            return path;
        }
        // Резервный вариант, если A* не нашел путь из-за жестких ограничений
        return traceDirectPath(world, start, end, plateauY);
    }

    private static List<BlockPos> findAStarPath(ServerLevel world, BlockPos start, BlockPos end, int plateauY) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<Long, Node> allNodes = new HashMap<>();

        BlockPos startSurface = getCleanSurfacePos(world, start.getX(), start.getZ(), plateauY);
        BlockPos endSurface = getCleanSurfacePos(world, end.getX(), end.getZ(), plateauY);

        Node startNode = new Node(startSurface, 0, distance(startSurface, endSurface), null);
        openSet.add(startNode);
        allNodes.put(pack2D(startSurface.getX(), startSurface.getZ()), startNode);

        int maxIterations = 1000; // Предотвращаем зависание сервера
        int iterations = 0;

        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dz = {0, 0, 1, -1, 1, -1, 1, -1};

        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            Node current = openSet.poll();

            if (Math.abs(current.pos.getX() - endSurface.getX()) <= 1 && Math.abs(current.pos.getZ() - endSurface.getZ()) <= 1) {
                return reconstructPath(current);
            }

            current.closed = true;

            for (int i = 0; i < 8; i++) {
                int nx = current.pos.getX() + dx[i];
                int nz = current.pos.getZ() + dz[i];

                if (!isTerrainSuitable(world, nx, nz, plateauY, MAX_DELTA_Y)) {
                    continue; // Пропускаем ямы, воду и обрывы
                }

                BlockPos neighborPos = getCleanSurfacePos(world, nx, nz, plateauY);
                long neighborKey = pack2D(nx, nz);

                Node neighbor = allNodes.get(neighborKey);
                if (neighbor != null && neighbor.closed) continue;

                // Стоимость движения (диагональ чуть дороже)
                double moveCost = (i < 4) ? 1.0 : 1.414;

                // ШТРАФ ЗА ПЕРЕПАД ВЫСОТЫ: Алгоритм будет активно обходить ямы и холмы
                int heightDelta = Math.abs(neighborPos.getY() - current.pos.getY());
                double heightPenalty = heightDelta * 6.0;

                double newGCost = current.gCost + moveCost + heightPenalty;

                if (neighbor == null) {
                    neighbor = new Node(neighborPos, newGCost, distance(neighborPos, endSurface), current);
                    allNodes.put(neighborKey, neighbor);
                    openSet.add(neighbor);
                } else if (newGCost < neighbor.gCost) {
                    neighbor.gCost = newGCost;
                    neighbor.fCost = newGCost + neighbor.hCost;
                    neighbor.parent = current;
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return List.of();
    }

    private static List<BlockPos> reconstructPath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static List<BlockPos> traceDirectPath(ServerLevel world, BlockPos start, BlockPos end, int plateauY) {
        List<BlockPos> path = new ArrayList<>();
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        double distance = Math.hypot(dx, dz);
        int steps = Math.max(1, (int) Math.ceil(distance));

        BlockPos lastAdded = null;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(start.getX() + dx * t);
            int z = (int) Math.round(start.getZ() + dz * t);

            BlockPos surfacePos = getCleanSurfacePos(world, x, z, plateauY);
            if (lastAdded == null || !lastAdded.equals(surfacePos)) {
                path.add(surfacePos);
                lastAdded = surfacePos;
            }
        }
        return path;
    }
    public static BlockPos getCleanSurfacePos(ServerLevel world, int x, int z, int defaultY) {
        // Берем верхнюю точку карты
        int rawY = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

        if (!world.isInsideBuildHeight(rawY)) {
            return new BlockPos(x, defaultY, z);
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, rawY, z);

        while (world.isInsideBuildHeight(pos.getY())) {
            BlockState state = world.getBlockState(pos);

            // Спускаемся ниже, если это воздух, трава, цветы, снег или нетвердый блок
            if (isFoliageOrAir(state)) {
                pos.move(0, -1, 0);
                continue;
            }

            // Нашли настоящий твердый блок грунта (дерн, земля, камень)
            return pos.immutable();
        }

        return new BlockPos(x, defaultY, z);
    }

    /**
     * Проверяет, является ли блок растительностью, воздухом или заменяемым декоративным блоком.
     */
    private static boolean isFoliageOrAir(BlockState state) {
        if (state.isAir()) return true;
        if (state.canBeReplaced()) return true; // Покрывает обычную траву, цветы, лианы, снежный покров
        if (state.is(net.minecraft.tags.BlockTags.FLOWERS)) return true;
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) return true;
        if (state.is(net.minecraft.tags.BlockTags.REPLACEABLE)) return true;

        // Финальная проверка: если блок не коллизионный (через него можно пройти), это не грунт
        return !state.blocksMotion();
    }

    public static boolean isTerrainSuitable(ServerLevel world, int x, int z, int plateauY, int maxAllowedDeltaY) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.hasChunk(chunkX, chunkZ)) {
            world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
        }

        BlockPos surface = getCleanSurfacePos(world, x, z, plateauY);

        var state = world.getBlockState(surface);
        var stateBelow = world.getBlockState(surface.below());

        // 1. Проверка на воду
        if (state.is(net.minecraft.world.level.block.Blocks.WATER) ||
                stateBelow.is(net.minecraft.world.level.block.Blocks.WATER) ||
                !world.getFluidState(surface).isEmpty()) {
            return false;
        }

        // 2. Проверка на крутой перепад высот от плато
        int heightDelta = Math.abs(surface.getY() - plateauY);
        return heightDelta <= maxAllowedDeltaY;
    }
    public static int calculateSmartPlateauY(ServerLevel world, BlockPos center, int radius) {
        List<Integer> heights = new ArrayList<>();

        // Сканируем высоту сеткой с шагом в 4-5 блоков для экономии ресурсов
        int step = 4;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                if (Math.hypot(dx, dz) <= radius) {
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    // Берем высоту поверхности
                    int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                    heights.add(y);
                }
            }
        }

        if (heights.isEmpty()) return center.getY();

        // Сортируем все найденные высоты
        Collections.sort(heights);

        // Берем 35-й перцентиль (чуть ниже середины), чтобы город лучше "врастал" в низину,
        // а не задирался на верхушки холмов
        int percentileIndex = (int) (heights.size() * 0.35);
        return heights.get(percentileIndex);
    }
    private static double distance(BlockPos a, BlockPos b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    private static long pack2D(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static class Node {
        BlockPos pos;
        double gCost;
        double hCost;
        double fCost;
        boolean closed = false;
        Node parent;

        Node(BlockPos pos, double gCost, double hCost, Node parent) {
            this.pos = pos;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
            this.parent = parent;
        }
    }
}