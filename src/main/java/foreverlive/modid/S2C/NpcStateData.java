package foreverlive.modid.S2C;

import foreverlive.modid.npc.components.proccessing.NeedType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record NpcStateData(
        Optional<Map<NeedType, Float>> needs,
        Optional<Integer> mood,
        Optional<NeedType> goal
) {
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<NeedType, Float>> NEEDS_MAP_CODEC =
            ByteBufCodecs.map(HashMap::new, NeedType.CODEC.cast(), ByteBufCodecs.FLOAT);

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcStateData> CODEC = StreamCodec.composite(
            NEEDS_MAP_CODEC.apply(ByteBufCodecs::optional), NpcStateData::needs,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs::optional), NpcStateData::mood,
            NeedType.CODEC.cast().apply(ByteBufCodecs::optional), NpcStateData::goal,
            NpcStateData::new
    );
}