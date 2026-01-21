package com.ocramer.stats.handlers;

import com.google.gson.JsonArray;
import com.hypixel.hytale.server.core.universe.world.World;
import com.ocramer.stats.StatsPlugin;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GlobalStatsHandler implements HttpHandler {
    private final StatsPlugin plugin;

    public GlobalStatsHandler(StatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JsonObject responseJson = new JsonObject();

        responseJson.addProperty("status", "online");

        responseJson.addProperty("universePlayers", plugin.universe.getPlayers().size());

        JsonArray worldsJson = new JsonArray();
        Map<String, World> worlds = plugin.universe.getWorlds();
        worlds.forEach((id, world) -> {
            JsonObject worldJson = new JsonObject();
            worldJson.addProperty("id", id);
            worldJson.addProperty("name", world.getName());
            worldJson.addProperty("players", world.getPlayerCount());
            worldsJson.add(worldJson);
        });

        responseJson.add("worlds", worldsJson);

        String response = responseJson.toString();
        sendResponse(exchange, response);
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}