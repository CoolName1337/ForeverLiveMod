package foreverlive.modid.politics.settlement.plot;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

// Enum категорий с поддержкой Codec (через StringRepresentable)

public enum ModuleCategory implements StringRepresentable {
    CORNER("CORNER"),
    PILLAR("PILLAR"),
    WALL("WALL"),
    WINDOW("WINDOW"),
    FLOOR("FLOOR"),
    ROOF("ROOF"),
    DOOR("DOOR");

    public static final Codec<ModuleCategory> CODEC = StringRepresentable.fromEnum(ModuleCategory::values);
    private final String name;

    ModuleCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}