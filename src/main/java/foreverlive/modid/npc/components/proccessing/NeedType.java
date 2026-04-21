package foreverlive.modid.npc.components.proccessing;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum NeedType{
    // Накапливаемые (Persistent)
    HUNGER(true),
    THIRST(true),
    SLEEP(true),
    POOP(true), // Куда же без этого

    // Мгновенные/Реактивные (Dynamic)
    SAFETY(false),
    SOCIAL(false),
    EXPLORE(false),
    IDLE(false);

    private final boolean persistent;

    NeedType(boolean persistent) {
        this.persistent = persistent;
    }

    public boolean isPersistent() { return persistent; }

    public String getKey() { return "need_" + name().toLowerCase(); }

    public static final StreamCodec<ByteBuf, NeedType> CODEC = ByteBufCodecs.idMapper(
            index -> NeedType.values()[index],
            NeedType::ordinal
    );
}

