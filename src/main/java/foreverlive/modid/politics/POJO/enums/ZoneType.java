package foreverlive.modid.politics.POJO.enums;

public enum ZoneType {
    CIVIC(12, 14),        // Крупные общественные здания
    COMMERCIAL(10, 12),   // Средние магазины/таверны
    RESIDENTIAL(8, 10),   // Жилые дома
    INDUSTRIAL(10, 10);   // Мастерские и амбары

    private final int defaultWidth;
    private final int defaultDepth;

    ZoneType(int defaultWidth, int defaultDepth) {
        this.defaultWidth = defaultWidth;
        this.defaultDepth = defaultDepth;
    }

    public int getDefaultWidth() { return defaultWidth; }
    public int getDefaultDepth() { return defaultDepth; }
}