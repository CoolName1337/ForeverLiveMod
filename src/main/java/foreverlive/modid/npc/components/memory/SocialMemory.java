package foreverlive.modid.npc.components.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SocialMemory {
    // Используем компактный объект. В списке храним только "значимых" личностей.
    private final List<SocialEntry> relations = new ArrayList<>();
    private static final int MAX_RELATIONS = 20; // Григорий не может помнить всех

    public void modifyOpinion(UUID id, float delta) {
        for (SocialEntry entry : relations) {
            if (entry.targetId.equals(id)) {
                entry.opinion = Math.max(-100, Math.min(100, entry.opinion + delta));
                entry.lastMet = System.currentTimeMillis();
                return;
            }
        }

        // Если места нет, выкидываем самого старого/неважного
        if (relations.size() >= MAX_RELATIONS) {
            relations.sort(Comparator.comparingLong(e -> e.lastMet));
            relations.remove(0);
        }
        relations.add(new SocialEntry(id, delta));
    }

    public float getOpinion(UUID id) {
        for (SocialEntry entry : relations) {
            if (entry.targetId.equals(id)) return entry.opinion;
        }
        return 0; // По умолчанию нейтрально
    }
}