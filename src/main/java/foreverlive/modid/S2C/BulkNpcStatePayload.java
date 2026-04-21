package foreverlive.modid.S2C;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

// Пакет теперь содержит список обновлений для разных сущностей
public record BulkNpcStatePayload(
        Map<Integer, NpcStateData> updates
) implements CustomPacketPayload {
    public static final Type<BulkNpcStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("forever_live", "bulk_npc_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulkNpcStatePayload> CODEC = StreamCodec.composite(
            // Кодек для карты: ID сущности -> Данные состояния
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, NpcStateData.CODEC), BulkNpcStatePayload::updates,
            BulkNpcStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}

