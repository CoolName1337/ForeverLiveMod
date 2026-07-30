package foreverlive.modid.politics.settlement.plot.building.planning;

import foreverlive.modid.politics.settlement.enums.PlotType;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

public class FloorPlanGenerator {

    public record RoomNode(
            BlockPos min,
            BlockPos max,
            RoomType type,
            int floorLevel
    ) {}

    public enum RoomType {
        MAIN_HALL, BEDROOM, KITCHEN, WORKSHOP, STORAGE, CORRIDOR, BALCONY
    }

    public static List<RoomNode> generateLayout(PlotType plotType, int tier, BlockPos min, BlockPos max) {
        List<RoomNode> rooms = new ArrayList<>();
        int width = max.getX() - min.getX() + 1;
        int depth = max.getZ() - min.getZ() + 1;
        int maxFloors = getMaxFloorsForTier(tier, plotType);

        for (int floor = 0; floor < maxFloors; floor++) {
            int yOffset = floor * 5; // Высота этажа 5 блоков (4 жилых + 1 перекрытие)
            BlockPos floorMin = min.above(yOffset);
            BlockPos floorMax = new BlockPos(max.getX(), min.getY() + yOffset + 4, max.getZ());

            if (floor == 0) {
                // Первый этаж: Главный зал + служебное помещение
                if (width >= 8 && depth >= 8) {
                    // Делим пополам
                    int splitZ = floorMin.getZ() + depth / 2;
                    rooms.add(new RoomNode(floorMin, new BlockPos(floorMax.getX(), floorMin.getY() + 4, splitZ), RoomType.MAIN_HALL, 0));
                    rooms.add(new RoomNode(new BlockPos(floorMin.getX(), floorMin.getY(), splitZ + 1), floorMax, RoomType.STORAGE, 0));
                } else {
                    rooms.add(new RoomNode(floorMin, floorMax, RoomType.MAIN_HALL, 0));
                }
            } else {
                // Второй этаж: Спальни
                rooms.add(new RoomNode(floorMin, floorMax, RoomType.BEDROOM, floor));
            }
        }

        return rooms;
    }

    private static int getMaxFloorsForTier(int tier, PlotType type) {
        if (type == PlotType.TOWN_HALL) return Math.min(3, tier);
        if (tier == 1) return 1;
        if (tier == 2) return 2;
        return 3;
    }
}