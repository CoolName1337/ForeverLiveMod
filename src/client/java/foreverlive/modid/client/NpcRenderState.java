package foreverlive.modid.client;

import foreverlive.modid.npc.components.proccessing.NeedType;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class NpcRenderState extends HumanoidRenderState {
    public String name = "Григорий";
    public int skinIndex = 0;
    public float thirst = 100f;
    public float hunger = 100f;
    public NeedType currentGoal = null;
}
