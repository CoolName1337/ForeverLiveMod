package foreverlive.modid.politics.builder.generators;

import foreverlive.modid.politics.POJO.Plot;
import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.builder.SettlementStyle;
import foreverlive.modid.politics.builder.WorldBuildQueue;
import foreverlive.modid.politics.POJO.enums.ZoneType;
import foreverlive.modid.politics.builder.services.PathfindingService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class PlotScanner {

    public static List<Plot> scanAndAllocatePlots(ServerLevel world, Settlement settlement, SettlementStyle style, List<List<BlockPos>> allRoads) {
        List<Plot> plots = new ArrayList<>();
        BlockPos center = settlement.origin;

        int step = Math.max(3, style.plotScanStep());

        for (List<BlockPos> road : allRoads) {
            if (road.size() < step * 2) continue;

            for (int i = step; i < road.size() - step; i += step) {
                BlockPos current = road.get(i);

                BlockPos prev = road.get(Math.max(0, i - step));
                BlockPos next = road.get(Math.min(road.size() - 1, i + step));

                double dx = next.getX() - prev.getX();
                double dz = next.getZ() - prev.getZ();
                double length = Math.hypot(dx, dz);
                if (length == 0) continue;

                dx /= length;
                dz /= length;

                double leftNx = -dz;
                double leftNz = dx;
                double rightNx = dz;
                double rightNz = -dx;

                double distToCenter = Math.hypot(current.getX() - center.getX(), current.getZ() - center.getZ());
                ZoneType zoneType = calculateZoneType(distToCenter, style.maxRadius());

                Plot leftPlot = tryCreatePlot(world, current, leftNx, leftNz, dx, dz, style, zoneType, plots, allRoads, road, i, settlement);
                if (leftPlot != null) plots.add(leftPlot);

                Plot rightPlot = tryCreatePlot(world, current, rightNx, rightNz, dx, dz, style, zoneType, plots, allRoads, road, i, settlement);
                if (rightPlot != null) plots.add(rightPlot);
            }
        }

        return plots;
    }

    private static Plot tryCreatePlot(ServerLevel world, BlockPos roadPos, double nx, double nz, double tx, double tz,
                                      SettlementStyle style, ZoneType zoneType, List<Plot> existingPlots,
                                      List<List<BlockPos>> allRoads, List<BlockPos> currentRoad, int currentRoadIdx, Settlement settlement) {

        int roadRadius = style.roadWidth() / 2;
        // Запас +2 блока для компенсации "лесенки" на диагональных дорогах
        int roadOffset = roadRadius + style.plotRoadMargin() + 2;

        int baseWidth = zoneType.getDefaultWidth();
        int baseDepth = zoneType.getDefaultDepth();
        int spokeCount = Math.max(4, style.spokeCount());

        // Адаптивная попытка: если стандартный размер не влезает, пробуем чуть уменьшить (актуально для столицы)
        for (double scale : new double[]{1.0, 0.8, 0.65}) {
            int width = Math.max(8, (int) (baseWidth * scale));
            int depth = Math.max(8, (int) (baseDepth * scale));

            double startX = roadPos.getX() + nx * roadOffset;
            double startZ = roadPos.getZ() + nz * roadOffset;

            double[][] corners = new double[4][2];
            corners[0] = new double[]{startX, startZ};
            corners[1] = new double[]{startX + nx * depth, startZ + nz * depth};
            corners[2] = new double[]{startX + nx * depth + tx * width, startZ + nz * depth + tz * width};
            corners[3] = new double[]{startX + tx * width, startZ + tz * width};

            // 1. ПРОВЕРКА ВХОЖДЕНИЯ В СТЕНЫ (ТОЧНАЯ ПОЛЯРНАЯ МАТЕМАТИКА)
            boolean fitsWalls = true;
            for (double[] corner : corners) {
                double cDx = corner[0] - settlement.origin.getX();
                double cDz = corner[1] - settlement.origin.getZ();
                double cDist = Math.hypot(cDx, cDz);
                double angle = Math.atan2(cDz, cDx);

                // Центральная площадь
                if (cDist < style.centralPlazaRadius() + 3) {
                    fitsWalls = false;
                    break;
                }

                // Внешняя стена многоугольника
                double outerWallAtAngle = getPolygonRadius(angle, style.maxRadius(), spokeCount);
                if (cDist >= outerWallAtAngle - style.outerWallMargin()) {
                    fitsWalls = false;
                    break;
                }

                // Внутренняя стена многоугольника
                if (style.hasInnerWall()) {
                    double innerWallAtAngle = getPolygonRadius(angle, style.innerWallRadius(), spokeCount);
                    if (Math.abs(cDist - innerWallAtAngle) < style.innerWallBufferRadius()) {
                        fitsWalls = false;
                        break;
                    }
                }
            }

            if (!fitsWalls) continue;

            // Вычисление границ AABB
            int minX = (int) Math.floor(Math.min(Math.min(corners[0][0], corners[1][0]), Math.min(corners[2][0], corners[3][0])));
            int maxX = (int) Math.ceil(Math.max(Math.max(corners[0][0], corners[1][0]), Math.max(corners[2][0], corners[3][0])));
            int minZ = (int) Math.floor(Math.min(Math.min(corners[0][1], corners[1][1]), Math.min(corners[2][1], corners[3][1])));
            int maxZ = (int) Math.ceil(Math.max(Math.max(corners[0][1], corners[1][1]), Math.max(corners[2][1], corners[3][1])));

            BlockPos p1 = PathfindingService.getSurfacePos(world, new BlockPos(minX, roadPos.getY(), minZ));
            BlockPos p2 = PathfindingService.getSurfacePos(world, new BlockPos(maxX, roadPos.getY(), maxZ));

            BlockPos minPos = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
            BlockPos maxPos = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

            BlockPos doorPos = PathfindingService.getSurfacePos(world, new BlockPos((int) Math.round(startX), roadPos.getY(), (int) Math.round(startZ)));
            Direction facing = Direction.getApproximateNearest(-nx, 0.0, -nz);

            Plot candidatePlot = new Plot(minPos, maxPos, doorPos, facing, zoneType);

            // 2. ПРОВЕРКА ПЕРЕСЕЧЕНИЯ С ДОРОГАМИ
            if (intersectsRoadsAccurate(startX, startZ, nx, nz, tx, tz, depth, width, allRoads, currentRoad, currentRoadIdx, style.roadCheckIgnoreRadius(), style.roadWidth())) {
                continue;
            }

            // 3. ПРОВЕРКА ПЕРЕСЕЧЕНИЯ С ДРУГИМИ УЧАСТКАМИ
            boolean intersectsOtherPlot = false;
            for (Plot existing : existingPlots) {
                if (candidatePlot.intersects(existing)) {
                    intersectsOtherPlot = true;
                    break;
                }
            }

            if (!intersectsOtherPlot) {
                return candidatePlot; // Участок успешно создан!
            }
        }

        return null;
    }

    /**
     * Точный расчет расстояния от центра до грани правильного N-угольника под углом angle
     */
    private static double getPolygonRadius(double angle, double vertexRadius, int spokeCount) {
        double sector = (2.0 * Math.PI) / spokeCount;
        double normAngle = (angle % (2.0 * Math.PI) + (2.0 * Math.PI)) % (2.0 * Math.PI);
        double localAngle = normAngle % sector;
        double alpha = Math.abs(localAngle - (sector / 2.0));
        return (vertexRadius * Math.cos(sector / 2.0)) / Math.cos(alpha);
    }

    private static boolean intersectsRoadsAccurate(double startX, double startZ,
                                                   double nx, double nz, double tx, double tz,
                                                   int depth, int width,
                                                   List<List<BlockPos>> allRoads,
                                                   List<BlockPos> currentRoad, int currentRoadIdx,
                                                   int ignoreRadius, int roadWidth) {

        // Запас +2 блока безопасности для кривых диагоналей
        int safetyMargin = (roadWidth / 2) + 2;

        for (List<BlockPos> road : allRoads) {
            boolean isCurrentRoad = (road == currentRoad);

            for (int k = 0; k < road.size(); k++) {
                if (isCurrentRoad && Math.abs(k - currentRoadIdx) <= ignoreRadius) {
                    continue;
                }

                BlockPos p = road.get(k);

                double dx = p.getX() - startX;
                double dz = p.getZ() - startZ;

                double projN = dx * nx + dz * nz;
                double projT = dx * tx + dz * tz;

                if (projN >= -safetyMargin && projN <= depth + safetyMargin &&
                        projT >= -safetyMargin && projT <= width + safetyMargin) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ZoneType calculateZoneType(double distance, int maxRadius) {
        double ratio = distance / maxRadius;
        if (ratio < 0.20) return ZoneType.CIVIC;
        if (ratio < 0.45) return ZoneType.COMMERCIAL;
        if (ratio < 0.75) return ZoneType.RESIDENTIAL;
        return ZoneType.INDUSTRIAL;
    }

    public static void debugRenderPlots(List<Plot> plots) {
        for (Plot plot : plots) {
            BlockState colorBlock = switch (plot.getZoneType()) {
                case CIVIC -> Blocks.GOLD_BLOCK.defaultBlockState();
                case COMMERCIAL -> Blocks.YELLOW_WOOL.defaultBlockState();
                case RESIDENTIAL -> Blocks.LIME_WOOL.defaultBlockState();
                case INDUSTRIAL -> Blocks.BROWN_WOOL.defaultBlockState();
            };

            BlockPos min = plot.getMinPos();
            BlockPos max = plot.getMaxPos();

            for (int x = min.getX(); x <= max.getX(); x++) {
                WorldBuildQueue.enqueue(new BlockPos(x, min.getY(), min.getZ()), colorBlock);
                WorldBuildQueue.enqueue(new BlockPos(x, min.getY(), max.getZ()), colorBlock);
            }
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                WorldBuildQueue.enqueue(new BlockPos(min.getX(), min.getY(), z), colorBlock);
                WorldBuildQueue.enqueue(new BlockPos(max.getX(), min.getY(), z), colorBlock);
            }

            WorldBuildQueue.enqueue(plot.getDoorPos().above(), Blocks.REDSTONE_TORCH.defaultBlockState());
        }
    }
}