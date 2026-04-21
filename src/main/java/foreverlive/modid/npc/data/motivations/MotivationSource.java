package foreverlive.modid.npc.data.motivations;

import foreverlive.modid.entities.NpcEntity;

public interface MotivationSource {
    /** @return Насколько сильно это "желание" сейчас (0.0 - 150.0) */
    float getScore(NpcEntity npc);
}