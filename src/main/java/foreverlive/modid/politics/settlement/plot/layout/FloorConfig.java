package foreverlive.modid.politics.settlement.plot.layout;

public record FloorConfig(
        int height,             // Высота конкретно этого этажа
        String styleFamily,     // Например: "residential", "forge", "castle"
        String material,        // Например: "cobblestone", "oak_wood", "deepslate"
        boolean allowDoors,     // Можно ли ставить двери на этом этаже
        int overhangRadius      // Вынос/нависание (0 = вровень, 1 = нависает на 1 блок)
) {}