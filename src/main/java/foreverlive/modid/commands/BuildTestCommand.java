package foreverlive.modid.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import foreverlive.modid.politics.settlement.enums.PlotType;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import foreverlive.modid.politics.settlement.SettlementStyle;
import foreverlive.modid.politics.settlement.plot.builders.ModularBuildingGenerator;
import foreverlive.modid.politics.settlement.plot.builders.PlotRenderer;
import foreverlive.modid.politics.settlement.plot.layout.BuildingLayout;
import foreverlive.modid.politics.settlement.plot.layout.FloorConfig;
import foreverlive.modid.politics.settlement.plot.layout.FloorLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

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
                                            int heightPerFloor = IntegerArgumentType.getInteger(context, "height");

                                            Direction facing = player.getDirection();
                                            // Спавним постройку в 3 блоках перед игроком
                                            BlockPos minPos = player.blockPosition().relative(facing, 3);

                                            // 1. КОНФИГУРИРУЕМ ЭТАЖИ (Например, генерируем 2-этажный дом для теста)
                                            List<FloorLayout> floors = new ArrayList<>();

                                            // 1-й этаж: Каменный цоколь с дверью
                                            FloorConfig groundConfig = new FloorConfig(
                                                    heightPerFloor,
                                                    "residential",
                                                    "cobblestone",
                                                    true,  // allowDoors
                                                    0      // overhangRadius
                                            );
                                            floors.add(new FloorLayout(0, groundConfig));

                                            // 2-й этаж: Деревянные стены без дверей (только окна)
                                            FloorConfig upperConfig = new FloorConfig(
                                                    heightPerFloor,
                                                    "residential",
                                                    "oak_wood",
                                                    false, // allowDoors = false
                                                    0
                                            );
                                            floors.add(new FloorLayout(1, upperConfig));

                                            // 2. СОЗДАЕМ ПЛОТ С ЭТАЖАМИ
                                            BuildingPlot plot = new BuildingPlot(
                                                    minPos,
                                                    width,
                                                    depth,
                                                    facing.getOpposite(), // Фасад смотрит на игрока
                                                    floors
                                            );

                                            try {
                                                // 3. ГЕНЕРИРУЕМ LAYOUT (Заполняем FloorLayout-ы элементами)
                                                ModularBuildingGenerator.generateBuilding(plot);

                                                // 4. РЕНДЕРИМ СТРУКТУРУ В МИР
                                                PlotRenderer.render(level, plot);

                                                player.sendSystemMessage(Component.literal(
                                                        "§aЗдание " + width + "x" + depth +
                                                                " (" + floors.size() + " этажа по " + heightPerFloor + " б.) успешно построено!"
                                                ));
                                            } catch (Exception e) {
                                                player.sendSystemMessage(Component.literal("§cОшибка генерации: " + e.getMessage()));
                                                e.printStackTrace();
                                            }

                                            return 1;
                                        })))));
    }
}