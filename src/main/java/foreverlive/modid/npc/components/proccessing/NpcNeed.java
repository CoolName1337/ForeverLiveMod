package foreverlive.modid.npc.components.proccessing;

public class NpcNeed {
    public NeedType name;
    public float value;
    public float priority;
    public float threshold;
    public float decay;

    public NpcNeed(NeedType type, float priority, float  threshold, float decay){
        this.name = type;
        this.priority = priority;
        this.threshold = threshold;
        this.decay = decay;
        this.value = 100;
    }

    public float getScore(){
        var deficit = 100 - value;
        if(deficit < threshold)
            return 0;

        return (deficit) * priority;
    }
}
