package foreverlive.modid.npc.logic;

import foreverlive.modid.npc.components.memory.MemoryEntry;
import foreverlive.modid.npc.components.memory.MemoryCategory;
import foreverlive.modid.npc.components.proccessing.NeedType;
import foreverlive.modid.entities.NpcEntity;
import foreverlive.modid.npc.components.memory.MemoryTag;
import foreverlive.modid.npc.data.behaviors.Behavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.*;


public class BehaviorRegistry {
    private static final Map<NeedType, Behavior> BEHAVIORS = new EnumMap<>(NeedType.class);

    static {
        BEHAVIORS.put(NeedType.THIRST, new Behavior() {
            private static final int DRINK_DURATION = 20;
            private BlockPos targetPos;
            private Entity targetItem;
            private int drinkTimer = 0;
            private boolean isInspectingChest = false;

            @Override
            public void onStart(NpcEntity npc) {
                drinkTimer = 0;
                targetPos = null;
                targetItem = null;
                isInspectingChest = false;

                // 1. ИНВЕНТАРЬ
                if (NpcUtils.hasDrinkInInventory(npc)) {
                    drinkTimer = DRINK_DURATION;
                    NpcUtils.consumeDrinkFromInventory(npc);
                    return;
                }

                // 2. ПРЕДМЕТЫ НА ЗЕМЛЕ (L0/L1 память)
                var droppedWater = npc.memory.getBest(MemoryCategory.DROP, MemoryTag.WATER);
                if (droppedWater.isPresent()) {
                    targetItem = npc.level().getEntity(droppedWater.get().getEntityId()); // Более надежный поиск
                    if (targetItem != null && targetItem.isAlive()) {
                        npc.getNavigation().moveTo(targetItem, 1.2f);
                        return;
                    }
                }

                // 3. ПРОВЕРЕННЫЕ ИСТОЧНИКИ (Сундуки с водой или Колодцы)
                var knownSource = npc.memory.getBest(MemoryCategory.BLOCKS, npc.blockPosition(), MemoryTag.WATER);
                if (knownSource.isPresent()) {
                    targetPos = knownSource.get().getPosition();
                } else {
                    // 4. РАЗВЕДКА: Ищем любой сундук, который мы ЕЩЕ НЕ ПРОВЕРЯЛИ на наличие воды
                    var unknownChest = npc.memory.getBest(MemoryCategory.BLOCKS, npc.blockPosition(), MemoryTag.CHEST);
                    if (unknownChest.isPresent() && !unknownChest.get().hasTag(MemoryTag.EMPTY_WATER)) {
                        targetPos = unknownChest.get().getPosition();
                        isInspectingChest = true;
                    }
                }

                if (targetPos != null) {
                    npc.getNavigation().moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, 1.1f);
                } else {
                    // 5. ГУЛЯЕМ И ИЩЕМ (Сенсоры сами подцепят воду, если она появится в радиусе)
                    Vec3 randomPos = DefaultRandomPos.getPos(npc, 15, 7);
                    if (randomPos != null) npc.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, 1.0f);
                }
            }

            @Override
            public void onUpdate(NpcEntity npc) {
                // Процесс питья (уже есть бутылка или стоим у воды)
                if (drinkTimer > 0) {
                    npc.getNavigation().stop();
                    if (--drinkTimer % 10 == 0) npc.playSound(SoundEvents.GENERIC_DRINK.value(), 0.5f, 1.0f);

                    if (drinkTimer <= 0) {
                        NpcUtils.consumeDrinkFromInventory(npc); // Либо черпаем из блока
                        npc.setNeedValue(NeedType.THIRST, 100f);
                    }
                    return;
                }

                // Взаимодействие с целью
                if (targetPos != null && npc.blockPosition().closerThan(targetPos, 2.0)) {
                    npc.getNavigation().stop();

                    if (isInspectingChest) {
                        // Пытаемся найти воду в сундуке
                        if (NpcUtils.tryDrinkFromStorage(npc, targetPos)) {
                            npc.memory.rememberBlock(MemoryCategory.BLOCKS, targetPos, MemoryTag.CHEST, MemoryTag.WATER);
                            if (NpcUtils.hasDrinkInInventory(npc)) drinkTimer = DRINK_DURATION;
                        } else {
                            // Воды нет — ставим метку, чтобы не проверять этот сундук на жажду какое-то время
                            npc.memory.rememberBlock(MemoryCategory.BLOCKS, targetPos, MemoryTag.CHEST, MemoryTag.EMPTY_WATER);
                            onStart(npc); // Ищем дальше
                        }
                    } else {
                        // Это просто блок воды (озеро/котел) — пьем прямо оттуда
                        drinkTimer = DRINK_DURATION;
                    }
                }

                // Подбор бутылки с земли
                if (targetItem != null && targetItem.isAlive() && npc.distanceToSqr(targetItem) < 1.5) {
                    // Предположим, предмет подбирается и попадает в инвентарь
                    if (NpcUtils.hasDrinkInInventory(npc)) drinkTimer = DRINK_DURATION;
                }
            }

            @Override
            public void onStop(NpcEntity npc) {

            }

            @Override public boolean isFinished(NpcEntity npc) { return npc.getNeedValue(NeedType.THIRST) >= 100f || (targetPos == null && targetItem == null && drinkTimer == 0); }
        });
        BEHAVIORS.put(NeedType.SLEEP, new Behavior() {
            private BlockPos targetBed;
            private float startSleepVal; // Чтобы знать, сколько мы реально проспали

            @Override
            public void onStart(NpcEntity npc) {
                startSleepVal = npc.getNeedValue(NeedType.SLEEP);
                var bedMem = npc.memory.getBest(MemoryCategory.REST, MemoryTag.BED);

                // Ищем кровать, если нет - только в крайнем случае ищем "тихий угол"
                targetBed = bedMem.map(MemoryEntry::getPosition).orElse(null);

                if (targetBed == null && startSleepVal < 10.0f) {
                    targetBed = NpcUtils.findQuietSpot(npc); // Падаем где стоим только при смерти
                }

                if (targetBed != null) {
                    float speed = startSleepVal < 20 ? 0.8f : 0.6f;
                    npc.getNavigation().moveTo(targetBed.getX() + 0.5, targetBed.getY(), targetBed.getZ() + 0.5, speed);
                }
            }

            @Override
            public void onUpdate(NpcEntity npc) {
                if (targetBed == null) return;

                if (npc.getPose() == Pose.SLEEPING) {
                    if (npc.tickCount % 20 == 0) {
                        npc.setNeedValue(NeedType.SLEEP, npc.getNeedValue(NeedType.SLEEP) + 2.0f);
                        ((ServerLevel)npc.level()).sendParticles(ParticleTypes.NOTE, npc.getX(), npc.getEyeY() + 0.3, npc.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                    }
                    return;
                }

                double distSqr = npc.blockPosition().distSqr(targetBed);
                if (distSqr < 2) {
                    // Если пришли к кровати или совсем устали
                    npc.getNavigation().stop();
                    npc.setDeltaMovement(Vec3.ZERO);
                    npc.setPos(targetBed.getX() + 0.5, targetBed.getY() + 0.2, targetBed.getZ() + 0.5);
                    npc.setPose(Pose.SLEEPING);
                }
            }

            @Override
            public void onStop(NpcEntity npc) {
                npc.setPose(Pose.STANDING);
                // Эффект даем только если реально поспали (например, больше 20 единиц)
                if (npc.getNeedValue(NeedType.SLEEP) - startSleepVal > 20) {
                    npc.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                }
                targetBed = null;
            }

            @Override
            public boolean isFinished(NpcEntity npc) {
                // Спим до победного
                return npc.getNeedValue(NeedType.SLEEP) >= 95.0f;
            }
        });

        BEHAVIORS.put(NeedType.HUNGER, new Behavior() {
            private BlockPos targetPos;
            private int eatProgress = 0;
            private boolean isFromInventory = false;
            private int EAT_TIME = 40;

            @Override
            public void onStart(NpcEntity npc) {
                eatProgress = 0;
                targetPos = null;
                isFromInventory = false;

                // 1. Сначала карманы. Если поел - мы закончили, не начиная.
                if (NpcUtils.tryEatFromInventory(npc)) {
                    isFromInventory = true;
                    return;
                }

                // 2. Ищем сундук, где ТОЧНО есть еда (по нашей памяти)
                var confirmedFood = npc.memory.getBest(MemoryCategory.BLOCKS, npc.blockPosition(), MemoryTag.CHEST, MemoryTag.FOOD);

                if (confirmedFood.isPresent()) {
                    targetPos = confirmedFood.get().getPosition();
                } else {
                    // 3. Если таких нет, ищем сундук, который МЫ ЕЩЕ НЕ ПРОВЕРЯЛИ (без тега EMPTY_FOOD)
                    // Тут важно: getBest должен уметь исключать теги, либо фильтруем вручную
                    var unknownChest = npc.memory.getAllByCategory(MemoryCategory.BLOCKS).stream()
                            .filter(e -> e.hasTag(MemoryTag.CHEST) && !e.hasTag(MemoryTag.EMPTY_FOOD))
                            .min(Comparator.comparingDouble(e -> e.getPosition().distSqr(npc.blockPosition())));

                    targetPos = unknownChest.map(MemoryEntry::getPosition).orElse(null);
                }

                if (targetPos != null) {
                    npc.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.1f);
                } else {
                    // Совсем беда - идем искать новые горизонты (чанки)
                    Vec3 searchPos = DefaultRandomPos.getPos(npc, 20, 10);
                    if (searchPos != null) npc.getNavigation().moveTo(searchPos.x, searchPos.y, searchPos.z, 1.2f);
                }
            }

            @Override
            public void onUpdate(NpcEntity npc) {
                // Если мы уже поели из инвентаря, просто ждем завершения тика или мгновенно финишируем
                if (isFromInventory || targetPos == null) return;

                if (targetPos.closerThan(npc.blockPosition(), 2)) {
                    npc.getNavigation().stop();
                    npc.getLookControl().setLookAt(targetPos.getX(), targetPos.getY(), targetPos.getZ());

                    if (eatProgress == 0) {
                        // Визуальный эффект открытия только если это РЕАЛЬНО сундук
                        npc.level().blockEvent(targetPos, npc.level().getBlockState(targetPos).getBlock(), 1, 1);
                    }

                    eatProgress++;

                    if (eatProgress % 10 == 0) {
                        npc.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 0.5f, 1.0f);
                    }

                    if (eatProgress >= EAT_TIME) {
                        if (NpcUtils.tryEatFromStorage(npc, targetPos)) {
                            npc.memory.rememberBlock(MemoryCategory.BLOCKS, targetPos, MemoryTag.CHEST, MemoryTag.FOOD);
                            npc.level().blockEvent(targetPos, npc.level().getBlockState(targetPos).getBlock(), 1, 0);
                            targetPos = null; // Мы сыты и довольны
                        } else {
                            // Пусто! Запоминаем это, чтобы не возвращаться
                            npc.memory.rememberBlock(MemoryCategory.BLOCKS, targetPos, MemoryTag.CHEST, MemoryTag.EMPTY_FOOD);
                            npc.level().blockEvent(targetPos, npc.level().getBlockState(targetPos).getBlock(), 1, 0);
                            onStart(npc); // Ищем следующий "деликатес"
                        }
                    }
                }
            }

            @Override
            public void onStop(NpcEntity npc) {

            }

            @Override
            public boolean isFinished(NpcEntity npc) {
                // Заканчиваем если: сыты, если поели из кармана, или если цель потеряна
                return npc.getNeedValue(NeedType.HUNGER) > 90 || isFromInventory || (targetPos == null && !npc.getNavigation().isInProgress());
            }
        });
        BEHAVIORS.put(NeedType.IDLE, new Behavior() {
            enum SubState { WANDER, LOOK_AROUND, VISIT_INTEREST, WATCH_SKY }

            private SubState state;
            private int stateTimer;
            private Entity lookTarget;
            private Vec3 distantView;

            @Override
            public void onStart(NpcEntity npc) {
                pickNewSubState(npc);
            }
            private void pickNewSubState(NpcEntity npc) {
                long time = npc.level().getGameTime() % 24000;
                boolean isGoldenHour = (time > 11500 && time < 12500) || (time > 23000 || time < 500);
                double r = npc.getRandom().nextDouble();

                stateTimer = 100 + npc.getRandom().nextInt(200); // Увеличим время фаз (5-15 сек)

                // 1. ПРИОРИТЕТ: Наслаждение закатом/рассветом
                if (isGoldenHour && r < 0.7) {
                    float angle = (time > 12000) ? 90f : -90f; // Запад или Восток
                    BlockPos viewPos = npc.blockPosition().relative(
                            (time > 12000) ? Direction.WEST : Direction.EAST, 2
                    );

                    // ПРОВЕРКА: Если прямо перед лицом стена — никакого заката
                    if (npc.level().getBlockState(viewPos).isAir()) {
                        state = SubState.WATCH_SKY;
                        npc.getNavigation().stop();
                        distantView = calculateHorizonPoint(npc, angle);
                        return;
                    }
                }

                // 2. Обычная рутина
                if (r < 0.1) { // К интересным местам
                    var interest = npc.memory.getBest(MemoryCategory.BLOCKS, npc.blockPosition());
                    if (interest.isPresent()) {
                        state = SubState.VISIT_INTEREST;
                        BlockPos pos = interest.get().getPosition();
                        npc.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.7f);
                        return;
                    }
                }

                if (r < 0.5) { // Погулять
                    Vec3 target = DefaultRandomPos.getPos(npc, 10, 3);
                    if (target != null) {
                        state = SubState.WANDER;
                        npc.getNavigation().moveTo(target.x, target.y, target.z, 0.7f);
                    } else {
                        state = SubState.LOOK_AROUND;
                    }
                } else { // Просто стоять
                    state = SubState.LOOK_AROUND;
                    npc.getNavigation().stop();
                }
            }
            @Override
            public void onUpdate(NpcEntity npc) {
                stateTimer--;

                // Плавный поиск цели взгляда
                if (stateTimer % 20 == 0 && state != SubState.WATCH_SKY) {
                    updateLookTarget(npc);
                }

                // ЛОГИКА ВЗГЛЯДА
                if (lookTarget != null && lookTarget.isAlive()) {
                    // Смотрим на существо
                    npc.getLookControl().setLookAt(lookTarget, 10f, 10f); // Уменьшил скорость поворота
                } else if (state == SubState.WATCH_SKY && distantView != null) {
                    if (stateTimer % 20 == 0) {
                        // Если перед глазами внезапно оказалась стена (кто-то поставил блок)
                        BlockPos headPos = npc.blockPosition().above();
                        Direction dir = (npc.level().getGameTime() % 24000 > 12000) ? Direction.WEST : Direction.EAST;

                        if (!npc.level().getBlockState(headPos.relative(dir)).isAir()) {
                            pickNewSubState(npc); // Ищем другое занятие, тут смотреть не на что
                        }
                    }
                    npc.getLookControl().setLookAt(distantView.x, npc.getEyeY() + 0.5, distantView.z, 5f, 5f);
                } else if (state == SubState.LOOK_AROUND) {
                    // Ленивое поглядывание по сторонам
                    if (stateTimer % 60 == 0) {
                        float randomYaw = npc.getYRot() + (npc.getRandom().nextFloat() - 0.5f) * 90;
                        distantView = calculateHorizonPoint(npc, randomYaw);
                    }
                    if (distantView != null) {
                        npc.getLookControl().setLookAt(distantView.x, npc.getEyeY(), distantView.z, 3f, 3f);
                    }
                }

                if (stateTimer <= 0 || (state == SubState.WANDER && npc.getNavigation().isDone())) {
                    pickNewSubState(npc);
                }
            }
            // Хелпер для взгляда в "никуда" (на горизонт)
            private Vec3 calculateHorizonPoint(NpcEntity npc, float yaw) {
                double rad = Math.toRadians(-yaw);
                return new Vec3(
                        npc.getX() + Math.sin(rad) * 10,
                        npc.getEyeY(),
                        npc.getZ() + Math.cos(rad) * 10
                );
            }
            private void updateLookTarget(NpcEntity npc) {
                var nearest = npc.memory.nearestEntities;
                if (!nearest.isEmpty()) {
                    Entity potential = nearest.get(0);
                    // Не пялимся на врагов (для этого есть SAFETY) и на тех, кто далеко
                    if (!(potential instanceof Monster) && npc.distanceToSqr(potential) < 64) {
                        lookTarget = potential;
                    }
                } else {
                    lookTarget = null;
                }
            }

            @Override
            public void onStop(NpcEntity npc) {
                lookTarget = null;
                distantView = null;
            }

            @Override
            public boolean isFinished(NpcEntity npc) { return false; }
        });

        BEHAVIORS.put(NeedType.SAFETY, new Behavior() {
            private LivingEntity primaryThreat;
            private int rethinkTimer;
            private boolean isFleeing; // Флаг текущей стратегии

            // Списки лучше оставить полями для Zero-Allocation, но ОБЯЗАТЕЛЬНО чистить
            private final List<LivingEntity> hostiles = new ArrayList<>();
            private final List<NpcEntity> brothers = new ArrayList<>();

            @Override
            public void onStart(NpcEntity npc) {
                rethinkTimer = 0;
                isFleeing = false;
                updateTactics(npc);
            }

            private void updateTactics(NpcEntity npc) {
                hostiles.clear();
                brothers.clear();

                // 1. Быстрый разбор окружения
                for (Entity e : npc.memory.nearestEntities) {
                    if (!e.isAlive() || e == npc) continue;
                    if (e instanceof Monster m) hostiles.add(m);
                    else if (e instanceof NpcEntity bro && npc.distanceToSqr(bro) < 100) brothers.add(bro);
                }

                if (hostiles.isEmpty()) {
                    npc.brain.stopActiveBehavior();
                    return;
                }

                primaryThreat = hostiles.getFirst();

                // 2. Оценка сил
                float enemyPower = 0;
                for (LivingEntity m : hostiles) enemyPower += NpcUtils.calculatePower(m);

                float ourPower = NpcUtils.calculatePower(npc);
                for (NpcEntity bro : brothers) ourPower += NpcUtils.calculatePower(bro) * 0.8f;

                // 3. Принятие решения
                isFleeing = (enemyPower > (ourPower * 1.2f)) || npc.getHealth() < 8;

                if (isFleeing) {
                    executeEscape(npc);
                } else {
                    // Просто даем команду на сближение, остальное сделает onUpdate
                    npc.getNavigation().moveTo(primaryThreat, 1.2f);
                }
            }

            private void executeEscape(NpcEntity npc) {
                var safePoint = npc.memory.getBest(MemoryCategory.REST, MemoryTag.BED);
                if (safePoint.isPresent()) {
                    BlockPos pos = safePoint.get().getPosition();
                    npc.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.3f);
                } else {
                    // Убегаем от первичной угрозы
                    Vec3 fleeVec = DefaultRandomPos.getPosAway(npc, 20, 10, primaryThreat.position());
                    if (fleeVec != null) npc.getNavigation().moveTo(fleeVec.x, fleeVec.y, fleeVec.z, 1.3f);
                }
            }

            @Override
            public void onUpdate(NpcEntity npc) {
                rethinkTimer++;

                if (rethinkTimer % 20 == 0) updateTactics(npc);

                if (primaryThreat != null && primaryThreat.isAlive()) {
                    double distSqr = npc.distanceToSqr(primaryThreat);

                    npc.getLookControl().setLookAt(primaryThreat, 30.0f, 30.0f);

                    // Если МЫ НЕ БЕЖИМ, тогда преследуем и бьем
                    if (!isFleeing) {
                        if (distSqr > 3.0 && rethinkTimer % 5 == 0) {
                            npc.getNavigation().moveTo(primaryThreat, 1.2f);
                        }

                        if (distSqr <= 4.0 && npc.getAttackAnim(0) == 0) {
                            npc.swing(InteractionHand.MAIN_HAND);
                            npc.doHurtTarget(primaryThreat);
                        }
                    }
                }
            }

            @Override
            public void onStop(NpcEntity npc) {
                primaryThreat = null;
                npc.setLastHurtByMob(null);
            }

            @Override
            public boolean isFinished(NpcEntity npc) {
                // Оптимизировано: если в памяти больше нет врагов, поведение закончено
                for (Entity e : npc.memory.nearestEntities) {
                    if (e instanceof Monster && e.isAlive()) return false;
                }
                return true;
            }
        });
    }

    public static Behavior get(NeedType type) { return BEHAVIORS.get(type); }
}