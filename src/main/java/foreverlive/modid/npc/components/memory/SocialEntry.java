package foreverlive.modid.npc.components.memory;

import java.util.UUID;

public class SocialEntry {
    public UUID targetId;
    public float opinion; // -100 (враг) до 100 (брат)
    public long lastMet;

    public SocialEntry(UUID id, float op) {
        this.targetId = id;
        this.opinion = op;
        this.lastMet = System.currentTimeMillis();
    }
}