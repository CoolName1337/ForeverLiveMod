package foreverlive.modid.politics.settlement.plot.layout;

import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class BuildingLayout {

    public enum ModuleType { WALL, PILLAR, WINDOW, DOOR, CORNER, FLOOR, ROOF }
    private final List<PlacedElement> elements = new ArrayList<>();
    public void add(PlacedElement element) {
        this.elements.add(element);
    }

    public List<PlacedElement> getElements() {
        return elements;
    }

    private final List<PlacedElement> modules = new ArrayList<>();

    public void addModule(ModuleCategory category, int width, int height, BlockPos relPos, Direction facing) {
        modules.add(new PlacedElement(category, relPos, width, height,facing));
    }

    public List<PlacedElement> getModules() {
        return modules;
    }

    public void clear() {
        modules.clear();
    }

    // --- Сериализация ---

    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (PlacedElement module : modules) {
            list.add(module.saveToNBT());
        }
        tag.put("Modules", list);
        return tag;
    }

    public static BuildingLayout loadFromNBT(CompoundTag tag) {
        BuildingLayout layout = new BuildingLayout();
        if (tag.contains("Modules")) {
            ListTag list = tag.getList("Modules").get();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag modTag = list.getCompound(i).get();
                layout.modules.add(PlacedElement.loadFromNBT(modTag));
            }
        }
        return layout;
    }
}