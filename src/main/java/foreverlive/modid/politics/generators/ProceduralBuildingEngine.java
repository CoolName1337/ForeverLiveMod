package foreverlive.modid.politics.generators;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.*;

public class ProceduralBuildingEngine {

    public record BuildingStyle(
            BlockState foundation,
            BlockState wallPrimary,
            BlockState wallFrame,
            BlockState roofBlock,
            BlockState roofStair,
            BlockState floorBlock
    ) {
        public static BuildingStyle MEDIEVAL_OAK = new BuildingStyle(
                Blocks.COBBLESTONE.defaultBlockState(),
                Blocks.WHITE_CONCRETE.defaultBlockState(), // Стены (Фахверк)
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(), // Каркас
                Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.DARK_OAK_STAIRS.defaultBlockState(),
                Blocks.SPRUCE_PLANKS.defaultBlockState()
        );
    }

    /**
     * Главный метод генерации адаптивного процедурного здания
     */
    public static void generateBuilding(ServerLevel world, BlockPos origin, Direction facing, BuildingStyle style) {
        Random rand = new Random(origin.asLong());

        // 1. Вычисляем размеры основного корпуса и пристройки
        int mainWidth = 6 + rand.nextInt(3);  // 6-8 блоков
        int mainDepth = 7 + rand.nextInt(4);  // 7-10 блоков
        int floors = 2 + rand.nextInt(2);     // 2-3 этажа

        // 2. Строим каменный фундамент (адаптация к рельефу Мезы)
        buildFoundation(world, origin, mainWidth, mainDepth, style.foundation());

        // 3. Возводим этажи с вылетом (Overhangs) и каркасом
        List<BlockPos> doorPositions = new ArrayList<>();
        BlockPos currentOrigin = origin;
        int currentW = mainWidth;
        int currentD = mainDepth;

        for (int f = 0; f < floors; f++) {
            int floorHeight = 3;
            int yOffset = f * (floorHeight + 1);

            // На 1-м и 2-м этажах делаем вылет (overhang) на 1 блок в каждую сторону
            if (f > 0) {
                currentOrigin = currentOrigin.offset(-1, 0, -1);
                currentW += 2;
                currentD += 2;
            }

            BlockPos floorBase = currentOrigin.above(yOffset + 1);

            // Пол этажа
            buildFloor(world, floorBase, currentW, currentD, style.floorBlock());

            // Каркас и стены этажа
            buildFloorWalls(world, floorBase, currentW, currentD, floorHeight, style);

            // Если это первый этаж — прорубаем дверь
            if (f == 0) {
                BlockPos doorPos = floorBase.offset(currentW / 2, 1, 0);
                world.setBlock(doorPos, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(doorPos.above(), Blocks.AIR.defaultBlockState(), 3);
                doorPositions.add(doorPos);
            }

            // Процедурный интерьер этажа (Перегородки, Мебель, Окна)
            decorateInterior(world, floorBase, currentW, currentD, floorHeight, f, rand);
        }

        // 4. Генерируем сложную двухскатную крышу
        int roofStartY = floors * 4 + 1;
        buildGableRoof(world, currentOrigin.above(roofStartY), currentW, currentD, facing, style);

        // 5. Добавляем Дымоход с камином (Атмосфера/Вайб)
        buildChimney(world, origin, floors * 4 + 4, rand);
    }

    private static void buildFoundation(ServerLevel world, BlockPos origin, int w, int d, BlockState foundationState) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                BlockPos top = origin.offset(x, 0, z);
                for (int y = 0; y <= 12; y++) {
                    BlockPos check = top.below(y);
                    if (world.getBlockState(check).isSolidRender()) break;
                    world.setBlock(check, foundationState, 3);
                }
            }
        }
    }

    private static void buildFloor(ServerLevel world, BlockPos base, int w, int d, BlockState floorState) {
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                world.setBlock(base.offset(x, 0, z), floorState, 3);
            }
        }
    }

    private static void buildFloorWalls(ServerLevel world, BlockPos base, int w, int d, int height, BuildingStyle style) {
        for (int y = 1; y <= height; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean isCorner = (x == 0 || x == w - 1) && (z == 0 || z == d - 1);
                    boolean isEdge = x == 0 || x == w - 1 || z == 0 || z == d - 1;

                    if (isEdge) {
                        BlockPos p = base.offset(x, y, z);
                        if (isCorner) {
                            // Бревна по углам (Pillars)
                            world.setBlock(p, style.wallFrame(), 3);
                        } else {
                            // Заполнение стен
                            world.setBlock(p, style.wallPrimary(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * Генерация двухскатной крыши со свесами
     */
    private static void buildGableRoof(ServerLevel world, BlockPos roofBase, int w, int d, Direction facing, BuildingStyle style) {
        int steps = w / 2 + 1;

        for (int i = 0; i < steps; i++) {
            int currentY = i;
            int startX = i;
            int endX = w - 1 - i;

            for (int z = -1; z <= d; z++) { // Свес крыши на 1 блок спереди и сзади
                BlockPos leftStair = roofBase.offset(startX - 1, currentY, z);
                BlockPos rightStair = roofBase.offset(endX + 1, currentY, z);

                if (style.roofStair().hasProperty(StairBlock.FACING)) {
                    world.setBlock(leftStair, style.roofStair().setValue(StairBlock.FACING, Direction.EAST), 3);
                    world.setBlock(rightStair, style.roofStair().setValue(StairBlock.FACING, Direction.WEST), 3);
                } else {
                    world.setBlock(leftStair, style.roofBlock(), 3);
                    world.setBlock(rightStair, style.roofBlock(), 3);
                }

                // Заполнение конька
                for (int fillX = startX; fillX <= endX; fillX++) {
                    world.setBlock(roofBase.offset(fillX, currentY, z), style.roofBlock(), 3);
                }
            }
        }
    }

    /**
     * Процедурная меблировка и нарезка комнат
     */
    private static void decorateInterior(ServerLevel world, BlockPos base, int w, int d, int height, int floorIndex, Random rand) {
        // 1. Вставляем окна в стены
        for (int x = 2; x < w - 2; x += 3) {
            world.setBlock(base.offset(x, 2, 0), Blocks.GLASS_PANE.defaultBlockState(), 3);
            world.setBlock(base.offset(x, 2, d - 1), Blocks.GLASS_PANE.defaultBlockState(), 3);
        }

        // 2. Мебель первого этажа (Кухня / Гостиная)
        if (floorIndex == 0) {
            // Очаг / Печка
            world.setBlock(base.offset(1, 1, 1), Blocks.BLAST_FURNACE.defaultBlockState(), 3);
            world.setBlock(base.offset(1, 1, 2), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            world.setBlock(base.offset(w - 2, 1, 1), Blocks.CHEST.defaultBlockState(), 3);
        }
        // 3. Мебель второго этажа (Спальня)
        else if (floorIndex == 1) {
            world.setBlock(base.offset(1, 1, d - 2), Blocks.RED_BED.defaultBlockState(), 3);
            world.setBlock(base.offset(2, 1, d - 2), Blocks.BOOKSHELF.defaultBlockState(), 3);
            world.setBlock(base.offset(w - 2, 1, d - 2), Blocks.JUKEBOX.defaultBlockState(), 3);
        }

        // 4. Лестница на следующий этаж
        for (int h = 1; h <= height; h++) {
            BlockPos stairPos = base.offset(w - 2, h, 1 + h);
            world.setBlock(stairPos, Blocks.OAK_STAIRS.defaultBlockState(), 3);
            // Пробиваем дыру в потолке над лестницей
            world.setBlock(stairPos.above(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /**
     * Дымоход из камня с дымящим костром на вершине
     */
    private static void buildChimney(ServerLevel world, BlockPos origin, int totalHeight, Random rand) {
        BlockPos chimneyBase = origin.offset(1, 1, 1);
        for (int y = 0; y <= totalHeight + 2; y++) {
            world.setBlock(chimneyBase.above(y), Blocks.BRICKS.defaultBlockState(), 3);
        }
        // Костер на вершине трубы для дыма
        world.setBlock(chimneyBase.above(totalHeight + 3), Blocks.CAMPFIRE.defaultBlockState(), 3);
    }
}