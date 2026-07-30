package foreverlive.modid.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import foreverlive.modid.politics.settlement.enums.PlotType;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.settlement.plot.builders.MultiFloorGenerator;
import foreverlive.modid.politics.settlement.plot.builders.PlotRenderer;
import foreverlive.modid.politics.settlement.plot.builders.SkeletonLayoutGenerator;
import foreverlive.modid.politics.settlement.plot.layout.BuildingLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BuildTestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("buildtest")
                .then(Commands.argument("width", IntegerArgumentType.integer(4, 30))
                        .then(Commands.argument("depth", IntegerArgumentType.integer(4, 30))
                                .then(Commands.argument("height", IntegerArgumentType.integer(3, 10))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ServerLevel level = player.level();

                                            int width = IntegerArgumentType.getInteger(context, "width");
                                            int depth = IntegerArgumentType.getInteger(context, "depth");
                                            int height = IntegerArgumentType.getInteger(context, "height");

                                            Direction facing = player.getDirection();
                                            BlockPos startPos = player.blockPosition().relative(facing, 3);

                                            BlockPos minPos = startPos;
                                            BlockPos maxPos = startPos.offset(width - 1, height - 1, depth - 1);

                                            BuildingPlot plot = new BuildingPlot(
                                                    player.getUUID(),
                                                    "TestPlot",
                                                    minPos,
                                                    maxPos,
                                                    PlotType.RESIDENTIAL
                                            );

                                            plot.addTag("WOOD");
                                            plot.setFacing(facing.getOpposite());
                                            plot.setAnchorPos(minPos.east(width / 2));

                                            // 1. ГЕНЕРИРУЕМ LAYOUT И ЗАПИСЫВАЕМ В PLOT
                                            BuildingLayout layout = MultiFloorGenerator.generate(plot);

                                            plot.
                                            // 2. РЕНДЕРИМ
                                            SettlementStyle testStyle = SettlementStyle.createVillage();

                                            try {
                                                PlotRenderer.render(level, plot, layout);
                                                player.sendSystemMessage(Component.literal("§aЗдание " + width + "x" + depth + " (H:" + height + ") успешно создано!"));
                                            } catch (Exception e) {
                                                player.sendSystemMessage(Component.literal("§cОшибка генерации: " + e.getMessage()));
                                                e.printStackTrace();
                                            }

                                            return 1;
                                        })))));
    }
}