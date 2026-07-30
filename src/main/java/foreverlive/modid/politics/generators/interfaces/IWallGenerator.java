package foreverlive.modid.politics.generators.interfaces;

import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.services.WorldBuildQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface IWallGenerator {
    void buildWallSegment(WorldBuildQueue queue, BlockPos pos, SettlementStyle style);
    void buildGate(WorldBuildQueue queue, BlockPos gatePos, Direction direction, SettlementStyle style);
}