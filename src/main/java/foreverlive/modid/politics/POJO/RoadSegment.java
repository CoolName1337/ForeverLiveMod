package foreverlive.modid.politics.POJO;

import net.minecraft.core.BlockPos;

import java.util.List;
public class RoadSegment {
    public enum RoadType { SPOKE, RING, ALLEY }

    private final List<BlockPos> points;
    private final int width;
    private final RoadType type;

    public RoadSegment(List<BlockPos> points, int width, RoadType type) {
        this.points = points;
        this.width = width;
        this.type = type;
    }

    public List<BlockPos> getPoints() { return points; }
    public int getWidth() { return width; }
    public RoadType getType() { return type; }
}