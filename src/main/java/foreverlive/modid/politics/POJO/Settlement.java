package foreverlive.modid.politics.POJO;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class Settlement {

    private String name;
    public BlockPos origin;
    private int tier;

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    private int radius = 30;
    private int loyalty = 100;

    private final List<RoadSegment> roads = new ArrayList<>();
    private final List<BuildingPlot> plots = new ArrayList<>();

    public List<RoadSegment> getRoads() { return roads; }
    public List<BuildingPlot> getPlots() { return plots; }

    private int population = 10;
    private int wealth = 30;

    public void tick(ServerLevel world){

    }
}
