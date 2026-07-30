package foreverlive.modid.politics.settlement.plot.building.palette;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import java.util.EnumMap;
import java.util.Map;

public class BuildingPalette {

    public enum PaletteKey {
        FOUNDATION,    // Фундамент (Булыжник, Глубинный сланец)
        FRAME,         // Несущий каркас (Бревна, Обтесанные бревна)
        WALL_PRIMARY,  // Основной материал стен (Доски, Трамбованная глина)
        WALL_SECONDARY,// Акцентный материал стен
        ROOF_EDGE,     // Край крыши (Ступени/плиты из камня)
        ROOF_SURFACE,  // Полотно крыши (Дубовые ступени, Тёмный дуб)
        FLOOR,         // Пол внутренних помещений
        GLASS,         // Стекло/решетки
        LIGHT          // Источники света (Фонари, Факелы)
    }

    private final Map<PaletteKey, BlockState> mapping = new EnumMap<>(PaletteKey.class);

    public BuildingPalette put(PaletteKey key, BlockState state) {
        mapping.put(key, state);
        return this;
    }

    public BlockState get(PaletteKey key) {
        return mapping.getOrDefault(key, Blocks.OAK_PLANKS.defaultBlockState());
    }
}