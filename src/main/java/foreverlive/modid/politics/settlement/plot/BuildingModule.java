package foreverlive.modid.politics.settlement.plot;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.*;

public record BuildingModule(
        Identifier nbtPath,
        ModuleCategory category,
        int width,
        int height,
        Set<String> tags
) {
    // 1. Описываем Codec для рекорд-класса
    public static final Codec<BuildingModule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("nbt_path").forGetter(BuildingModule::nbtPath),
            ModuleCategory.CODEC.fieldOf("category").forGetter(BuildingModule::category),
            Codec.INT.fieldOf("width").forGetter(BuildingModule::width),
            Codec.INT.fieldOf("height").forGetter(BuildingModule::height),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("tags").forGetter(m -> Set.copyOf(m.tags()))
    ).apply(instance, BuildingModule::new));

}