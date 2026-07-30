package foreverlive.modid.politics.settlement.plot.builders;

import foreverlive.modid.politics.settlement.plot.ModuleRegistry;
import foreverlive.modid.politics.settlement.plot.BuildingModule;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.plot.layout.FloorConfig;
import foreverlive.modid.politics.settlement.plot.layout.PlacedElement;
import foreverlive.modid.politics.settlement.plot.utils.StructureMathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlotRenderer {
    public static void render(ServerLevel level, BuildingPlot plot) {
        BlockPos minWorldPos = plot.getMinPos();
        StructureTemplateManager templateManager = level.getStructureManager();
        RandomSource random = level.getRandom();

        for (FloorLayout floor : plot.getFloors()) {
            FloorConfig config = floor.getConfig();

            // 1. Собираем теги для поисковика из плота и конфигурации этажа
            Set<String> searchTags = new HashSet<>();
            for (String tag : plot.getTags()) searchTags.add(tag.toUpperCase());
            if (config.styleFamily() != null) searchTags.add(config.styleFamily().toUpperCase());
            if (config.material() != null) searchTags.add(config.material().toUpperCase());

            // 2. Итерируемся по расставленным абстрактным элементам
            for (PlacedElement element : floor.getElements()) {

                // Ищем наиболее подходящий NBT-модуль
                Optional<BuildingModule> moduleOpt = ModuleRegistry.findModule(
                        element.category(),
                        element.width(),
                        element.height(),
                        searchTags,
                        random
                );

                if (moduleOpt.isEmpty()) {
                    System.err.println("⚠️ Не найден NBT модуль для категории: " + element.category());
                    continue;
                }

                BuildingModule module = moduleOpt.get();

                // 3. Загружаем NBT структуру по Identifier из реестра
                Optional<StructureTemplate> templateOpt = templateManager.get(module.nbtPath());
                if (templateOpt.isEmpty()) {
                    System.err.println("❌ NBT файл не найден по пути: " + module.nbtPath());
                    continue;
                }

                // 4. Вычисляем абсолютные координаты и повороты
                BlockPos targetWorldPos = minWorldPos.offset(element.relPos());

                StructurePlaceSettings settings = new StructurePlaceSettings()
                        .setRotation(calculateRotation(element.facing()));

                // Размещаем структуру в мире
                templateOpt.get().placeInWorld(
                        level,
                        targetWorldPos,
                        targetWorldPos,
                        settings,
                        random,
                        2 // Флаг обновления блоков
                );
            }
        }
    }
    private static Rotation calculateRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case EAST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}