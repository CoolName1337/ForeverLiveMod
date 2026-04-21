package foreverlive.modid.npc.components.memory;

public enum MemoryCategory {
    BLOCKS, // Сундуки, бочки
    ENTITIES, // Всякая живность (коровы, монстры)
    SOCIAL,   // Игроки, NPC
    WORLD,    // Дом, магазин, церковь
    REST,     // Кровати
    DROP,     // Предметы на земле (наша еда!)
    STATIONS; // Рабочие места (ферма, кузница)

    public String getKey() { return "cat_"+this.name().toLowerCase(); }
}
