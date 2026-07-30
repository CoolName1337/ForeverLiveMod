package foreverlive.modid.politics.generators;

import foreverlive.modid.politics.POJO.enums.ZoneType;
import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.settlement.enums.PlotType;

import java.util.Map;

public class PlotTypeResolver {

    /**
     * Определяет тип участка на основе текущих дефицитов города и зонирования
     */
    public static PlotType resolveType(Settlement settlement, ZoneType zone) {
        Map<PlotType, Integer> existingCounts = settlement.getPlotCountsByType();

        // 1. КРИТИЧЕСКИЙ ПРИОРИТЕТ: Ратуша (Town Hall) всегда первая в административной зоне
        if (existingCounts.getOrDefault(PlotType.TOWN_HALL, 0) == 0 && zone == ZoneType.CIVIC) {
            return PlotType.TOWN_HALL;
        }

        // 2. ЕДА: Если жителей больше, чем фермы могут прокормить (из расчета 4 человека на ферму)
        int residents = settlement.getResidentCount();
        int farms = existingCounts.getOrDefault(PlotType.FARM, 0);
        if (farms == 0 || (residents / Math.max(1, farms)) > 4) {
            if (zone == ZoneType.INDUSTRIAL || zone == ZoneType.RESIDENTIAL) {
                return PlotType.FARM;
            }
        }

        // 3. ЖИЛЬЕ: Если не хватает мест (по 3 жителя на 1 дом)
        int housingCapacity = existingCounts.getOrDefault(PlotType.RESIDENTIAL, 0) * 3;
        if (residents >= housingCapacity && (zone == ZoneType.RESIDENTIAL || zone == ZoneType.COMMERCIAL)) {
            return PlotType.RESIDENTIAL;
        }

        // 4. РАСПРЕДЕЛЕНИЕ ПО ЗОНАМ
        if (zone == null) {
            return PlotType.RESIDENTIAL;
        }

        switch (zone) {
            case CIVIC:
                if (existingCounts.getOrDefault(PlotType.TAVERN, 0) == 0) return PlotType.TAVERN;
                if (existingCounts.getOrDefault(PlotType.GUARD_HOUSE, 0) == 0) return PlotType.GUARD_HOUSE;
                return PlotType.RESIDENTIAL;

            case COMMERCIAL:
                if (existingCounts.getOrDefault(PlotType.FORGE, 0) == 0) return PlotType.FORGE;
                if (existingCounts.getOrDefault(PlotType.TAVERN, 0) == 0) return PlotType.TAVERN;
                return PlotType.RESIDENTIAL;

            case INDUSTRIAL:
                if (existingCounts.getOrDefault(PlotType.FORGE, 0) == 0) return PlotType.FORGE;
                return PlotType.FARM;

            case RESIDENTIAL:
            default:
                return PlotType.RESIDENTIAL;
        }
    }
}