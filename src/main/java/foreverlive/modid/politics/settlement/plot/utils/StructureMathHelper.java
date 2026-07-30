package foreverlive.modid.politics.settlement.plot.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructureMathHelper {

    public record PlacementResult(BlockPos finalPos, Rotation rotation) {}

    /**
     * Рассчитывает точную позицию в мире и поворот для NBT-файла, сохраненного лицом на SOUTH.
     */
    public static PlacementResult calculate(BlockPos targetPos, Direction targetFacing, StructureTemplate template) {
        Rotation rotation = getRotationFromSouthBase(targetFacing);

        int sizeX = template.getSize().getX();
        int sizeZ = template.getSize().getZ();

        // Смещение пивота (0,0,0) при повороте вокруг своей оси
        BlockPos finalPos = switch (rotation) {
            case NONE -> targetPos; // SOUTH
            case CLOCKWISE_90 -> targetPos.offset(sizeZ - 1, 0, 0); // WEST
            case CLOCKWISE_180 -> targetPos.offset(sizeX - 1, 0, sizeZ - 1); // NORTH
            case COUNTERCLOCKWISE_90 -> targetPos.offset(0, 0, sizeX - 1); // EAST
        };

        return new PlacementResult(finalPos, rotation);
    }

    /**
     * Превращает целевое направление фасада в поворот Minecraft (База = SOUTH)
     */
    public static Rotation getRotationFromSouthBase(Direction facing) {
        return switch (facing) {
            case SOUTH -> Rotation.NONE;
            case WEST  -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST  -> Rotation.COUNTERCLOCKWISE_90;
            default    -> Rotation.NONE;
        };
    }
}