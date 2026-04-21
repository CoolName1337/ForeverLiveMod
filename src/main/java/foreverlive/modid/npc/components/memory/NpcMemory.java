package foreverlive.modid.npc.components.memory;

import foreverlive.modid.Foreverlive;
import foreverlive.modid.entities.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class NpcMemory {
    private static final int MAX_ENTRIES_PER_CAT = 50;
    private final NpcEntity npc;
    private long lastScanTick = -1;
    public SocialMemory socialMemory = new SocialMemory();

    // L0: Instant data
    public final List<Entity> nearestEntities = new ArrayList<>();
    public LivingEntity target;

    // L1: Transient data
    private final Map<MemoryCategory, List<MemoryEntry>> storage = new EnumMap<>(MemoryCategory.class);

    // L2: Persistent data
    private final Map<String, MemoryEntry> persistentFacts = new HashMap<>();

    public NpcMemory(NpcEntity npc) {
        for (MemoryCategory cat : MemoryCategory.values()) {
            storage.put(cat, new ArrayList<>(MAX_ENTRIES_PER_CAT));
        }
        this.npc = npc;
    }
    private boolean isNewScanTick() {
        long now = npc.level().getGameTime();
        if (now > lastScanTick) {
            lastScanTick = now;
            return true;
        }
        return false;
    }

    public void updateL0(List<Entity> visibleNow) {
        if (isNewScanTick()) {
            this.nearestEntities.clear();
            this.nearestEntities.addAll(visibleNow);
        }
    }

    public void rememberEntity(MemoryCategory cat, BlockPos pos, UUID uuid, MemoryTag... tags) {
        // 1. Ищем, а не помним ли мы уже этого бедолагу?
        Optional<MemoryEntry> existing = storage.get(cat).stream()
                .filter(e -> uuid.equals(e.getEntityId()))
                .findFirst();

        if (existing.isPresent()) {
            // Обновляем старую память (освежаем знания)
            MemoryEntry entry = existing.get();
            entry.setPosition(pos);
            entry.setWhen(npc.level().getGameTime());
            entry.setExpires(npc.level().getGameTime() + 600); // Продлеваем жизнь памяти
        } else {
            // Создаем новую запись, если раньше не видели
            MemoryEntry newEntry = new MemoryEntry(cat, EnumSet.of(MemoryTag.ENTITY, tags), 70, npc.level().getGameTime(), -1, pos, uuid);
            remember(newEntry);
        }
    }
    public void rememberBlock(MemoryCategory category, BlockPos pos, MemoryTag... tags) {
        MemoryEntry existing = findEntry(category, null, pos);
        long now = npc.level().getGameTime();

        if (existing == null) {
            EnumSet<MemoryTag> tagSet = EnumSet.noneOf(MemoryTag.class);
            for (MemoryTag t : tags) tagSet.add(t);

            // Важно: если сундук пустой, ставим низкую важность, чтобы он быстрее вылетел из памяти
            int importance = tagSet.contains(MemoryTag.EMPTY) ? 10 : 50;

            this.remember(new MemoryEntry(category, tagSet, importance, now, -1, pos.immutable(), null));
        } else {
            existing.setWhen(now);
            for (MemoryTag t : tags) {
                // Если нашли еду, убираем тег "пусто"
                if (t == MemoryTag.FOOD) existing.removeTag(MemoryTag.EMPTY);
                existing.addTags(t);
            }
        }
    }
    private @Nullable MemoryEntry findEntry(MemoryCategory category, @Nullable UUID uuid, @Nullable BlockPos pos) {
        List<MemoryEntry> entries = storage.get(category);
        for (MemoryEntry e : entries) {
            if (uuid != null && uuid.equals(e.getEntityId())) return e;
            if (pos != null && pos.equals(e.getPosition())) return e;
        }
        // Не забываем проверить L2 (Persistent)
        for (MemoryEntry e : persistentFacts.values()) {
            if (e.getCategory() == category) {
                if (uuid != null && uuid.equals(e.getEntityId())) return e;
                if (pos != null && pos.equals(e.getPosition())) return e;
            }
        }
        return null;
    }

    public void remember(MemoryEntry entry) {
        if (entry.isPersistent() || entry.getImportance() >= 90) {
            String key = entry.getCategory().name() + "_" + (entry.getEntityId() != null ? entry.getEntityId() : entry.getPosition());
            persistentFacts.put(key, entry);
            return;
        }

        List<MemoryEntry> entries = storage.get(entry.getCategory());
        entries.removeIf(e -> isSameTarget(e, entry));
        entries.add(entry);

        if (entries.size() > MAX_ENTRIES_PER_CAT) {
            entries.sort(Comparator.comparingInt(MemoryEntry::getImportance));
            entries.removeFirst();
        }
    }
    private boolean isSameTarget(MemoryEntry a, MemoryEntry b) {
        if (a.getEntityId() != null && b.getEntityId() != null)
            return a.getEntityId().equals(b.getEntityId());

        if (a.getPosition() != null && b.getPosition() != null) {
            return a.getPosition().equals(b.getPosition());
        }
        return false;
    }

    public boolean hasThreats(){
        return true;
    }

    public Optional<MemoryEntry> getBest(MemoryCategory category) {
        return getBest(category, null, null);
    }

    public Optional<MemoryEntry> getBest(MemoryCategory category, MemoryTag... tags) {
        return getBest(category, null, tags);
    }

    public Optional<MemoryEntry> getBest(MemoryCategory category, BlockPos pos) {
        return getBest(category, pos, null);
    }

    public Optional<MemoryEntry> getBest(MemoryCategory category, @Nullable BlockPos currentPos, MemoryTag... requiredTags) {
        List<MemoryEntry> entries = storage.get(category);
        if (entries.isEmpty()) return Optional.empty();

        MemoryEntry best = null;
        double minWeight = Double.MAX_VALUE;
        long timeNow = npc.level().getGameTime();

        for (MemoryEntry e : entries) {
            if (requiredTags.length > 0 && !e.hasAllTags(requiredTags)) continue;

            double distSqr = (currentPos != null && e.getPosition() != null) ? e.getPosition().distSqr(currentPos) : 0;
            long age = timeNow - e.getWhen();

            // Формула: чем меньше результат, тем "лучше" воспоминание
            double weight = (distSqr / (e.getImportance() + 1)) + (age / 100.0);

            if (weight < minWeight) {
                minWeight = weight;
                best = e;
            }
        }
        return Optional.ofNullable(best);
    }
    public void forgetAt(MemoryCategory category, BlockPos pos) {
        if (pos == null) return;
        storage.get(category).removeIf(e -> pos.equals(e.getPosition()));
    }

    public List<MemoryEntry> getAllByCategory(MemoryCategory category){
        return storage.get(category);
    }
    public void tick(long currentTick) {
        for (List<MemoryEntry> list : storage.values()) {
            list.removeIf(e -> e.isExpired(currentTick));
        }
        // L0 выметаем только если сенсоры долго не работали (страховка)
        if ((currentTick - lastScanTick) > 100) {
            nearestEntities.clear();
        }
    }
    public void invalidate(MemoryCategory category, BlockPos pos) {
        // Удаляем из рабочей памяти
        storage.get(category).removeIf(e -> pos.equals(e.getPosition()));
        // Удаляем из постоянной
        persistentFacts.entrySet().removeIf(entry -> pos.equals(entry.getValue().getPosition()));
    }
    public void save(ValueOutput out) {
        // 1. Сохраняем L1 (Transient)
        for (MemoryCategory cat : MemoryCategory.values()) {
            List<MemoryEntry> entries = storage.get(cat);
            if (entries.isEmpty()) continue; // Пропускаем пустые категории

            out.putInt(cat.getKey() + "_size", entries.size());
            for (int i = 0; i < entries.size(); i++) {
                saveEntry(out, entries.get(i), cat.getKey() + "_" + i + "_");
            }
        }

        // 2. Сохраняем L2 (Persistent) — КРИТИЧНО!
        out.putInt("persistent_size", persistentFacts.size());
        int idx = 0;
        for (MemoryEntry e : persistentFacts.values()) {
            saveEntry(out, e, "pers_" + idx++ + "_");
        }
    }

    private void saveEntry(ValueOutput out, MemoryEntry e, String prefix) {
        out.putString(prefix + "cat", e.getCategory().name());
        // Теги через битовую масочку — экономим место
        long maskLow = 0;
        long maskHigh = 0;

        for (MemoryTag t : e.getTags()) {
            int ordinal = t.ordinal();
            if (ordinal < 64) {
                maskLow |= (1L << ordinal);
            } else {
                maskHigh |= (1L << (ordinal - 64));
            }
        }
        out.putLong(prefix + "ml", maskLow);
        out.putLong(prefix + "mh", maskHigh);

        out.putLong(prefix + "w", e.getWhen());
        out.putInt(prefix + "i", e.getImportance());
        out.putLong(prefix + "x", e.getExpires());

        if (e.getEntityId() != null) {
            out.putLong(prefix + "e_id_m", e.getEntityId().getMostSignificantBits());
            out.putLong(prefix + "e_id_l", e.getEntityId().getLeastSignificantBits());
        } else {
            out.putLong(prefix + "e_id_m", -1L);
        }
    }
    public void read(ValueInput in) {
        // 1. Чистим текущее состояние
        for (List<MemoryEntry> entries : storage.values()) entries.clear();
        persistentFacts.clear();

        // 2. Читаем L1 (Transient)
        for (MemoryCategory cat : MemoryCategory.values()) {
            int size = in.getIntOr(cat.getKey() + "_size", 0);
            for (int i = 0; i < size; i++) {
                MemoryEntry entry = readEntry(in, cat.getKey() + "_" + i + "_", cat);
                if (entry != null) storage.get(cat).add(entry);
            }
        }

        // 3. Читаем L2 (Persistent)
        int pSize = in.getIntOr("persistent_size", 0);
        for (int i = 0; i < pSize; i++) {
            // Категорию для L2 тоже нужно сохранить или прочитать
            String catName = in.getStringOr("pers_" + i + "_cat", "WORLD");
            MemoryCategory cat = MemoryCategory.valueOf(catName);

            MemoryEntry entry = readEntry(in, "pers_" + i + "_", cat);
            if (entry != null) {
                String key = cat.name() + "_" + (entry.getEntityId() != null ? entry.getEntityId() : entry.getPosition());
                persistentFacts.put(key, entry);
            }
        }
    }

    private @Nullable MemoryEntry readEntry(ValueInput in, String prefix, MemoryCategory cat) {
        // Читаем маски тегов
        long ml = in.getLongOr(prefix + "ml", 0L);
        long mh = in.getLongOr(prefix + "mh", 0L);

        EnumSet<MemoryTag> tags = EnumSet.noneOf(MemoryTag.class);
        for (MemoryTag t : MemoryTag.values()) {
            int ord = t.ordinal();
            if (ord < 64) {
                if ((ml & (1L << ord)) != 0) tags.add(t);
            } else {
                if ((mh & (1L << (ord - 64))) != 0) tags.add(t);
            }
        }

        // Позиция
        long posLong = in.getLongOr(prefix + "p", -1L);
        BlockPos pos = posLong != -1L ? BlockPos.of(posLong) : null;

        // Данные
        long time = in.getLongOr(prefix + "w", 0L);
        int imp = in.getIntOr(prefix + "i", 0);
        long exp = in.getLongOr(prefix + "x", -1L);

        // UUID
        long mBits = in.getLongOr(prefix + "e_id_m", -1L);
        UUID e_id = null;
        if (mBits != -1L) {
            long lBits = in.getLongOr(prefix + "e_id_l", 0L);
            e_id = new UUID(mBits, lBits);
        }

        return new MemoryEntry(cat, tags, imp, time, exp, pos, e_id);
    }
    private @Nullable MemoryCategory safeGetCategory(ValueInput in, String key) {
        String name = in.getStringOr(key, "ENTITIES");
        try {
            return MemoryCategory.valueOf(name);
        } catch (IllegalArgumentException e) {
            Foreverlive.LOGGER.warn("Memory category not found. Key: {}", key);
            return null;
        }
    }
}