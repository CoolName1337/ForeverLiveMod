package foreverlive.modid.politics.settlement.plot;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleRegistry extends SimpleJsonResourceReloadListener<List<BuildingModule>> implements IdentifiableResourceReloadListener {

    private static final List<BuildingModule> MODULES = new ArrayList<>();

    public ModuleRegistry() {
        super(BuildingModule.CODEC.listOf(), FileToIdConverter.json("building_modules"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("foreverlive", "building_modules_reload_listener");
    }

    @Override
    protected void apply(Map<Identifier, List<BuildingModule>> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        MODULES.clear();
        for (List<BuildingModule> moduleList : prepared.values()) {
            MODULES.addAll(moduleList);
        }
        System.out.println("[ForeverLive] Загружено NBT-модулей зданий: " + MODULES.size());
    }

    /**
     * Умный поиск модуля с учетом высоты, ширины, совпадения тегов и рандомизацией
     */
    public static Optional<BuildingModule> findModule(
            ModuleCategory category,
            int width,
            int height,
            Set<String> requiredTags,
            RandomSource random
    ) {
        // 1. Фильтруем категории и ВЫСОТУ (высота этажа критична — нельзя менять height!)
        List<BuildingModule> candidates = MODULES.stream()
                .filter(m -> m.category() == category)
                .filter(m -> m.height() == height)
                .toList();

        if (candidates.isEmpty()) {
            System.err.println("❌ Ошибка: В реестре нет модулей категории " + category + " высотой " + height);
            return Optional.empty();
        }

        // 2. Ищем идеальные совпадения по ширине и максимальному совпадению тегов
        Map<BuildingModule, Integer> scoredCandidates = new HashMap<>();

        for (BuildingModule candidate : candidates) {
            int score = 0;

            // Точное совпадение по ширине дает огромный приоритет
            if (candidate.width() == width) {
                score += 100;
            }

            // Подсчет совпавших тегов (Материал, Стиль, Уровень)
            for (String reqTag : requiredTags) {
                if (candidate.tags().contains(reqTag)) {
                    score += 10;
                }
            }

            scoredCandidates.put(candidate, score);
        }

        // 3. Находим максимальный балл
        int maxScore = scoredCandidates.values().stream()
                .max(Integer::compareTo)
                .orElse(-1);

        if (maxScore <= 0) {
            // Если ничего из тегов не совпало — берем любой кандидат нужной высоты и ширины
            List<BuildingModule> sizeOnly = candidates.stream()
                    .filter(m -> m.width() == width)
                    .toList();
            if (!sizeOnly.isEmpty()) {
                return Optional.of(sizeOnly.get(random.nextInt(sizeOnly.size())));
            }
            return Optional.of(candidates.get(random.nextInt(candidates.size())));
        }

        // 4. Собираем ВСЕ модули с максимальным баллом (для вариативности декора)
        List<BuildingModule> bestMatches = scoredCandidates.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. Возвращаем СЛУЧАЙНЫЙ из лучших (разные текстуры/детали одного типа)
        return Optional.of(bestMatches.get(random.nextInt(bestMatches.size())));
    }
}