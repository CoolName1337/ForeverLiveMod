package foreverlive.modid;

import foreverlive.modid.politics.settlement.plot.BuildingModule;
import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public class ModuleRegistry extends SimpleJsonResourceReloadListener<List<BuildingModule>> implements IdentifiableResourceReloadListener {

    private static final List<BuildingModule> MODULES = new ArrayList<>();

    public ModuleRegistry() {
        // Конструктор под новые версии MC: Codec + Конвертер папки data/<modid>/building_modules/*.json
        super(BuildingModule.CODEC.listOf(), FileToIdConverter.json("building_modules"));
    }

    // Уникальный ID релоадера для Fabric API
    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("foreverlive", "building_modules_reload_listener");
    }

    // Майнкрафт сам спарсил все JSON в объекты BuildingModule и передал в Map!
    @Override
    protected void apply(Map<Identifier, List<BuildingModule>> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        MODULES.clear();
        for (List<BuildingModule> moduleList : prepared.values()) {
            MODULES.addAll(moduleList);
        }
        System.out.println("[ForeverLive] Загружено NBT-модулей зданий: " + MODULES.size());
    }

    // --- Поисковые методы ---
    public static Optional<BuildingModule> findModule(ModuleCategory category, int width, int height, Set<String> requiredTags) {
        // 1. Идеально: Категория + Ширина + Высота + Теги
        Optional<BuildingModule> exactMatch = MODULES.stream()
                .filter(m -> m.category() == category)
                .filter(m -> m.width() == width && m.height() == height)
                .filter(m -> m.tags().containsAll(requiredTags))
                .findFirst();
        if (exactMatch.isPresent()) return exactMatch;

        // 2. Без тегов: Категория + Ширина + Высота
        Optional<BuildingModule> sizeMatch = MODULES.stream()
                .filter(m -> m.category() == category)
                .filter(m -> m.width() == width && m.height() == height)
                .findFirst();
        if (sizeMatch.isPresent()) return sizeMatch;

        // 3. ПРИОРИТЕТ ВЫСОТЕ: Если ширины 3 нет, берем ЛЮБУЮ ширину, НО строго нужной высоты
        Optional<BuildingModule> heightMatch = MODULES.stream()
                .filter(m -> m.category() == category)
                .filter(m -> m.height() == height)
                .findFirst();
        if (heightMatch.isPresent()) return heightMatch;

        // 4. Крайний фоллбек
        return MODULES.stream()
                .filter(m -> m.category() == category)
                .findFirst();
    }
    public static Set<Integer> getAvailableWidths(ModuleCategory category, int height, Set<String> tags) {
        Set<Integer> widths = new HashSet<>();
        for (BuildingModule m : MODULES) {
            if (m.category() == category && m.height() == height && m.tags().containsAll(tags)) {
                widths.add(m.width());
            }
        }
        return widths;
    }
}