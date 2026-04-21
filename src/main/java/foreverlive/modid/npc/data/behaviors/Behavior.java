package foreverlive.modid.npc.data.behaviors;

import foreverlive.modid.entities.NpcEntity;

public interface Behavior {
    void onStart(NpcEntity npc);
    void onUpdate(NpcEntity npc);

    void onStop(NpcEntity npc);

    boolean isFinished(NpcEntity npc);
}