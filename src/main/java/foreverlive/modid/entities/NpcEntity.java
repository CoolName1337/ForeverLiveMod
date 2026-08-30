package foreverlive.modid.entities;

import foreverlive.modid.Foreverlive;
import foreverlive.modid.npc.components.proccessing.NpcNeed;
import foreverlive.modid.npc.components.memory.MemoryCategory;
import foreverlive.modid.npc.components.memory.MemoryTag;
import foreverlive.modid.npc.components.proccessing.NeedType;
import foreverlive.modid.npc.*;
import foreverlive.modid.npc.components.proccessing.NpcBrain;
import foreverlive.modid.npc.components.memory.NpcMemory;
import foreverlive.modid.npc.components.personality.NpcPersonality;
import foreverlive.modid.npc.components.proccessing.NpcSensors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NpcEntity extends PathfinderMob {
    private static final List<String> NAMES = List.of("Small Dickenson", "Remi Rat", "Harry Sex", "Peter Tvar", "Sigma Shitfun", "Dick Sucker");
    public static final float TICK_MULTIPLIER = 0.013888f;
    public static final int NEEDS_UPDATE_PER_TICK = 20;
    private final Map<NeedType, NpcNeed> needs = new EnumMap<>(NeedType.class);

    private BlockPos targetPos;
    private Entity cachedLookTarget;

    public final NpcBrain brain = new NpcBrain(this);
    public final NpcMemory memory = new NpcMemory(this);
    private final NpcSensors sensors = new NpcSensors(this);
    public final NpcPersonality personality = new NpcPersonality();
    public final NpcSyncTracker syncTracker = new NpcSyncTracker(this);
    private final SimpleContainer inventory = new SimpleContainer(27);

    private static final EntityDataAccessor<String> NPC_NAME =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SKIN_INDEX =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.INT);

    public SimpleContainer getInventory(){
        return inventory;
    }

    public float getNeedThreshold(NeedType type){
        var need = this.needs.get(type);
        return need != null ? need.threshold : 1f;
    }
    public float getNeedScore(NeedType type){
        var need = this.needs.get(type);
        return (need != null) ? need.getScore() : 0f;
    }
    public float getNeedValue(NeedType type) {
        var need = this.needs.get(type);
        return need != null ? need.value : 0f;
    }
    public void setNeedValue(NeedType type, float val) {
        var need = this.needs.get(type);
        if (need != null)
            need.value = val;

        if(!this.level().isClientSide())
            syncTracker.checkNeedThreshold(type, val);
    }

    public String getNpcName() {
        return this.entityData.get(NPC_NAME);
    }

    public void setNpcName(String name) {
        this.entityData.set(NPC_NAME, name);
    }

    public int getSkinIndex() {
        return this.entityData.get(SKIN_INDEX);
    }

    public void setSkinIndex(int index) {
        this.entityData.set(SKIN_INDEX, index);
    }

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        initFromConfig();
        ensureNeedsInitialized();
    }

    @Override
    public SpawnGroupData finalizeSpawn(@Nullable ServerLevelAccessor world, @Nullable DifficultyInstance difficulty, @Nullable EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(world, difficulty, spawnReason, spawnGroupData);

        generateRandomProfile();

        return spawnGroupData;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NonNull Builder builder) {
        super.defineSynchedData(builder);
        // Регистрируем дефолтные значения
        builder.define(NPC_NAME, "Григорий");
        builder.define(SKIN_INDEX, 0);
    }

    private void generateRandomProfile() {
        setNpcName(NAMES.get(random.nextInt(NAMES.size())));
        setSkinIndex(random.nextInt(5));
        setCustomName(Component.literal(getNpcName()));
    }

    private void initFromConfig(){
        var config = Foreverlive.CONFIG;

        if(config != null && config.needs != null){
            config.needs.forEach((type, settings) -> {
                this.needs.put(type, new NpcNeed(
                        type,
                        settings.priority,
                        settings.threshold + (getRandom().nextFloat() * 10),
                        settings.decay
                ));
            });
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 1)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5);
    }

    @Override
    public void addAdditionalSaveData(@NonNull ValueOutput valueOutput){
        super.addAdditionalSaveData(valueOutput);

        memory.save(valueOutput);
        brain.save(valueOutput);
        personality.save(valueOutput);

        needs.forEach((type,need) ->
                valueOutput.putFloat(type.getKey(), need.value));


        if(targetPos != null)
            valueOutput.putLong("target_pos", targetPos.asLong());

        valueOutput.putString("npc_name", getNpcName());
        valueOutput.putInt("npc_skin_index", getSkinIndex());
    }
    @Override
    public void readAdditionalSaveData(@NonNull ValueInput valueInput){
        super.readAdditionalSaveData(valueInput);

        memory.read(valueInput);
        brain.load(valueInput);
        personality.load(valueInput);

        for (NeedType type : NeedType.values()){
            float savedValue = valueInput.getFloatOr(type.getKey(), 100f);

            if(this.needs.containsKey(type))
                this.needs.get(type).value = savedValue;
            else
                this.needs.put(type, new NpcNeed(type, 1, 40,1));
        }

        long posLong = valueInput.getLongOr("target_pos", -1L);
        if (posLong != -1L) {
            this.targetPos = BlockPos.of(posLong);
        }

        setNpcName(valueInput.getStringOr("npc_name", "Greg"));
        setSkinIndex(valueInput.getIntOr("npc_skin_index", 0));
    }

    private void ensureNeedsInitialized() {
        for (NeedType type : NeedType.values()) {
            this.needs.computeIfAbsent(type, t -> new NpcNeed(t, 1.0f, 40.0f, 1.0f));
        }
    }
    @Override
    public @NonNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.level().isClientSide()) {
            StringBuilder status = new StringBuilder("§6NPC [" + this.getNpcName() + "]§r: ");

            needs.forEach((type, need) -> {
                status.append(need.name).append(": ")
                        .append(String.format("%.1f", need.value))
                        .append(" | ");
            });
            status.append("Nearest entities: \n");
            status.append(memory.nearestEntities.size());
            status.append("Best block: \n");
            status.append(memory.getBest(MemoryCategory.BLOCKS, MemoryTag.CHEST));
            player.sendSystemMessage(Component.literal(status.toString()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        pickUpItems();
    }
    public void pickUpItems() {
        // Ищем предметы в радиусе 1.5 блоков (чтобы не пылесосило через стены)
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(1.5, 1.0, 1.5));

        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();

            // Пытаемся засунуть в наш кастомный инвентарь
            ItemStack remainder = inventory.addItem(stack); // Метод addItem должен быть в твоем классе инвентаря

            if (remainder.isEmpty()) {
                itemEntity.discard(); // Подобрали всё
                playSound(SoundEvents.ITEM_PICKUP, 0.5f, 1.5f);
            } else {
                itemEntity.setItem(remainder); // Если инвентарь забит, часть останется на земле
            }
        }
    }

    // дал пизды
    public boolean doHurtTarget(Entity target){
        if(level() instanceof ServerLevel serverLevel)
            return super.doHurtTarget(serverLevel, target);
        return false;
    }

    // получил пизды
    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        super.actuallyHurt(level, source, amount);

        // Если это не урон от падения или голода, а реально кто-то втащил
        if (source.getEntity() != null) {
            // Мгновенно пробуждаем мозг, не дожидаясь фазы в 5 тиков
            this.sensors.senseEntities();

            this.brain.reThink();
            // Можно добавить звук возмущения
            // this.playSound(SoundEvents.VILLAGER_HURT, 1.0f, 1.0f);
        }
    }
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        // Нужды тикают сами по себе
        this.updateNeeds();
        // Sync
        this.syncTracker.tickSync();

        // Частота скана указана внутри
        this.sensors.senseWorld();

        this.brain.tick(); // <- тут updateBehavior() + think()

        // Память чистит старые записи
        if(isPhase(200)){
            this.memory.tick(this.level().getGameTime());
        }
    }
    public boolean isPhase(int interval) {
        // Используем ID сущности как оффсет, чтобы Григории тикали в разные моменты
        return BrainController.isPhase (this, interval);
    }

    private void updateNeeds() {
        // Тикаем по фазе, чтобы не грузить проц
        if ((this.tickCount + this.getId()) % NEEDS_UPDATE_PER_TICK != 0) return;

        for (Map.Entry<NeedType, NpcNeed> entry : needs.entrySet()) {
            // Обновляем только те нужды, которые помечены как накапливаемые
            if (entry.getKey().isPersistent()) {
                NpcNeed need = entry.getValue();
                float totalDecay = need.decay * TICK_MULTIPLIER * NEEDS_UPDATE_PER_TICK;

                // Уменьшаем значение (Григорий становится голоднее/хочет спать)
                setNeedValue(entry.getKey(), Math.max(0, need.value - totalDecay));
            }
        }
    }
}
