package com.ocramer.stats.endpoints.player;

import com.ocramer.stats.StatsPlugin;
import com.ocramer.stats.endpoints.player.inventory.PlayerInventory;
import com.ocramer.stats.util.PlayerData;
import com.ocramer.stats.util.PlayerLookup;
import com.ocramer.stats.util.SendResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerHandler implements HttpHandler {

    private final StatsPlugin plugin;
    private final PlayerLookup playerLookup;

    public PlayerHandler(StatsPlugin plugin) {
        this.plugin = plugin;
        this.playerLookup = new PlayerLookup(plugin);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] segments = exchange.getRequestURI().getPath().split("/");

        String response;
        String playerName = segments[2];
        String query = segments[3];

        PlayerInventory inventoryFunctions = new PlayerInventory(plugin);

        PlayerData playerData;
        try {
            playerData = playerLookup.getAsync(playerName).get(5, TimeUnit.SECONDS);
            if (playerData == null) {
                SendResponse.send(exchange, String.format("{\"error\":\"Player %s not found\"}", playerName));
                return;
            }
        } catch (Exception e) {
            SendResponse.send(exchange, "{\"error\":\"Lookup timed out or failed\"}");
            return;
        }

        switch (query.toLowerCase()) {
            case "inventory":
                response = inventoryFunctions.getInventoryJson(playerData);
                SendResponse.send(exchange, response);
                break;
            case "world":
                response = "{\"world\":\"" + plugin.universe.getWorld(UUID.fromString(playerName)).getName() + "\"}";
                SendResponse.send(exchange, response);
                break;
            default:
                response = "{\"error\":\"Invalid query parameter\"}";
                SendResponse.send(exchange, response);
                break;
        }
    }
}
