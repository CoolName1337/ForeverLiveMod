package foreverlive.modid.politics.POJO;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuildingProperty {

    public enum PropertyType {
        HOUSING,     // Жилье
        WORKPLACE,   // Работа / Производство
        PUBLIC       // Общественное место
    }

    private final String plotId;
    private final PropertyType type;
    private UUID owner;                 // Хозяин (NPC или null если городское)
    private final Set<UUID> users = new HashSet<>(); // Жильцы / Работники
    private int capacity;               // Вместимость (число коек или рабочих мест)

    public BuildingProperty(String plotId, PropertyType type, int capacity) {
        this.plotId = plotId;
        this.type = type;
        this.capacity = capacity;
    }

    public boolean assignOwner(UUID npcId) {
        this.owner = npcId;
        return addUser(npcId); // Хозяин автоматически становится пользователем
    }

    public boolean addUser(UUID npcId) {
        if (users.size() < capacity) {
            users.add(npcId);
            return true;
        }
        return false; // Нет свободных мест!
    }

    public void removeUser(UUID npcId) {
        users.remove(npcId);
        if (npcId.equals(owner)) {
            this.owner = users.stream().findFirst().orElse(null); // Передаем права следующему
        }
    }

    public boolean isFull() { return users.size() >= capacity; }
    public UUID getOwner() { return owner; }
    public Set<UUID> getUsers() { return users; }
    public PropertyType getType() { return type; }
}