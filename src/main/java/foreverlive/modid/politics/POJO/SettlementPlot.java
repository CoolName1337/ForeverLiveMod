package foreverlive.modid.politics.POJO;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class SettlementPlot {
    private final String plotId;
    private final BoundingBox bounds; // Координаты участка в мире
    private final Direction facing;    // Направление фасада к дороге

    private ZoneType zoneType;
    private PlotStatus status;

    // Социальный слой
    private BuildingProperty property;

    public SettlementPlot(String plotId, BoundingBox bounds, Direction facing) {
        this.plotId = plotId;
        this.bounds = bounds;
        this.facing = facing;
    }

    public enum ZoneType {
        CIVIC,        // Ратуша, Собор, Площадь
        COMMERCIAL,   // Рынок, Таверна, Лавки
        RESIDENTIAL,  // Жилые дома
        INDUSTRIAL,   // Кузни, Пекарни, Кожевни
        AGRICULTURAL  // Поля, Загоны
    }

    public enum PlotStatus {
        EMPTY,                // Свободная земля
        RESERVED_FOR_BUILD,   // Зарезервировано под постройку
        CONSTRUCTING,         // Идет стройка жителями
        OCCUPIED,             // Построено и заселено
        ABANDONED             // Заброшено / Разрушено
    }

    // Логика проверки: вместится ли здесь еще один жилец или работник
    public boolean canAcceptNewUser() {
        return status == PlotStatus.OCCUPIED && property != null && !property.isFull();
    }
}
