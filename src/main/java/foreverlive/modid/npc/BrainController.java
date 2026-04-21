package foreverlive.modid.npc;

import net.minecraft.world.entity.Entity;

public class BrainController {
    public static boolean isPhase(Entity entity, int interval) {
        return (entity.level().getGameTime() + entity.getId()) % interval == 0;
    }
}