package foreverlive.modid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import foreverlive.modid.S2C.BulkNpcStatePayload;
import foreverlive.modid.S2C.NpcSyncManager;
import foreverlive.modid.npc.SyncCategory;
import foreverlive.modid.config.ModConfig;
import foreverlive.modid.entities.NpcEntity;
import foreverlive.modid.npc.components.memory.MemoryTag;
import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.builder.SettlementPlanner;
import foreverlive.modid.politics.builder.SettlementStyle;
import foreverlive.modid.politics.builder.WorldBuildQueue;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permissions;
import java.util.concurrent.CompletableFuture;

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

	private void generateCommandExecute(String lit, ServerPlayer player) {
		ServerLevel world = player.level();
		BlockPos playerPos = player.blockPosition();

		Settlement settlement = new Settlement();
		settlement.origin = playerPos;

		SettlementStyle style = switch (lit) {
			case "village" -> SettlementStyle.createVillage();
			case "town" -> SettlementStyle.createTown();
			case "city" -> SettlementStyle.createCity();
			case "capital" -> SettlementStyle.createCapital();
			default -> SettlementStyle.createHamlet();
		};

		// ЗАПУСКАЕМ ТЯЖЕЛЫЙ РАСЧЕТ В ФОНОВОМ ПОТОКЕ, чтобы не фризить сервер!
		CompletableFuture.runAsync(() -> {
			LOGGER.info("Расчет города {} запущен асинхронно...", lit);
			SettlementPlanner.planAndBuild(world, settlement, style);
		});
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Let's get it");
		loadConfig();

		ModEntities.NPC.toString();
		MemoryTag.ENTITY.toString();

		FabricDefaultAttributeRegistry.register(ModEntities.NPC, NpcEntity.createAttributes());
		PayloadTypeRegistry.clientboundPlay().register(BulkNpcStatePayload.ID, BulkNpcStatePayload.CODEC);

		// ИСПРАВЛЕНО: Регистрируем ивент ЕДИНОЖДЫ, а внутренний цикл строит ветки
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			var generateCmd = Commands.literal("generate");

			for (String type : new String[] {"hamlet", "village", "town", "city", "capital"}) {
				generateCmd.then(Commands.literal(type)
						.executes(context -> {
							context.getSource().sendSuccess(
									() -> Component.literal("§a[CityEngine] §fЗапускаем асинхронный расчет для: " + type),
									false
							);
							generateCommandExecute(type, context.getSource().getPlayerOrException());
							return 1;
						}));
			}

			dispatcher.register(generateCmd);
		});

		ServerTickEvents.START_SERVER_TICK.register(world -> {
			ServerLevel overworld = world.overworld();
			if (overworld != null) {
				WorldBuildQueue.tick(overworld);
			}
		});

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