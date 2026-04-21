package foreverlive.modid.npc.components.proccessing;

import foreverlive.modid.npc.SyncCategory;
import foreverlive.modid.npc.BrainController;
import foreverlive.modid.npc.data.behaviors.Behavior;
import foreverlive.modid.entities.NpcEntity;
import foreverlive.modid.npc.logic.BehaviorRegistry;
import foreverlive.modid.npc.data.motivations.MotivationSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class NpcBrain {
    private final Map<NeedType, MotivationSource> motivations = new EnumMap<>(NeedType.class);
    private final NpcEntity npc;
    private NeedType currentActiveGoal;
    private float lastGoalScore = 0f;

    public void setCurrentGoal(NeedType goal) {
        this.currentActiveGoal = goal;
    }

    public NeedType getCurrentGoal() {
        return this.currentActiveGoal;
    }

    public NpcBrain(NpcEntity npc) {
        this.npc = npc;
        setupMotivations();
        this.currentActiveGoal = NeedType.IDLE;
    }

    public void save(ValueOutput out) {
        if (currentActiveGoal != null) {
            out.putString("current_goal", currentActiveGoal.name());
        }
        out.putFloat("last_goal_score", lastGoalScore);
    }

    public void load(ValueInput in) {
        if (in.contains("current_goal")) {
            try {
                this.currentActiveGoal = NeedType.valueOf(in.getStringOr("current_goal", "IDLE"));
            } catch (IllegalArgumentException e) {
                this.currentActiveGoal = NeedType.IDLE;
            }
        }
        this.lastGoalScore = in.getFloatOr("last_goal_score", 0);
    }

    public void tick() {
        updateActiveBehavior();

        if (npc.level().isClientSide()) return;

        if (BrainController.isPhase(this.npc, 5)) return;
        think();
    }
    public void reThink(){
        stopActiveBehavior();
        think();
    }
    public void think() {
        // Обновляем скор текущей задачи, чтобы понимать, пора ли её бросить
        if (currentActiveGoal != null) {
            MotivationSource currentSource = motivations.get(currentActiveGoal);
            if (currentSource != null) {
                this.lastGoalScore = currentSource.getScore(npc);
            }
        }

        NeedType winner = null;
        float maxScore = -1.0f;

        // Опрашиваем всех провайдеров мотивации
        for (var entry : motivations.entrySet()) {
            float currentScore = entry.getValue().getScore(npc);
            if (currentScore > maxScore) {
                maxScore = currentScore;
                winner = entry.getKey();
            }
        }

        if (winner == null || maxScore <= 0) return;

        // Если победил тот же, кто и был — ничего не меняем
        if (winner == currentActiveGoal) return;

        // Порог переключения (hysteresis), чтобы Григорий не метался
        boolean isFree = currentActiveGoal == null || currentActiveGoal == NeedType.IDLE;
        float threshold = isFree ? 1.0f : 1.15f;

        if (maxScore > (lastGoalScore * threshold)) {
            startBehavior(winner, maxScore);
        }
    }

    private void startBehavior(NeedType type, float score) {
        // Останавливаем старое, если оно есть в реестре
        Behavior oldBehavior = BehaviorRegistry.get(currentActiveGoal);
        if (oldBehavior != null) oldBehavior.onStop(npc);

        currentActiveGoal = type;
        lastGoalScore = score;

        // Запускаем новое
        Behavior nextBehavior = BehaviorRegistry.get(type);
        if (nextBehavior != null) {
            nextBehavior.onStart(npc);
        } else {
            // Если забыл зарегать поведение - падаем в IDLE
            currentActiveGoal = NeedType.IDLE;
        }

        npc.syncTracker.markDirty(SyncCategory.GOAL);
    }
    private void updateActiveBehavior() {
        Behavior behavior = BehaviorRegistry.get(currentActiveGoal);
        if (behavior == null) {
            currentActiveGoal = NeedType.IDLE; // Аварийный сброс
            return;
        }

        behavior.onUpdate(npc);

        if (behavior.isFinished(npc) && currentActiveGoal != NeedType.IDLE) {
            startBehavior(NeedType.IDLE, 5.0f); // Закончил дело — гуляй смело
        }
    }
    public void stopActiveBehavior() {
        if (currentActiveGoal != null) {
            Behavior behavior = BehaviorRegistry.get(currentActiveGoal);
            if (behavior != null) {
                behavior.onStop(npc);
            }
            currentActiveGoal = NeedType.IDLE;
        }
    }
    private void setupMotivations() {
        // Простые нужды (Голод, Жажда)
        for (NeedType type : Arrays.asList(NeedType.HUNGER, NeedType.THIRST)) {
            motivations.put(type, entity -> {
                float deficit = 100 - entity.getNeedValue(type);
                return deficit >= entity.getNeedThreshold(type) ? deficit : 0;
            });
        }

        motivations.put(NeedType.SLEEP, entity -> {
            float val = entity.getNeedValue(NeedType.SLEEP);
            float deficit = 100 - val;
            long time = entity.level().getGameTime() % 24000;
            boolean isNight = time > 13000 && time < 23000;
            boolean isAlreadySleeping = entity.brain.getCurrentGoal() == NeedType.SLEEP;

            // Если он УЖЕ спит, даем огромный приоритет, чтобы досмотрел сон до 100%
            if (isAlreadySleeping) {
                return val < 95 ? 80.0f : 0.0f;
            }

            // Если он не спит:
            float score = deficit;
            if (isNight) {
                // Ночью идем спать, только если устали хотя бы на 30%
                score = (deficit > 30) ? score + 40 : 0;
            } else {
                // Днем - только если валимся с ног
                score -= 50.0f;
            }

            return Math.max(0, score);
        });

        motivations.put(NeedType.IDLE, entity -> 5.0f); // Базовый шум

        motivations.put(NeedType.SAFETY, entity -> {
            var hostiles = npc.memory.nearestEntities.stream()
                    .filter(e -> e instanceof Monster && e.isAlive())
                    .toList();

            if (hostiles.isEmpty()) return 0.0f;

            // Расчет агрессии: чем ближе враг и чем их больше, тем выше Utility
            float threatLevel = 0;
            for (Entity m : hostiles) {
                double dist = npc.distanceToSqr(m);
                threatLevel += (dist < 16) ? 40 : 20; // В упор — страшно
            }

            // Если нас уже бьют, Utility взлетает до небес
            if (npc.getLastHurtByMob() != null && npc.getLastHurtByMob().isAlive()) {
                threatLevel += 50;
            }
            return Math.min(threatLevel, 150.0f);
        });
    }
}
