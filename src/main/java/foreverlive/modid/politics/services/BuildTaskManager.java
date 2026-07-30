package foreverlive.modid.politics.services;

import foreverlive.modid.politics.settlement.Settlement;
import foreverlive.modid.politics.settlement.SettlementStyle;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public class BuildTaskManager {

    private static final List<SettlementBuildTask> activeTasks = new ArrayList<>();

    // Запуск постройки города
    public static void startBuild(ServerLevel world, Settlement settlement, SettlementStyle style) {
        activeTasks.add(new SettlementBuildTask(world, settlement, style));
    }
    // Вызывается каждый тик на стороне сервера
    public static void onServerTick(ServerLevel world) {
        // 1. Сначала выставляем порцию из 300 блоков в физический мир
        WorldBuildQueue.tick(world);

        // 2. Затем проверяем статус постройки городов
        BuildTaskManager.tick(world);
    }
    // Вызывать каждый тик сервера
    public static void tick(ServerLevel world) {
        if (activeTasks.isEmpty()) return;

        // Продвигаем задачи, удаляем те, что завершились
        activeTasks.removeIf(task -> {
            task.tick();
            return task.isFinished();
        });
    }
}