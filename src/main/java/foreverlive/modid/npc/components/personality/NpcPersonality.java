package foreverlive.modid.npc.components.personality;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public class NpcPersonality {
    // Настроение
    public float mood = 50.0f;
    public float getMood(){ return mood; }
    public void setMood(float value) { mood = Math.clamp(value, 0 , 100); }

    // Черты характера
    private final EnumSet<NpcTrait> traits = EnumSet.noneOf(NpcTrait.class);
    public void addTrait(NpcTrait trait) {
        // Валидация конфликтов
        for (NpcTrait conflict : trait.getConflicts()) {
            traits.remove(conflict);
        }
        traits.add(trait);
    }
    public boolean hasTrait(NpcTrait trait) {
        return traits.contains(trait);
    }

    // Основные атрибуты, сила там ловкость и другая фигня
    private final Map<AttrType, Float> attributes = new EnumMap<>(AttrType.class);
    public float getAttr(AttrType type){ return attributes.get(type); }
    public void setAttr(AttrType type, float value){
        attributes.put(type, Math.clamp(value, 0, 20));
    }
    // Скиллы
    private final Map<SkillType, Float> skills = new EnumMap<>(SkillType.class);
    public float getSkill(SkillType type){ return skills.get(type); }
    public void setSkill(SkillType type, float value){
        skills.put(type, Math.clamp(value, 0, 100));
    }

    public void save(ValueOutput out) {
        out.putFloat("m", mood);

        long traitMask = 0;
        for (NpcTrait trait : traits) {
            traitMask |= (1L << trait.ordinal());
        }
        out.putLong("t", traitMask);

        for (int i = 0; i < AttrType.values().length; i++){
            out.putFloat("a_"+i, attributes.getOrDefault(AttrType.values()[i], 0f));
        }

        for (int i = 0; i < SkillType.values().length; i++){
            out.putFloat("s_"+i, skills.getOrDefault(SkillType.values()[i], 0f));
        }
    }
    public void load(ValueInput in) {
        this.mood = in.getFloatOr("m", 50);

        this.traits.clear();
        long traitMask = in.getLongOr("t", 0);
        for (NpcTrait trait : NpcTrait.values()) {
            if ((traitMask & (1L << trait.ordinal())) != 0) {
                traits.add(trait);
            }
        }

        for (int i = 0; i < AttrType.values().length; i++) {
            attributes.put(AttrType.values()[i], in.getFloatOr("a_"+i, 0));
        }

        for (int i = 0; i < SkillType.values().length; i++) {
            skills.put(SkillType.values()[i], in.getFloatOr("s_"+i, 0));
        }
    }
}