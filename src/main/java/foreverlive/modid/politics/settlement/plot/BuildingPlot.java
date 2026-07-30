package foreverlive.modid.politics.settlement.plot;

import foreverlive.modid.politics.settlement.PlotTask;
import foreverlive.modid.politics.settlement.PlotTaskManager;
import foreverlive.modid.politics.settlement.enums.PlotType;
import foreverlive.modid.politics.settlement.enums.RoleTag;
import foreverlive.modid.politics.settlement.enums.TaskType;
import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BuildingPlot {

    private final UUID plotId;
    private final UUID settlementId;
    private String name;

    private BlockPos minPos;
    private BlockPos maxPos;
    private BlockPos anchorPos;
    private Direction facing = Direction.NORTH;

    private Set<String> tags = new HashSet<>();
    private PlotType type;

    // --- Параметры постройки и Слои Этажей ---
    private int tier = 1;
    private final List<FloorLayout> floors;

    private final PlotTaskManager taskManager;
    private final Map<BlockPos, BlockState> damagedBlocks = new HashMap<>();

    private final Set<UUID> assignedWorkers = new HashSet<>();
    private final Set<UUID> assignedResidents = new HashSet<>();

    // --- Конструкторы ---

    public BuildingPlot(UUID settlementId, String name, BlockPos minPos, BlockPos maxPos, PlotType type) {
        this(UUID.randomUUID(), settlementId, name, minPos, maxPos, type, new ArrayList<>());
    }

    public BuildingPlot(BlockPos minPos, int widthX, int lengthZ, Direction facing, List<FloorLayout> floors) {
        this(UUID.randomUUID(), null, "Plot_" + UUID.randomUUID().toString().substring(0, 5),
                minPos, minPos.offset(widthX - 1, calculateTotalHeight(floors) - 1, lengthZ - 1),
                PlotType.RESIDENTIAL, floors);
        this.facing = facing;
    }

    public BuildingPlot(UUID plotId, UUID settlementId, String name, BlockPos minPos, BlockPos maxPos, PlotType type) {
        this(plotId, settlementId, name, minPos, maxPos, type, new ArrayList<>());
    }

    public BuildingPlot(UUID plotId, UUID settlementId, String name, BlockPos minPos, BlockPos maxPos, PlotType type, List<FloorLayout> floors) {
        this.plotId = plotId;
        this.settlementId = settlementId;
        this.name = name;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.anchorPos = new BlockPos(
                (minPos.getX() + maxPos.getX()) / 2,
                minPos.getY(),
                (minPos.getZ() + maxPos.getZ()) / 2
        );
        this.type = type;
        this.floors = new ArrayList<>(floors);
        this.taskManager = new PlotTaskManager(this);
    }

    // --- Геометрия и Коллизии ---

    public boolean intersects(BuildingPlot other) {
        int margin = 1;
        return this.minPos.getX() - margin <= other.maxPos.getX() && this.maxPos.getX() + margin >= other.minPos.getX() &&
                this.minPos.getZ() - margin <= other.maxPos.getZ() && this.maxPos.getZ() + margin >= other.minPos.getZ();
    }

    public boolean isInside(BlockPos pos) {
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX() &&
                pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY() &&
                pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
    }

    // --- Логика Тиков и Ремонта ---

    public void tick(Level level) {
        if (level.getGameTime() % 100 == 0) {
            scanAndGenerateTasks(level);
        }
        this.taskManager.tick(level);
    }

    private void scanAndGenerateTasks(Level level) {
        if (!damagedBlocks.isEmpty() && !taskManager.hasTaskType(TaskType.REPAIR_STRUCTURE)) {
            for (BlockPos pos : damagedBlocks.keySet()) {
                taskManager.addTask(new PlotTask(
                        TaskType.REPAIR_STRUCTURE,
                        pos,
                        RoleTag.BUILDER,
                        70
                ));
            }
        }
    }

    public void registerBlockDamage(BlockPos pos, BlockState originalState) {
        if (isInside(pos)) {
            this.damagedBlocks.put(pos, originalState);
        }
    }

    public void resolveBlockRepair(BlockPos pos) {
        this.damagedBlocks.remove(pos);
    }

    // --- Управление этажами ---

    public void addFloor(FloorLayout floor) {
        this.floors.add(floor);
        recalculateMaxHeight();
    }

    public void clearFloors() {
        this.floors.clear();
    }

    public FloorLayout getFloor(int index) {
        if (index >= 0 && index < floors.size()) {
            return floors.get(index);
        }
        return null;
    }

    public int getFloorsCount() {
        return floors.size();
    }

    private void recalculateMaxHeight() {
        int totalHeight = calculateTotalHeight(this.floors);
        if (totalHeight > 0) {
            this.maxPos = new BlockPos(this.maxPos.getX(), this.minPos.getY() + totalHeight - 1, this.maxPos.getZ());
        }
    }

    private static int calculateTotalHeight(List<FloorLayout> floorList) {
        int h = 0;
        for (FloorLayout f : floorList) {
            h += f.getConfig().height();
        }
        return Math.max(1, h);
    }

    // --- Сохранение в NBT ---

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();

        if (plotId != null) saveUUID(tag, "PlotId", plotId);
        if (settlementId != null) saveUUID(tag, "SettlementId", settlementId);

        tag.putString("Name", name);
        if (type != null) tag.putString("Type", type.name());
        tag.putString("Facing", facing.getName());

        tag.putLong("MinPos", minPos.asLong());
        tag.putLong("MaxPos", maxPos.asLong());
        tag.putLong("AnchorPos", anchorPos.asLong());

        tag.putInt("Tier", tier);

        // Сохранение списка этажей
        ListTag floorsTag = new ListTag();
        for (FloorLayout floor : floors) {
            floorsTag.add(floor.saveToNBT());
        }
        tag.put("Floors", floorsTag);

        // Теги
        ListTag tagsList = new ListTag();
        for (String tagStr : tags) {
            tagsList.add(StringTag.valueOf(tagStr));
        }
        tag.put("Tags", tagsList);

        // Рабочие
        ListTag workersTag = new ListTag();
        for (UUID workerId : assignedWorkers) {
            CompoundTag workerCompound = new CompoundTag();
            saveUUID(workerCompound, "ID", workerId);
            workersTag.add(workerCompound);
        }
        tag.put("Workers", workersTag);

        // Жители
        ListTag residentsTag = new ListTag();
        for (UUID residentId : assignedResidents) {
            CompoundTag residentCompound = new CompoundTag();
            saveUUID(residentCompound, "ID", residentId);
            residentsTag.add(residentCompound);
        }
        tag.put("Residents", residentsTag);

        // Поврежденные блоки
        ListTag damagedTag = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : damagedBlocks.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Pos", entry.getKey().asLong());
            entryTag.putInt("StateId", Block.getId(entry.getValue()));
            damagedTag.add(entryTag);
        }
        tag.put("DamagedBlocks", damagedTag);

        return tag;
    }

    // --- Загрузка из NBT ---

    public static BuildingPlot loadFromNBT(CompoundTag tag) {
        UUID plotId = tag.contains("PlotIdMost") ? loadUUID(tag, "PlotId") : UUID.randomUUID();
        UUID settlementId = tag.contains("SettlementIdMost") ? loadUUID(tag, "SettlementId") : null;
        String name = tag.getString("Name").get();
        PlotType type = tag.contains("Type") ? PlotType.valueOf(tag.getString("Type").get()) : PlotType.RESIDENTIAL;

        BlockPos min = BlockPos.of(tag.getLong("MinPos").get());
        BlockPos max = BlockPos.of(tag.getLong("MaxPos").get());

        BuildingPlot plot = new BuildingPlot(plotId, settlementId, name, min, max, type);

        if (tag.contains("AnchorPos")) plot.anchorPos = BlockPos.of(tag.getLong("AnchorPos").get());
        if (tag.contains("Facing")) plot.facing = Direction.byName(tag.getString("Facing").get());

        if (tag.contains("Tier")) plot.tier = tag.getInt("Tier").get();

        // Загрузка списка этажей
        if (tag.contains("Floors")) {
            ListTag floorsTag = tag.getList("Floors").get();
            for (int i = 0; i < floorsTag.size(); i++) {
                CompoundTag floorCompound = floorsTag.getCompound(i).get();
                plot.floors.add(FloorLayout.loadFromNBT(floorCompound));
            }
        }

        if (tag.contains("Tags")) {
            ListTag tagsList = tag.getList("Tags").get();
            for (int i = 0; i < tagsList.size(); i++) {
                plot.tags.add(tagsList.getString(i).get());
            }
        }

        if (tag.contains("Workers")) {
            ListTag workersTag = tag.getList("Workers").get();
            for (int i = 0; i < workersTag.size(); i++) {
                CompoundTag workerCompound = workersTag.getCompound(i).get();
                plot.assignedWorkers.add(loadUUID(workerCompound, "ID"));
            }
        }

        if (tag.contains("Residents")) {
            ListTag residentsTag = tag.getList("Residents").get();
            for (int i = 0; i < residentsTag.size(); i++) {
                CompoundTag residentCompound = residentsTag.getCompound(i).get();
                plot.assignedResidents.add(loadUUID(residentCompound, "ID"));
            }
        }

        if (tag.contains("DamagedBlocks")) {
            ListTag damagedTag = tag.getList("DamagedBlocks").get();
            for (int i = 0; i < damagedTag.size(); i++) {
                CompoundTag entryTag = damagedTag.getCompound(i).get();
                BlockPos pos = BlockPos.of(entryTag.getLong("Pos").get());
                BlockState state = Block.stateById(entryTag.getInt("StateId").get());
                plot.damagedBlocks.put(pos, state);
            }
        }

        return plot;
    }

    private void saveUUID(CompoundTag tag, String key, UUID uuid) {
        tag.putLong(key + "Most", uuid.getMostSignificantBits());
        tag.putLong(key + "Least", uuid.getLeastSignificantBits());
    }

    private static UUID loadUUID(CompoundTag tag, String key) {
        return new UUID(tag.getLong(key + "Most").get(), tag.getLong(key + "Least").get());
    }

    // --- Геттеры и Сеттеры ---

    public UUID getPlotId() { return plotId; }
    public UUID getSettlementId() { return settlementId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<String> getTags() { return tags; }

    public void setTags(Collection<String> newTags) {
        this.tags = new HashSet<>(newTags);
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    public BlockPos getMinPos() { return minPos; }
    public BlockPos getMaxPos() { return maxPos; }
    public BlockPos getAnchorPos() { return anchorPos; }
    public void setAnchorPos(BlockPos anchorPos) { this.anchorPos = anchorPos; }

    public Direction getFacing() { return facing; }
    public void setFacing(Direction facing) { this.facing = facing; }

    public PlotType getType() { return type; }
    public PlotTaskManager getTaskManager() { return taskManager; }
    public Map<BlockPos, BlockState> getDamagedBlocks() { return damagedBlocks; }

    public Set<UUID> getAssignedWorkers() { return assignedWorkers; }
    public Set<UUID> getAssignedResidents() { return assignedResidents; }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public List<FloorLayout> getFloors() { return floors; }

    // --- Хелперы размеров участка ---

    public int getWidthX() {
        return maxPos.getX() - minPos.getX() + 1;
    }

    public int getLengthZ() {
        return maxPos.getZ() - minPos.getZ() + 1;
    }

    public int getTotalHeight() {
        return maxPos.getY() - minPos.getY() + 1;
    }
}