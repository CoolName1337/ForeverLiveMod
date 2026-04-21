package foreverlive.modid.npc.logic;

import foreverlive.modid.npc.components.proccessing.NeedType;
import foreverlive.modid.entities.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class NpcUtils {
    public static BlockPos findSmart(
            Entity entity,
            int radius,
            Predicate<BlockState> lightCondition,
            BiPredicate<Level, BlockPos> deepCondition){

        BlockPos pos = entity.blockPosition();
        Level level = entity.level();

        for(BlockPos targetPos :
                BlockPos.betweenClosed(pos.offset(-radius,-2, -radius),
                        pos.offset(radius, 2, radius))){
            BlockState state = level.getBlockState(targetPos);
            // At first light check
            if (lightCondition.test(state)) {
                // At second a much harder check
                if (deepCondition.test(entity.level(), targetPos)) {
                    return targetPos.immutable();
                }
            }
        }
        return null;
    }

    public static BlockPos findSmart(
            Entity entity,
            int radius,
            Predicate<BlockState> lightCondition){
        return findSmart(entity, radius, lightCondition,
                (level, blockPos) -> true);
    }

    public static BlockPos findSmart(
            Entity entity,
            int radius,
            Block block) {
        return findSmart(
                entity, radius,
                blockState -> blockState.getBlock() == block,
                (level, blockPos) -> true);
    }


    public static boolean tryConsumeItemFromContainer(Level level, BlockPos blockPos, Item item){
        if(level.getBlockEntity(blockPos) instanceof Container container){
            for (int i = 0; i < container.getContainerSize(); i++){
                ItemStack stack = container.getItem(i);
                if(stack.is(item)){
                    stack.shrink(1);
                    container.setChanged();

                    level.playSound(null,
                            blockPos,
                            SoundEvents.CHEST_OPEN,
                            SoundSource.BLOCKS,
                            0.5f,
                            1f);

                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasItem(Level level, BlockPos pos, Item item) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);

                if (!stack.isEmpty() && stack.is(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryEatFromInventory(NpcEntity npc) {
        Container inv = npc.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                consumeFoodStack(npc, stack);
                return true;
            }
        }
        return false;
    }

    public static boolean hasDrinkInInventory(NpcEntity npc) {
        Container inv = npc.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isLiquid(inv.getItem(i))) return true;
        }
        return false;
    }

    public static void consumeDrinkFromInventory(NpcEntity npc) {
        Container inv = npc.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isLiquid(stack)) {
                stack.shrink(1);
                // Можно вернуть пустую бутылку
                return;
            }
        }
    }

    public static boolean isLiquid(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(net.minecraft.world.item.Items.POTION) || stack.has(DataComponents.POTION_CONTENTS));
    }
    public static void consumeFoodStack(NpcEntity npc, ItemStack stack) {
        var foodComp = stack.get(DataComponents.FOOD);
        float nutrition = (foodComp != null) ? foodComp.nutrition() : 10;
        float currentVal = npc.getNeedValue(NeedType.HUNGER);

        npc.setNeedValue(NeedType.HUNGER, Math.min(100f, currentVal + (nutrition * 10f)));
        stack.shrink(1);

        // Эффекты
        npc.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 0.5f, 1.0f);
    }

    public static void consumeDrinkStack(NpcEntity npc, ItemStack stack) {
        float currentVal = npc.getNeedValue(NeedType.THIRST);

        npc.setNeedValue(NeedType.THIRST, Math.min(100f, currentVal + (5 * 10f)));
        stack.shrink(1);

        // Эффекты
        npc.playSound(SoundEvents.GENERIC_DRINK.value(), 0.5f, 1.0f);
    }
    public static boolean tryDrinkFromStorage(NpcEntity npc, BlockPos pos) {
        if (!(npc.level().getBlockEntity(pos) instanceof net.minecraft.world.Container container)) return false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && isLiquid(stack)) {
                consumeDrinkStack(npc,stack);
                return true;
            }
        }
        return false;
    }
    public static boolean tryEatFromStorage(NpcEntity npc, BlockPos pos) {
        if (!(npc.level().getBlockEntity(pos) instanceof net.minecraft.world.Container container)) return false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                consumeFoodStack(npc,stack);
                return true;
            }
        }
        return false;
    }

    public static BlockPos findQuietSpot(NpcEntity npc) {
        BlockPos currentPos = npc.blockPosition();
        Level level = npc.level();
        RandomSource random = npc.getRandom();

        BlockPos bestSpot = currentPos;
        int bestScore = -1;

        // Делаем всего 15 попыток. Этого достаточно, чтобы найти угол.
        for (int i = 0; i < 15; i++) {
            // Ищем точку в радиусе 10 блоков
            int x = currentPos.getX() + random.nextInt(21) - 10;
            int z = currentPos.getZ() + random.nextInt(21) - 10;

            // Находим поверхность (Y).
            // В идеале юзать npc.level().getHeightmapPos, это быстрее всего.
            BlockPos candidate = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));

            // 1. Простая проверка: можно ли там стоять/лежать?
            if (!level.getBlockState(candidate.below()).isSolid() || !level.isEmptyBlock(candidate)) {
                continue;
            }

            int score = 0;

            // 2. Критерий "Темнота" (чем темнее, тем лучше для сна)
            // getBrightness возвращает свет от неба и блоков.
            int light = level.getBrightness(LightLayer.BLOCK, candidate) + level.getBrightness(LightLayer.SKY, candidate);
            score += (15 - light);

            // 3. Критерий "Укромность" (проверяем, есть ли рядом стены)
            // Вместо проверки всех сторон, проверяем только 2 случайные.
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (!level.isEmptyBlock(candidate.relative(dir))) {
                    score += 5; // Стенка рядом — это уютно
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestSpot = candidate;
            }
        }

        return bestSpot;
    }

    public static boolean validateSleepingSpot(NpcEntity npc, BlockPos spot){
        int light = npc.level().getBrightness(LightLayer.BLOCK, spot);
        if (light > 7) return false;

        boolean occupied = !npc.level().getEntitiesOfClass(NpcEntity.class,
                new AABB(spot).inflate(0.5),
                e -> e != npc && e.getPose() == Pose.SLEEPING).isEmpty();

        if (occupied) return false;

        return npc.level().getBlockState(spot.below()).isSolid();
    }


    public static float calculatePower(LivingEntity entity) {
        float power = entity.getHealth();
        // Учитываем броню: каждый поинт брони — это +5% к "выживаемости" в глазах Григория
        power += entity.getArmorValue() * 1.5f;

        // Если у врага меч (особенно зачарованный) — это серьезно
        if (entity.getMainHandItem().isDamageableItem()) {
            power += 10f;
        }
        return power;
    }
    public static double getAttackDamage(LivingEntity entity) {
        // Проверяем, есть ли такой атрибут вообще
        if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            return entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }
        // Если атрибута нет (например, это овечка или мирный NPC), возвращаем 0 или 1 (удар рукой)
        return 1.0;
    }
}