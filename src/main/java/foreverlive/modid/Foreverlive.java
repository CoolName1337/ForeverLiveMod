package foreverlive.modid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import foreverlive.modid.S2C.BulkNpcStatePayload;
import foreverlive.modid.S2C.NpcSyncManager;
import foreverlive.modid.npc.SyncCategory;
import foreverlive.modid.config.ModConfig;
import foreverlive.modid.entities.NpcEntity;
import foreverlive.modid.npc.components.memory.MemoryTag;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Foreverlive implements ModInitializer {
	public static final String MOD_ID = "forever_live";
	public static ModConfig CONFIG;
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fl_ai.json");
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public void loadConfig() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			if (Files.exists(CONFIG_PATH)) {
				CONFIG = gson.fromJson(Files.newBufferedReader(CONFIG_PATH), ModConfig.class);
			} else {
				CONFIG = ModConfig.createDefault();
				Files.write(CONFIG_PATH, gson.toJson(CONFIG).getBytes());
			}
		} catch (IOException ex) {
			CONFIG = ModConfig.createDefault();
			ex.printStackTrace();
		}
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Let's get it");
		loadConfig();

		ModEntities.NPC.toString();
		MemoryTag.ENTITY.toString();

		FabricDefaultAttributeRegistry.register(ModEntities.NPC, NpcEntity.createAttributes());
		PayloadTypeRegistry.clientboundPlay().register(BulkNpcStatePayload.ID, BulkNpcStatePayload.CODEC);

		ServerTickEvents.END_SERVER_TICK.register(world -> {
			if (world.getTickCount() % 5 == 0) {
				NpcSyncManager.flush(world);
			}
		});

		EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
			if (entity instanceof NpcEntity npc) {
				npc.syncTracker.markDirty(SyncCategory.NEEDS);
				LOGGER.info("Forcing sync for player {} started tracking NPC {}", player.getName().getString(), npc.getNpcName());
			}
		});
	}
}