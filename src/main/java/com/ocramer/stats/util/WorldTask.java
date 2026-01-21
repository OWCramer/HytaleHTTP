package com.ocramer.stats.util;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class WorldTask {

    /**
     * Safely executes a task on a specific world thread and returns the result.
     *
     * @param world The world whose thread we need to run on.
     * @param task  The logic to execute.
     * @return A CompletableFuture that will contain the result.
     */
    public static <T> CompletableFuture<T> supply(World world, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();

        world.execute(() -> {
            try {
                future.complete(task.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Helper to run and wait for a result with a default timeout.
     */
    public static <T> T getSync(World world, Supplier<T> task) throws Exception {
        return supply(world, task).get(5, TimeUnit.SECONDS);
    }
}