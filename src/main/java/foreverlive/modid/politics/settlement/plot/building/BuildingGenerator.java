package foreverlive.modid.politics.settlement.plot.building;

public class BuildingGenerator {

//    public static void buildPlot(ServerLevel level, BuildingPlot plot, BuildingPalette palette, int tier) {
//        // 1. Генерируем планировку комнат
//        List<FloorPlanGenerator.RoomNode> rooms = FloorPlanGenerator.generateLayout(
//                plot.getType(), tier, plot.getMinPos(), plot.getMaxPos()
//        );
//
//        // 2. Настраиваем NBT вставку с перехватчиком блоков
//        StructureTemplateManager templateManager = level.getStructureManager();
//        StructurePlaceSettings settings = new StructurePlaceSettings()
//                .setRotation(getRotationFromDirection(plot.getFacing()))
//                .addProcessor(new PaletteStructureProcessor(palette));
//
//        // 3. Строим фундаментальную часть
//        buildFoundation(level, plot, palette);
//
//        // 4. Размещаем модули комнат
//        for (FloorPlanGenerator.RoomNode room : rooms) {
//            // Загружаем готовый NBT-модуль комнаты/стены
//            ResourceLocation templateLoc = new ResourceLocation("foreverlive", "buildings/modules/" + room.type().name().toLowerCase());
//            Optional<StructureTemplate> templateOpt = templateManager.get(templateLoc);
//
//            templateOpt.ifPresent(template -> {
//                template.placeInWorld(level, room.min(), room.min(), settings, level.getRandom(), 2);
//            });
//        }
//    }
//
//    private static void buildFoundation(ServerLevel level, BuildingPlot plot, BuildingPalette palette) {
//        BlockPos min = plot.getMinPos();
//        BlockPos max = plot.getMaxPos();
//
//        // Заполняем прослойку под зданием до твердого грунта
//        for (int x = min.getX(); x <= max.getX(); x++) {
//            for (int z = min.getZ(); z <= max.getZ(); z++) {
//                BlockPos current = new BlockPos(x, min.getY() - 1, z);
//                while (level.isEmptyBlock(current) && level.isInsideBuildHeight(current.getY())) {
//                    level.setBlock(current, palette.get(BuildingPalette.PaletteKey.FOUNDATION), 3);
//                    current = current.below();
//                }
//            }
//        }
//    }
//
//    private static Rotation getRotationFromDirection(net.minecraft.core.Direction direction) {
//        return switch (direction) {
//            case SOUTH -> Rotation.CLOCKWISE_180;
//            case WEST -> Rotation.COUNTERCLOCKWISE_90;
//            case EAST -> Rotation.CLOCKWISE_90;
//            default -> Rotation.NONE;
//        };
//    }
}