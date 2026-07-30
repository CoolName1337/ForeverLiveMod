package foreverlive.modid.politics.settlement.plot.builders;

import foreverlive.modid.ModuleRegistry;
import foreverlive.modid.politics.settlement.plot.BuildingModule;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.plot.layout.PlacedElement;
import foreverlive.modid.politics.settlement.plot.utils.StructureMathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;

public class PlotRenderer {

    public static void render(ServerLevel level, BuildingPlot plot) {
        BlockPos minWorldPos = plot.getMinPos();

        for (FloorLayout floor : plot.getFloors()) {
            // Читаем индивидуальный стиль и материал ЭТОГО этажа!

            for (PlacedElement element : floor.getElements()) {

                BuildingModule module = ModuleRegistry.findModule(
                        element.category(),
                        element.width(),
                        element.height(),
                        plot.getTags()
                ).orElse(null);

                Optional<StructureTemplate> templateOpt = level.getStructureManager().get(module.nbtPath());
                if (templateOpt.isEmpty()) {
                    System.out.println("❌ [BUILDER ERROR] Файл NBT не найден по пути: " + module.nbtPath());
                    continue;
                }

                StructureTemplate template = templateOpt.get();
                BlockPos targetWorldPos = minWorldPos.offset(element.relPos());

                StructureMathHelper.PlacementResult math = StructureMathHelper.calculate(
                        targetWorldPos,
                        element.facing(),
                        template
                );

                StructurePlaceSettings settings = new StructurePlaceSettings()
                        .setRotation(math.rotation())
                        .setIgnoreEntities(true);

                template.placeInWorld(level, math.finalPos(), math.finalPos(), settings, level.getRandom(), 2);
            }
        }
    }
}