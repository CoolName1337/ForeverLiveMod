package foreverlive.modid.politics.settlement;

import foreverlive.modid.politics.settlement.enums.RoleTag;
import foreverlive.modid.politics.settlement.enums.TaskType;
import net.minecraft.core.BlockPos;
import java.util.UUID;

public class PlotTask {

    private final UUID taskId;
    private final TaskType type;
    private final BlockPos targetPos; // Целевой блок (или null, если задача глобальная)
    private final RoleTag requiredRole;

    private int priority;
    private UUID assignedNpcId = null;
    private long lastAssignedTick = 0;

    public PlotTask(TaskType type, BlockPos targetPos, RoleTag requiredRole, int priority) {
        this.taskId = UUID.randomUUID();
        this.type = type;
        this.targetPos = targetPos;
        this.requiredRole = requiredRole;
        this.priority = priority;
    }

    public void assignTo(UUID npcId, long currentTick) {
        this.assignedNpcId = npcId;
        this.lastAssignedTick = currentTick;
    }

    public void release() {
        this.assignedNpcId = null;
        this.lastAssignedTick = 0;
    }

    public boolean isAssigned() {
        return assignedNpcId != null;
    }

    // Getters & Setters
    public UUID getTaskId() { return taskId; }
    public TaskType getType() { return type; }
    public BlockPos getTargetPos() { return targetPos; }
    public RoleTag getRequiredRole() { return requiredRole; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public UUID getAssignedNpcId() { return assignedNpcId; }
    public long getLastAssignedTick() { return lastAssignedTick; }
    public void setLastAssignedTick(long lastAssignedTick) { this.lastAssignedTick = lastAssignedTick; }
}