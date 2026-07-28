package foreverlive.modid.politics.builder;

import foreverlive.modid.politics.POJO.Settlement;
import foreverlive.modid.politics.builder.generators.ProceduralBuildingEngine;
import foreverlive.modid.politics.builder.generators.RoadGenerator;
import foreverlive.modid.politics.builder.generators.WallGenerator;
import foreverlive.modid.politics.builder.services.BuildingPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class SettlementGrowthController {

    public static void advanceTier(ServerLevel world, Settlement settlement) {
        int currentTier = settlement.getTier();
        settlement.setTier(currentTier + 1);

        switch (settlement.getTier()) {
            case 2 -> {
                // Tier 2: Добавляем ветки дорог, ставим 10 новых домов, строим деревянный забор
//                RoadGenerator.expandRoads(world, settlement, 4);
//                BuildingPlacer.populateSettlement(world, settlement, ProceduralBuildingEngine.BuildingStyle.MEDIEVAL_OAK);
            }
            case 3 -> {
                // Tier 3: Заменяем забор на каменную стену с воротами, мостим дороги брусчаткой
//                WallGenerator.generateWallsAndGates(world, settlement, settlement.getRoads(), SettlementStyle.TOWN_STONE);
//                BuildingPlacer.populateSettlement(world, settlement, ProceduralBuildingEngine.BuildingStyle.TOWN_BRICK);
            }
            case 4 -> {
                // Tier 4: Замок на самой высокой точке, 2-й контур стен, ратуша
//                BlockPos highestPoint = findHighestPointInSettlement(world, settlement);
//                CastleGenerator.buildCastleKeep(world, highestPoint);
            }
            case 5 -> {
                // Tier 5 (Столица): Акведуки, Шпили, Мосты через ущелья
//                AqueductGenerator.buildAqueducts(world, settlement);
//                BridgingService.connectCliffsWithViaducts(world, settlement);
            }
        }
    }
}