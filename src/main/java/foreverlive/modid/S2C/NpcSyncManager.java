package foreverlive.modid.S2C;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcSyncManager {
    // Очередь обновлений: Игрок -> (ID NPC -> Данные)
    private static final Map<UUID, Map<Integer, NpcStateData>> syncQueue = new HashMap<>();

    // Вызывается из NpcEntity, когда что-то изменилось
    public static void enqueueUpdate(ServerPlayer player, int entityId, NpcStateData data) {
        syncQueue.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(entityId, data);
    }

    // Главный метод, который вызывается в конце серверного тика
    public static void flush(MinecraftServer server) {
        if (syncQueue.isEmpty()) return;

        syncQueue.forEach((playerUUID, updates) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null && !updates.isEmpty()) {
                ServerPlayNetworking.send(player, new BulkNpcStatePayload(updates));
            }
        });
        syncQueue.clear();
    }
}
