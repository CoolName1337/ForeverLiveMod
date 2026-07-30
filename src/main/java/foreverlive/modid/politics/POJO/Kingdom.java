package foreverlive.modid.politics.POJO;

import foreverlive.modid.politics.settlement.Settlement;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class Kingdom {
    private final UUID id = UUID.randomUUID();
    private String name;
    public List<Settlement> settlements = new ArrayList<>();

    private final Map<UUID, Integer> diplomacy = new HashMap<>();

    public void tickEconomy(ServerLevel world){
        for(Settlement s : settlements){
            s.tick(world);
        }
    }
}
