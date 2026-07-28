package foreverlive.modid.politics.POJO;

import foreverlive.modid.politics.POJO.enums.ZoneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class Plot {
    private final BlockPos minPos;
    private final BlockPos maxPos;
    private final BlockPos doorPos;
    private final Direction facing;
    private final ZoneType zoneType;

    public Plot(BlockPos minPos, BlockPos maxPos, BlockPos doorPos, Direction facing, ZoneType zoneType) {
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.doorPos = doorPos;
        this.facing = facing;
        this.zoneType = zoneType;
    }

    /**
     * Проверка на пересечение с другим участком (AABB)
     */
    public boolean intersects(Plot other) {
        return minPos.getX() <= other.maxPos.getX() && maxPos.getX() >= other.minPos.getX() &&
                minPos.getZ() <= other.maxPos.getZ() && maxPos.getZ() >= other.minPos.getZ();
    }

    /**
     * Проверка: попадает ли точка внутрь участка
     */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX() &&
                pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
    }

    public BlockPos getMinPos() { return minPos; }
    public BlockPos getMaxPos() { return maxPos; }
    public BlockPos getDoorPos() { return doorPos; }
    public Direction getFacing() { return facing; }
    public ZoneType getZoneType() { return zoneType; }
}