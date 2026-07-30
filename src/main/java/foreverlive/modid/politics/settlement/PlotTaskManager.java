package foreverlive.modid.politics.settlement;

import foreverlive.modid.politics.settlement.enums.RoleTag;
import foreverlive.modid.politics.settlement.enums.TaskType;
import foreverlive.modid.politics.settlement.plot.BuildingPlot;
import net.minecraft.world.level.Level;

import java.util.*;

public class PlotTaskManager {

    private final BuildingPlot parentPlot;
    private final List<PlotTask> activeTasks = new ArrayList<>();

    public PlotTaskManager(BuildingPlot parentPlot) {
        this.parentPlot = parentPlot;
    }

    /**
     * Основной тик менеджера (вызывается из BuildingPlot.tick)
     */
    public void tick(Level level) {
        long currentTime = level.getGameTime();

        // Очистка тасок: таймаут на сброс "зависших" исполнителей
        for (PlotTask task : activeTasks) {
            if (task.isAssigned()) {
                // Если НПС не подтверждал выполнение 20 секунд (400 тиков) — освобождаем задачу
                if (currentTime - task.getLastAssignedTick() > 400) {
                    task.release();
                }
            }
        }
    }

    /**
     * Добавление задачи на доску (с защитой от дубликатов по типу и позиции)
     */
    public boolean addTask(PlotTask newTask) {
        // Проверяем, нет ли уже точно такой же задачи на этой же позиции
        boolean exists = activeTasks.stream().anyMatch(task ->
                task.getType() == newTask.getType() &&
                        Objects.equals(task.getTargetPos(), newTask.getTargetPos())
        );

        if (!exists) {
            activeTasks.add(newTask);
            return true;
        }
        return false;
    }

    /**
     * Выбор самой приоритетной задачи для конкретного НПС
     */
    public Optional<PlotTask> claimBestTask(UUID workerId, Set<RoleTag> workerRoles) {
        return activeTasks.stream()
                .filter(task -> !task.isAssigned()) // Задача еще ни за кем не зафиксирована
                .filter(task -> workerRoles.contains(task.getRequiredRole()) || task.getRequiredRole() == RoleTag.ANY)
                .max(Comparator.comparingInt(PlotTask::getPriority)) // Берем высший приоритет
                .map(task -> {
                    task.assignTo(workerId, 0);
                    return task;
                });
    }

    /**
     * Подтверждение от НПС, что он все еще выполняет задачу (Keep-Alive)
     */
    public void pingTask(UUID taskId, long currentTick) {
        getTaskById(taskId).ifPresent(task -> task.setLastAssignedTick(currentTick));
    }

    /**
     * Завершение задачи (вызывается, когда НПС сделал работу)
     */
    public void completeTask(UUID taskId) {
        getTaskById(taskId).ifPresent(task -> {
            // Если это был ремонт — сообщаем участку, что узел восстановлен
            if (task.getType() == TaskType.REPAIR_STRUCTURE && task.getTargetPos() != null) {
                parentPlot.resolveBlockRepair(task.getTargetPos());
            }
            activeTasks.remove(task);
        });
    }

    /**
     * Сброс задачи (если НПС отвлекся на бой, испугался и т.д.)
     */
    public void abandonTask(UUID taskId) {
        getTaskById(taskId).ifPresent(PlotTask::release);
    }

    /**
     * Проверка: есть ли уже на доске задача определенного типа
     */
    public boolean hasTaskType(TaskType type) {
        return activeTasks.stream().anyMatch(task -> task.getType() == type);
    }

    public Optional<PlotTask> getTaskById(UUID taskId) {
        return activeTasks.stream()
                .filter(task -> task.getTaskId().equals(taskId))
                .findFirst();
    }

    public List<PlotTask> getActiveTasks() {
        return Collections.unmodifiableList(activeTasks);
    }
}