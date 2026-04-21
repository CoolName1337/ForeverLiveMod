package foreverlive.modid.client;

import foreverlive.modid.Foreverlive;
import foreverlive.modid.npc.components.proccessing.NeedType;
import foreverlive.modid.entities.NpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, NpcRenderState, HumanoidModel<NpcRenderState>> {

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }
    @Override
    public Component getNameTag(NpcEntity entity) {
        // 1. Берем базовое имя (Григорий)
        MutableComponent tag = Component.literal(entity.getNpcName()).withStyle(styleWithColor("yellow"));

        if (entity.brain == null) return tag;

        // 2. Получаем данные
        String goal = entity.brain.getCurrentGoal() != null ? entity.brain.getCurrentGoal().name() : "Проеб";
        int thirst = (int) entity.getNeedValue(NeedType.THIRST);
        int hunger = (int) entity.getNeedValue(NeedType.HUNGER);

        // Так как \n не работает, сделаем всё в одну красивую строку с разделителями
        // Или используем разные цвета для читаемости
        tag.append(Component.literal(" | ").withStyle(styleWithColor("dark_gray")))
                .append(Component.literal(goal).withStyle(styleWithColor("gray")))
                .append(Component.literal(" | ").withStyle(styleWithColor("dark_gray")));

        // Полоска жажды (синяя)
        tag.append(renderBar(thirst, styleWithColor("blue")))
                .append(Component.literal(" "));

        // Полоска голода (оранжевая/красная)
        tag.append(renderBar(hunger, styleWithColor("gold")));

        return tag;
    }

    private Style styleWithColor(String colorName){
        var resColor = TextColor.parseColor(colorName);
        if(resColor.isSuccess()){
            return Style.EMPTY.withColor(resColor.getOrThrow());
        }
        else{
            return Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        }
    }

    // Вспомогательный метод для рисования полосок без "коробок"
    private MutableComponent renderBar(int value, Style style) {
        int bars = Math.clamp(value / 10, 0, 10);
        return Component.literal("[").withStyle(style)
                .append(Component.literal("|".repeat(bars)).withStyle(style))
                .append(Component.literal("|".repeat(10 - bars)).withStyle(styleWithColor("dark_gray")))
                .append(Component.literal("]").withStyle(style));
    }
    @Override
    public @NonNull NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(@NonNull NpcEntity entity, @NonNull NpcRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.name = entity.getNpcName() != null ? entity.getNpcName() : "Waiting...";
        state.skinIndex = entity.getSkinIndex();

        if (entity.brain.getCurrentGoal() != null) {
            state.currentGoal = entity.brain.getCurrentGoal();
        }

        state.thirst = entity.getNeedValue(NeedType.THIRST);
        state.hunger = entity.getNeedValue(NeedType.HUNGER);
    }

    @Override
    public @NonNull Identifier getTextureLocation(NpcRenderState state) {
        return Identifier.fromNamespaceAndPath(Foreverlive.MOD_ID, "textures/entity/npc/skin_" + state.skinIndex + ".png");
    }
}