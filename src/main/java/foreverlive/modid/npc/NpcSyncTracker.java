package foreverlive.modid.npc;

import foreverlive.modid.S2C.NpcStateData;
import foreverlive.modid.S2C.NpcSyncManager;
import foreverlive.modid.npc.components.proccessing.NeedType;
import foreverlive.modid.entities.NpcEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

import java.util.*;

public class NpcSyncTracker {

    private static final long MIN_SYNC_INTERVAL = 1000;
    private static final float SYNC_THRESHOLD = 0.05f; // Синхроним, если изменилось на указанный процент

    private final Set<SyncCategory> dirtyCategories = EnumSet.noneOf(SyncCategory.class);
    private final Map<SyncCategory, Long> lastSyncTimestamps = new EnumMap<>(SyncCategory.class);
    private final Map<NeedType, Float> lastSyncedNeeds = new EnumMap<>(NeedType.class);

    private final NpcEntity npc;

    public NpcSyncTracker(NpcEntity npc){
        this.npc = npc;
    }

    public void markDirty(SyncCategory category){
        this.dirtyCategories.add(category);
    }

    public void checkNeedThreshold(NeedType type, float newValue) {
        float lastValue = lastSyncedNeeds.getOrDefault(type, -1f);
        if (Math.abs(newValue - lastValue) > SYNC_THRESHOLD) {
            markDirty(SyncCategory.NEEDS);
        }
    }

    public void tickSync() {
        if (dirtyCategories.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        var needsUpdate = Optional.<Map<NeedType, Float>>empty();
        var moodUpdate = Optional.<Integer>empty();
        var goalUpdate = Optional.<NeedType>empty();

        // Проходим по "грязным" категориям
        Iterator<SyncCategory> iterator = dirtyCategories.iterator();
        while (iterator.hasNext()) {
            SyncCategory category = iterator.next();

            // Проверка кулдауна на отправку
            if (category != SyncCategory.GOAL) {
                long lastSync = lastSyncTimestamps.getOrDefault(category, 0L);
                if (currentTime - lastSync < MIN_SYNC_INTERVAL) continue;
            }

            switch (category) {
                case NEEDS -> {
                    Map<NeedType, Float> values = new HashMap<>();

                    for (NeedType type : NeedType.values()){
                        float needValue = npc.getNeedValue(type);
                        lastSyncedNeeds.put(type, needValue);
                        values.put(type, needValue);
                    }
                    needsUpdate = Optional.of(values);
                }
                case GOAL -> {
                    goalUpdate = Optional.ofNullable(npc.brain.getCurrentGoal());
                }
            }

            lastSyncTimestamps.put(category, currentTime);
            iterator.remove(); // Очищаем категорию после обработки
        }

        // Если есть что отправлять — пакуем и в очередь
        if (needsUpdate.isPresent() || moodUpdate.isPresent() || goalUpdate.isPresent()) {
            NpcStateData stateData = new NpcStateData(needsUpdate, moodUpdate, goalUpdate);
            PlayerLookup.tracking(this.npc).forEach(player ->
                    NpcSyncManager.enqueueUpdate(player, this.npc.getId(), stateData)
            );
        }
    }

    public void updateClientState(NpcStateData data) {
        data.needs().ifPresent(remoteNeeds -> {
            remoteNeeds.forEach(this.npc::setNeedValue);
        });
        data.goal().ifPresent(this.npc.brain::setCurrentGoal);
    }
}
