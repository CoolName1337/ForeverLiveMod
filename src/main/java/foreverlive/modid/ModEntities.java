package foreverlive.modid;

import foreverlive.modid.entities.NpcEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final EntityType<NpcEntity> NPC = register("npc",
            EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.95f));

    private static <T extends Entity> EntityType<T> register(
            String id, EntityType.Builder<T> builder){
        Identifier identifier = Identifier.fromNamespaceAndPath("forever_live", id);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, identifier);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, identifier, builder.build(key));
    }
}
