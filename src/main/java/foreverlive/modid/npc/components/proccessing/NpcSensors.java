package foreverlive.modid.npc.components.proccessing;

import foreverlive.modid.Foreverlive;
import foreverlive.modid.npc.components.memory.MemoryCategory;
import foreverlive.modid.npc.components.memory.MemoryTag;
import foreverlive.modid.entities.NpcEntity;
import foreverlive.modid.npc.BrainController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NpcSensors {
    private final NpcEntity npc;
    public final int SCAN_RADIUS = 7;
    public int scanBlockInterval = 20; // Блоки можно сканировать реже (раз в секунду)
    public int scanEntityInterval = 10; // Сущности — раз в полсекунды

    public NpcSensors(NpcEntity npc){
        this.npc = npc;
    }

    public void senseWorld(){
        // Важно: проверяем фазы раздельно, чтобы не делать всё в один тик
        if (BrainController.isPhase(npc, scanEntityInterval))
            senseEntities();
        if (BrainController.isPhase(npc, scanBlockInterval))
            senseBlocks();

    }

    public void senseEntities(){
        int effectiveRadius = SCAN_RADIUS;

        // Если Григорий спит, он слышит только в радиусе 3 блоков
        if (npc.getPose() == Pose.SLEEPING) {
            effectiveRadius = 3;
        }

        AABB searchBox = npc.getBoundingBox().inflate(effectiveRadius);

        // Получаем список. Minecraft внутри использует быстрый поиск по секциям чанков.
        List<Entity> entities = npc.level().getEntitiesOfClass(
                Entity.class,
                searchBox,
                e -> e != npc && e.isAlive()
        );

        if (entities.isEmpty()) {
            npc.memory.updateL0(Collections.emptyList());
            return;
        }

        // Вместо стримов: если сущностей мало, берем всех. Если много — фильтруем вручную.
        if (entities.size() > 24) {
            entities.sort(Comparator.comparingDouble(npc::distanceToSqr));
            entities = entities.subList(0, 20);
        }

        npc.memory.updateL0(entities);

        for (Entity e : entities) {
            if (e instanceof Monster) {
                npc.memory.rememberEntity(MemoryCategory.ENTITIES, e.blockPosition(), e.getUUID(), MemoryTag.THREAT);
            } else if (e instanceof ItemEntity item) {
                // Если это бутылка воды или еда — помечаем специально
                if (isUsefulItem(item.getItem())) {
                    npc.memory.rememberEntity(MemoryCategory.DROP, e.blockPosition(), e.getUUID(), MemoryTag.ITEM);
                }
            } else if (e instanceof NpcEntity) {
                npc.memory.rememberEntity(MemoryCategory.SOCIAL, e.blockPosition(), e.getUUID(), MemoryTag.NPC);
            }
        }
    }
    private boolean isUsefulItem(ItemStack stack) {
        return stack.is(Items.POTION) || stack.has(DataComponents.FOOD);
    }
    private void senseBlocks() {
        BlockPos eyePos = npc.blockPosition();
        ServerLevel world = (ServerLevel) npc.level();

        // 1. POI (Кровати)
        world.getPoiManager().getInSquare(type -> type.is(PoiTypes.HOME), eyePos, SCAN_RADIUS, PoiManager.Occupancy.ANY)
                .forEach(poi -> npc.memory.rememberBlock(MemoryCategory.REST, poi.getPos().immutable(), MemoryTag.BED));

        // 2. Блоки которые по POI не пробиваются
        int chunkMinX = (eyePos.getX() - SCAN_RADIUS) >> 4;
        int chunkMaxX = (eyePos.getX() + SCAN_RADIUS) >> 4;
        int chunkMinZ = (eyePos.getZ() - SCAN_RADIUS) >> 4;
        int chunkMaxZ = (eyePos.getZ() + SCAN_RADIUS) >> 4;

        for (int x = chunkMinX; x <= chunkMaxX; x++) {
            for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                LevelChunk chunk = world.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;

                // Вместо .values() используем прямой перебор Map, если возможно,
                // либо обычный for-each, который в новых Java неплохо оптимизируется.
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();

                    // Простая проверка дистанции (без корня)
                    int dx = eyePos.getX() - pos.getX();
                    int dz = eyePos.getZ() - pos.getZ();
                    if ((dx * dx + dz * dz) <= (SCAN_RADIUS * SCAN_RADIUS)) {
                        if (be instanceof ChestBlockEntity) {
                            // Важно: передаем immutable, чтобы не хранить ссылку на мутабельный объект
                            Foreverlive.LOGGER.info("Find chest {}",be.getBlockPos());
                            npc.memory.rememberBlock(MemoryCategory.BLOCKS, pos.immutable(), MemoryTag.CHEST);
                        }
                    }
                }
            }
        }
    }
}