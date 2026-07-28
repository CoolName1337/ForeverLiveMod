package foreverlive.modid.politics.POJO;

import foreverlive.modid.politics.POJO.enums.PlotType;
import net.minecraft.core.BlockPos;

public class BuildingPlot {

    private final BlockPos center;
    private final int sizeX;
    private final int sizeZ;
    private PlotType type;

    public BuildingPlot(BlockPos center, int sizeX, int sizeZ, PlotType type){
        this.center = center;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.type = type;
    }

    public int getSizeX() {
        return sizeX;
    }
    public int getSizeZ() {
        return sizeZ;
    }
    public BlockPos getCenter() {
        return center;
    }
    public PlotType getType() {
        return type;
    }
}
