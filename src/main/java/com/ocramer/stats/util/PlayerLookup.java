package com.ocramer.stats.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.ocramer.stats.StatsPlugin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PlayerLookup {

    private final StatsPlugin plugin;

    public PlayerLookup(StatsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Safely retrieves player data by switching to the correct world thread.
     */
    public CompletableFuture<PlayerData> getAsync(String playerName) {
        CompletableFuture<PlayerData> future = new CompletableFuture<>();

        // 1. Find the player reference (Safe off-thread)
        Optional<PlayerRef> playerOpt = plugin.universe.getPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(playerName))
                .findFirst();

        if (playerOpt.isEmpty()) {
            future.complete(null);
            return future;
        }

        PlayerRef playerRef = playerOpt.get();
        World world = plugin.universe.getWorld(playerRef.getWorldUuid());

        if (world == null) {
            future.complete(null);
            return future;
        }

        // 2. Switch to the world thread to safely access components
        world.execute(() -> {
            try {
                Player player = world.getEntityStore().getStore().getComponent(
                        playerRef.getReference(),
                        Player.getComponentType()
                );
                future.complete(new PlayerData(player, world));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}