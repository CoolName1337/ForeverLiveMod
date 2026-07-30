package foreverlive.modid.politics.settlement.plot.layout;

import java.util.ArrayList;
import java.util.List;

public class FloorLayout {
    private final int floorIndex;
    private final FloorConfig config;
    private final List<PlacedElement> elements = new ArrayList<>();

    public FloorLayout(int floorIndex, FloorConfig config) {
        this.floorIndex = floorIndex;
        this.config = config;
    }

    public void add(PlacedElement element) {
        this.elements.add(element);
    }

    public int getFloorIndex() { return floorIndex; }
    public FloorConfig getConfig() { return config; }
    public List<PlacedElement> getElements() { return elements; }
}