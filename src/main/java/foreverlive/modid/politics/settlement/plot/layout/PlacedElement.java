package foreverlive.modid.politics.settlement.plot.layout;

import foreverlive.modid.politics.settlement.plot.ModuleCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Set;

/**
 * Одиночная инструкция: ЧТО поставить, КАКОГО размера, КУДА и КУДА СМОТРИТ.
 */
public record PlacedElement(
        ModuleCategory category, // WALL, CORNER, PILLAR, WINDOW, DOOR, FLOOR, ROOF
        BlockPos relPos,          // Относительные координаты от точки (0, 0, 0) участка
        int width,
        int height,
        Direction facing        // Направление наружу (куда смотрит фасад модуля)
) {
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", category.name());
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        tag.putLong("Pos", relPos.asLong());
        tag.putString("Dir", facing.getName());
        return tag;
    }

    public static PlacedElement loadFromNBT(CompoundTag tag) {
        ModuleCategory category = ModuleCategory.valueOf(tag.getString("Type").get());
        int width = tag.getInt("Width").get();
        int height = tag.getInt("Height").get();
        BlockPos relPos = BlockPos.of(tag.getLong("Pos").get());
        Direction facing = Direction.byName(tag.getString("Dir").get());
        return new PlacedElement(category, relPos, width, height,facing);
    }
}