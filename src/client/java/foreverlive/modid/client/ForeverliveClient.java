package foreverlive.modid.client;

import foreverlive.modid.Foreverlive;
import foreverlive.modid.ModEntities;
import foreverlive.modid.S2C.BulkNpcStatePayload;
import foreverlive.modid.entities.NpcEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;

public class ForeverliveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Регистрация рендерера
		EntityRenderers.register(ModEntities.NPC, NpcRenderer::new);

		// Регистрация агрегированного ресивера
		ClientPlayNetworking.registerGlobalReceiver(BulkNpcStatePayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				var world = context.client().level;
				if (world == null) return;

				payload.updates().forEach((entityId, stateData) -> {
					Entity entity = world.getEntity(entityId);

					if (entity instanceof NpcEntity npc) {
						npc.syncTracker.updateClientState(stateData);
					}
				});
			});
		});
	}
}
