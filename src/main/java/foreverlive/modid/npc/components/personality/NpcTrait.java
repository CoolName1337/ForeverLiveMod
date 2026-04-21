package foreverlive.modid.npc.components.personality;

public enum NpcTrait {
    GLUTTON,
    ASCETIC,
    BRAVE,
    COWARD;

    public NpcTrait[] getConflicts() {
        if (this == GLUTTON) return new NpcTrait[]{ASCETIC};
        if (this == BRAVE) return new NpcTrait[]{COWARD};
        return new NpcTrait[0];
    }
}
