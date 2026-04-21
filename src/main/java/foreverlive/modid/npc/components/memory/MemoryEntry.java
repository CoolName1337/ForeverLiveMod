package foreverlive.modid.npc.components.memory;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

public class MemoryEntry{

    private final MemoryCategory category;
    private final EnumSet<MemoryTag> tags;
    private int importance;
    private long when;
    private long expires; // -1 чтобы вечно
    private @Nullable BlockPos position;
    private @Nullable UUID entityId;

    public MemoryCategory getCategory() {
        return category;
    }

    public EnumSet<MemoryTag> getTags() {
        return tags;
    }
    public void addTags(MemoryTag... tags){
        this.tags.addAll(Arrays.stream(tags).toList());
    }
    public void removeTag(MemoryTag tag){
        this.tags.remove(tag);
    }

    public int getImportance() {
        return importance;
    }
    public void setImportance(int importance) {
        this.importance = importance;
    }

    public long getWhen() {
        return when;
    }
    public void setWhen(long when) {
        this.when = when;
    }

    public long getExpires() {
        return expires;
    }
    public void setExpires(long expires) {
        this.expires = expires;
    }

    public @Nullable BlockPos getPosition() {
        return position;
    }
    public void setPosition(@Nullable BlockPos position) {
        this.position = position;
    }

    public @Nullable UUID getEntityId() {
        return entityId;
    }

    public MemoryEntry(MemoryCategory category, EnumSet<MemoryTag> tags, int importance, long when, long duration, @Nullable BlockPos position, @Nullable UUID uuid){
        this.category = category;
        this.tags = tags;
        this.importance = importance;
        this.when = when;
        this.expires = (duration == -1) ? -1 : when + duration;
        this.position = position;
        this.entityId = uuid;
    }

    public boolean isPersistent() {
        return expires == -1;
    }
    public boolean isExpired(long currentTick) {
        if (isPersistent()) return false;
        return currentTick > expires;
    }

    public boolean hasTag(MemoryTag tag) {
        return tags.contains(tag);
    }
    public boolean hasAllTags(MemoryTag... tags){
        for (MemoryTag tag : tags) {
        if (!this.tags.contains(tag)) return false;
    }
        return true;
    }
}
