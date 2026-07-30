package foreverlive.modid.politics.services;

import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.settlement.SettlementStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TerraformService {

    public static void terraformSettlementArea(ServerLevel world, Settlement settlement, SettlementStyle style, int plateauY) {
        if (settlement == null || settlement.origin == null) return;

        BlockPos center = settlement.origin;
        int baseCityRadius = style.maxRadius();

        BlockState topBlock = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirtBlock = Blocks.DIRT.defaultBlockState();
        BlockState stoneBlock = Blocks.STONE.defaultBlockState();
        BlockState airBlock = Blocks.AIR.defaultBlockState();
        BlockState retainingWallBlock = Blocks.STONE_BRICKS.defaultBlockState();

        // Максимальный радиус сканирования с запасом на шум и плавные склоны
        int maxScanRadius = baseCityRadius + 35;

        for (int dx = -maxScanRadius; dx <= maxScanRadius; dx++) {
            for (int dz = -maxScanRadius; dz <= maxScanRadius; dz++) {
                double distance = Math.hypot(dx, dz);
                if (distance > maxScanRadius) continue;

                int x = center.getX() + dx;
                int z = center.getZ() + dz;

                // 1. ИСКРИВЛЕНИЕ ГРАНИЦ (Органичный шум вместо идеального круга)
                double angle = Math.atan2(dz, dx);
                double noiseOffset = Math.sin(angle * 3) * 7.0
                        + Math.cos(angle * 7) * 4.0
                        + Math.sin(angle * 13) * 2.0;

                double effectiveCityRadius = baseCityRadius + noiseOffset;

                // Находим естественную высоту рельефа в этой точке
                int currentGroundY = PathfindingService.getCleanSurfacePos(world, x, z, plateauY).getY();
                int heightDiff = Math.abs(currentGroundY - plateauY);

                // 2. ДИНАМИЧЕСКИЙ РАДИУС СГЛАЖИВАНИЯ
                // Чем выше срезаемая гора, тем длиннее должен быть откос (минимум 2 блока длины на 1 блок высоты)
                int dynamicBlendRadius = Math.max(16, heightDiff * 2);

                int desiredY;

                if (distance <= effectiveCityRadius) {
                    // Внутри зоны города — строго выравниваем в плато
                    desiredY = plateauY;
                } else if (distance <= effectiveCityRadius + dynamicBlendRadius) {
                    // В зоне откоса — плавная интерполяция (Smoothstep) к естественному рельефу
                    double factor = (distance - effectiveCityRadius) / (double) dynamicBlendRadius;
                    double smoothFactor = factor * factor * (3 - 2 * factor); // Smoothstep

                    desiredY = (int) Math.round((1 - smoothFactor) * plateauY + smoothFactor * currentGroundY);
                } else {
                    // За пределами — не трогаем рельеф
                    continue;
                }

                // 3. ПРИМЕНЕНИЕ И УМНАЯ ОБРАБОТКА СТЕНОК/ОБРЫВОВ
                applySmartTerraforming(world, x, z, currentGroundY, desiredY, plateauY, distance <= effectiveCityRadius,
                        topBlock, dirtBlock, stoneBlock, airBlock, retainingWallBlock);
            }
        }

    }

    private static void applySmartTerraforming(ServerLevel world, int x, int z, int currentY, int targetY, int plateauY,
                                               boolean insideCity, BlockState topBlock, BlockState dirtBlock,
                                               BlockState stoneBlock, BlockState airBlock, BlockState wallBlock) {

        if (currentY < targetY) {
            // ЗАПОЛНЕНИЕ ЯМ И ОВРАГОВ
            for (int y = currentY; y < targetY; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (targetY - y <= 1) {
                    WorldBuildQueue.enqueue(pos, dirtBlock);
                } else if (targetY - y <= 4) {
                    WorldBuildQueue.enqueue(pos, dirtBlock);
                } else {
                    WorldBuildQueue.enqueue(pos, stoneBlock);
                }
            }
            WorldBuildQueue.enqueue(new BlockPos(x, targetY, z), topBlock);

        } else if (currentY > targetY) {
            // СРЕЗАНИЕ ХОЛМОВ И ГОР

            // Очищаем верхушку горы до targetY
            for (int y = targetY + 1; y <= currentY + 10; y++) {
                WorldBuildQueue.enqueue(new BlockPos(x, y, z), airBlock);
            }

            int drop = currentY - targetY;

            // Если срез получился слишком крутым на границе города — оформляем подпорную стену или скалу
            if (insideCity && drop > 4) {
                // Ставим подпорную каменную стену на краю городского плато
                WorldBuildQueue.enqueue(new BlockPos(x, targetY, z), wallBlock);
                WorldBuildQueue.enqueue(new BlockPos(x, targetY - 1, z), stoneBlock);
            } else {
                // Естественный дерновый покров
                WorldBuildQueue.enqueue(new BlockPos(x, targetY, z), topBlock);
                WorldBuildQueue.enqueue(new BlockPos(x, targetY - 1, z), dirtBlock);
            }

            // Ниже дерна заменяем висячую землю на натуральный камень
            for (int depth = 2; depth <= 5; depth++) {
                WorldBuildQueue.enqueue(new BlockPos(x, targetY - depth, z), stoneBlock);
            }
        } else {
            // Высота совпала — просто обновляем верх
            WorldBuildQueue.enqueue(new BlockPos(x, targetY, z), topBlock);
        }
    }
}