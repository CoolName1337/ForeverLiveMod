package foreverlive.modid.politics.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WorldBuildQueue {
    private static final Queue<BlockTask> queue = new ConcurrentLinkedQueue<>();

    private static final int MAX_BLOCKS_PER_TICK = 5000;

    public record BlockTask(BlockPos pos, BlockState state) {}

    public static void enqueue(BlockPos pos, BlockState state){
        queue.add(new BlockTask(pos, state));
    }


    public static void tick(ServerLevel world){
        int processed = 0;
        while(!queue.isEmpty() && processed < MAX_BLOCKS_PER_TICK){
            BlockTask task = queue.poll();
            if(task != null){
                world.setBlock(task.pos(), task.state(), 2);
                processed++;
            }
        }
    }
    public static boolean isEmpty() { return queue.isEmpty(); }
}
