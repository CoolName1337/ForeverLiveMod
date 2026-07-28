package foreverlive.modid.politics.POJO;

import net.minecraft.core.BlockPos;

import java.util.List;

public class RoadSegment {
    private final List<BlockPos> points;

    public RoadSegment(List<BlockPos> points){
        this.points = points;
    }

    public List<BlockPos> getPoints() { return points; }
}
