package foreverlive.modid.politics.builder.interfaces;

import foreverlive.modid.politics.builder.SettlementStyle;
import foreverlive.modid.politics.builder.WorldBuildQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface IWallGenerator {
    void buildWallSegment(WorldBuildQueue queue, BlockPos pos, SettlementStyle style);
    void buildGate(WorldBuildQueue queue, BlockPos gatePos, Direction direction, SettlementStyle style);
}