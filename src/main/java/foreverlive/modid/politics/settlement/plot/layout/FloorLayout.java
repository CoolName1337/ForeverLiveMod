package foreverlive.modid.politics.settlement.plot.layout;

import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class FloorLayout {
    private final int floorIndex;
    private final FloorConfig config;
    private final List<PlacedElement> elements = new ArrayList<>();

    public FloorLayout(int floorIndex, FloorConfig config) {
        this.floorIndex = floorIndex;
        this.config = config;
    }

    public void add(PlacedElement element) {
        this.elements.add(element);
    }

    // --- Сериализация в NBT ---

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("FloorIndex", floorIndex);

        // 1. Сохраняем FloorConfig
        CompoundTag configTag = new CompoundTag();
        configTag.putInt("Height", config.height());
        configTag.putString("StyleFamily", config.styleFamily());
        configTag.putString("Material", config.material());
        configTag.putBoolean("AllowDoors", config.allowDoors());
        configTag.putInt("OverhangRadius", config.overhangRadius());
        tag.put("Config", configTag);

        // 2. Сохраняем список PlacedElement
        ListTag elementsList = new ListTag();
        for (PlacedElement element : elements) {
            CompoundTag elemTag = new CompoundTag();
            elemTag.putString("Category", element.category().name());
            elemTag.putLong("Pos", element.relPos().asLong());
            elemTag.putInt("Width", element.width());
            elemTag.putInt("Height", element.height());
            elemTag.putString("Facing", element.facing().getName());
            elementsList.add(elemTag);
        }
        tag.put("Elements", elementsList);

        return tag;
    }

    // --- Десериализация из NBT ---

    public static FloorLayout loadFromNBT(CompoundTag tag) {
        int floorIndex = tag.getInt("FloorIndex").get();

        // 1. Загружаем FloorConfig
        CompoundTag configTag = tag.getCompound("Config").get();
        FloorConfig config = new FloorConfig(
                configTag.getInt("Height").get(),
                configTag.getString("StyleFamily").get(),
                configTag.getString("Material").get(),
                configTag.getBoolean("AllowDoors").get(),
                configTag.getInt("OverhangRadius").get()
        );

        FloorLayout floorLayout = new FloorLayout(floorIndex, config);

        // 2. Загружаем PlacedElement
        if (tag.contains("Elements")) {
            ListTag elementsList = tag.getList("Elements").get();
            for (int i = 0; i < elementsList.size(); i++) {
                CompoundTag elemTag = elementsList.getCompound(i).get();

                ModuleCategory category = ModuleCategory.valueOf(elemTag.getString("Category").get());
                BlockPos pos = BlockPos.of(elemTag.getLong("Pos").get());
                int width = elemTag.getInt("Width").get();
                int height = elemTag.getInt("Height").get();
                Direction facing = Direction.byName(elemTag.getString("Facing").get());

                floorLayout.add(new PlacedElement(category, pos, width, height, facing));
            }
        }

        return floorLayout;
    }

    // --- Геттеры ---

    public int getFloorIndex() { return floorIndex; }
    public FloorConfig getConfig() { return config; }
    public List<PlacedElement> getElements() { return elements; }
}