package foreverlive.modid.config;

import foreverlive.modid.npc.components.proccessing.NeedType;

import java.util.EnumMap;
import java.util.Map;

public class ModConfig {
    public Map<NeedType, NeedSettings> needs = new EnumMap<>(NeedType.class);

    public static class NeedSettings{
        public float priority;
        public float threshold;
        public float decay;

        public NeedSettings(float priority, float threshold, float decay) {
            this.priority = priority;
            this.threshold = threshold;
            this.decay = decay;
        }
    }

    public static ModConfig createDefault() {
        ModConfig config = new ModConfig();
        config.needs.put(NeedType.THIRST, new NeedSettings(1, 40, 1));
        config.needs.put(NeedType.HUNGER, new NeedSettings(2, 30, 1));
        config.needs.put(NeedType.SLEEP, new NeedSettings(3, 10, 1));
        config.needs.put(NeedType.SAFETY, new NeedSettings(10.0f, 0, 0));

        config.needs.put(NeedType.IDLE, new NeedSettings(5f, 0, 1));
        return config;
    }
}
