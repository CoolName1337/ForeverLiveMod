package foreverlive.modid.politics.settlement;

import foreverlive.modid.politics.POJO.RoadSegment;
import foreverlive.modid.politics.settlement.enums.PlotType;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class Settlement {

    private final UUID id = UUID.randomUUID();
    private String name;
    public BlockPos origin;
    private int tier = 1;

    // Статистика
    private int radius = 30;
    private int loyalty = 100;
    private int population = 0;
    private int wealth = 30;

    // Участки под застройку (уже с построенными/запланированными домами)
    private final List<BuildingPlot> plots = new ArrayList<>();

    // Дорожная сеть
    private final List<RoadSegment> roads = new ArrayList<>();

    // Стены
    private Set<Long> wallPositions = new HashSet<>();

    public Settlement(BlockPos origin) {
        this.origin = origin;
    }

    public void tick(ServerLevel world) {
        // Симуляция жизни: сбор налогов, потребление еды, симуляция NPC
    }

    // --- Методы аналитики для генераторов и AI ---

    /**
     * Подсчитывает количество участков каждого типа в поселении
     */
    public Map<PlotType, Integer> getPlotCountsByType() {
        Map<PlotType, Integer> counts = new EnumMap<>(PlotType.class);
        for (BuildingPlot plot : plots) {
            if (plot.getType() != null) {
                counts.put(plot.getType(), counts.getOrDefault(plot.getType(), 0) + 1);
            }
        }
        return counts;
    }

    /**
     * Возвращает текущее количество жителей (алиас для getPopulation)
     */
    public int getResidentCount() {
        return this.population;
    }

    /**
     * Безопасное добавление нового участка
     */
    public void addPlot(BuildingPlot plot) {
        this.plots.add(plot);
    }

    /**
     * Объединяет дороги и стены в единую зону, куда домам вход воспрещен
     */
    public Set<Long> getAllForbiddenPositions() {
        Set<Long> forbidden = new HashSet<>(wallPositions);
        for (RoadSegment road : roads) {
            if (road != null && road.getPoints() != null) {
                for (BlockPos pos : road.getPoints()) {
                    forbidden.add(((long) pos.getX() & 0xFFFFFFFFL) | (((long) pos.getZ() & 0xFFFFFFFFL) << 32));
                }
            }
        }
        return forbidden;
    }

    // --- Геттеры и Сеттеры ---
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Long> getWallPositions() { return wallPositions; }
    public void setWallPositions(Set<Long> wallPositions) { this.wallPositions = wallPositions; }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; }

    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }

    public List<BuildingPlot> getPlots() { return plots; }
    public List<RoadSegment> getRoads() { return roads; }
}