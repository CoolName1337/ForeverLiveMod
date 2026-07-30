package foreverlive.modid.politics.generators;

import foreverlive.modid.politics.POJO.RoadSegment;
import foreverlive.modid.politics.POJO.enums.ZoneType;
import foreverlive.modid.politics.services.PathfindingService;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.settlement.enums.PlotType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlotScanner {

    public static List<BuildingPlot> scanAndAllocatePlots(ServerLevel world, Settlement settlement, SettlementStyle style) {
        List<BuildingPlot> plots = new ArrayList<>();
        BlockPos center = settlement.origin;
        List<RoadSegment> segments = settlement.getRoads();

        Set<Long> forbiddenPositions = settlement.getAllForbiddenPositions();

        int step = Math.max(3, style.plotScanStep());

        for (RoadSegment segment : segments) {
            List<BlockPos> road = segment.getPoints();
            if (road.size() < step * 2) continue;

            for (int i = step; i < road.size() - step; i += step) {
                BlockPos current = road.get(i);
                BlockPos prev = road.get(i - step);
                BlockPos next = road.get(i + step);

                double dx = next.getX() - prev.getX();
                double dz = next.getZ() - prev.getZ();
                double len = Math.hypot(dx, dz);
                if (len == 0) continue;

                dx /= len;
                dz /= len;

                double leftNx = -dz;
                double leftNz = dx;
                double rightNx = dz;
                double rightNz = -dx;

                double distToCenter = Math.hypot(current.getX() - center.getX(), current.getZ() - center.getZ());
                ZoneType zoneType = calculateZoneType(distToCenter, style.maxRadius());

                BuildingPlot leftPlot = tryCreatePlot(world, current, leftNx, leftNz, dx, dz, style, zoneType, plots, settlement, segment.getWidth(), forbiddenPositions);
                if (leftPlot != null) plots.add(leftPlot);

                BuildingPlot rightPlot = tryCreatePlot(world, current, rightNx, rightNz, dx, dz, style, zoneType, plots, settlement, segment.getWidth(), forbiddenPositions);
                if (rightPlot != null) plots.add(rightPlot);
            }
        }

        return plots;
    }

    private static BuildingPlot tryCreatePlot(ServerLevel world, BlockPos roadPos, double nx, double nz, double tx, double tz,
                                              SettlementStyle style, ZoneType zoneType, List<BuildingPlot> existingPlots,
                                              Settlement settlement, int actualRoadWidth, Set<Long> forbiddenPositions) {

        // 1. Определяем необходимый тип постройки для города
        PlotType resolvedType = PlotTypeResolver.resolveType(settlement, zoneType);

        int roadRadius = actualRoadWidth / 2;
        int roadOffset = roadRadius + style.plotRoadMargin() + 2;

        int baseWidth = zoneType.getDefaultWidth();
        int baseDepth = zoneType.getDefaultDepth();

        for (double scale : new double[]{1.0, 0.8, 0.6}) {
            int width = Math.max(4, (int) (baseWidth * scale));
            int depth = Math.max(4, (int) (baseDepth * scale));

            double startX = roadPos.getX() + nx * roadOffset;
            double startZ = roadPos.getZ() + nz * roadOffset;

            double[][] corners = new double[4][2];
            corners[0] = new double[]{startX, startZ};
            corners[1] = new double[]{startX + nx * depth, startZ + nz * depth};
            corners[2] = new double[]{startX + nx * depth + tx * width, startZ + nz * depth + tz * width};
            corners[3] = new double[]{startX + tx * width, startZ + tz * width};

            int minX = (int) Math.floor(Math.min(Math.min(corners[0][0], corners[1][0]), Math.min(corners[2][0], corners[3][0])));
            int maxX = (int) Math.ceil(Math.max(Math.max(corners[0][0], corners[1][0]), Math.max(corners[2][0], corners[3][0])));
            int minZ = (int) Math.floor(Math.min(Math.min(corners[0][1], corners[1][1]), Math.min(corners[2][1], corners[3][1])));
            int maxZ = (int) Math.ceil(Math.max(Math.max(corners[0][1], corners[1][1]), Math.max(corners[2][1], corners[3][1])));

            // ПРОВЕРКА 1: Проверка на пересечение дорог и стен
            if (intersectsForbiddenZone(minX, maxX, minZ, maxZ, forbiddenPositions)) {
                continue;
            }

            // ПРОВЕРКА 2: Границы стен и центральной площади
            boolean fitsInCity = true;
            for (double[] corner : corners) {
                double cDist = Math.hypot(corner[0] - settlement.origin.getX(), corner[1] - settlement.origin.getZ());
                if (cDist < style.centralPlazaRadius() + 1) { fitsInCity = false; break; }
                if (style.hasWalls() && cDist >= style.maxRadius() - style.wallThickness() - 1) { fitsInCity = false; break; }
            }
            if (!fitsInCity) continue;

            // ПРОВЕРКА 3: Вода
            boolean waterCheck = false;
            for (double[] corner : corners) {
                BlockPos surface = PathfindingService.getCleanSurfacePos(world, (int) corner[0], (int) corner[1], 0);
                if (world.getBlockState(surface).is(Blocks.WATER)) { waterCheck = true; break; }
            }
            if (waterCheck) continue;

            BlockPos p1 = PathfindingService.getCleanSurfacePos(world, minX, minZ, 0);
            BlockPos p2 = PathfindingService.getCleanSurfacePos(world, maxX, maxZ, 0);

            BlockPos minPos = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
            BlockPos maxPos = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

            BlockPos doorPos = PathfindingService.getCleanSurfacePos(world, (int) Math.round(startX), (int) Math.round(startZ), 0);
            Direction facing = Direction.getApproximateNearest(-nx, 0.0, -nz);

            // Создаем участок с валидными координатами
            String plotName = resolvedType.name() + "_" + existingPlots.size();
            BuildingPlot candidatePlot = new BuildingPlot(settlement.getId(), plotName, minPos, maxPos, resolvedType);
            candidatePlot.setAnchorPos(doorPos);
            candidatePlot.setFacing(facing);

            // ПРОВЕРКА 4: Накладывание на соседние участки
            boolean intersectsOther = false;
            for (BuildingPlot existing : existingPlots) {
                if (candidatePlot.intersects(existing)) {
                    intersectsOther = true;
                    break;
                }
            }

            if (!intersectsOther) {
                return candidatePlot;
            }
        }

        return null;
    }

    private static boolean intersectsForbiddenZone(int minX, int maxX, int minZ, int maxZ, Set<Long> forbiddenPositions) {
        int margin = 1;
        for (int x = minX - margin; x <= maxX + margin; x++) {
            for (int z = minZ - margin; z <= maxZ + margin; z++) {
                if (forbiddenPositions.contains(pack2D(x, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long pack2D(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    private static ZoneType calculateZoneType(double distance, int maxRadius) {
        double ratio = distance / maxRadius;
        if (ratio < 0.20) return ZoneType.CIVIC;
        if (ratio < 0.45) return ZoneType.COMMERCIAL;
        if (ratio < 0.75) return ZoneType.RESIDENTIAL;
        return ZoneType.INDUSTRIAL;
    }
}