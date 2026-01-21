package com.ocramer.stats.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.ocramer.stats.StatsPlugin;
import com.ocramer.stats.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PlayerInventoryHandler implements HttpHandler {

    private final StatsPlugin plugin;
    private final PlayerLookup playerLookup;

    public PlayerInventoryHandler(StatsPlugin plugin) {
        this.plugin = plugin;
        this.playerLookup = new PlayerLookup(plugin);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] segments = exchange.getRequestURI().getPath().split("/");

        String response;
        String playerName = segments[2];
        String query = segments[3];

        PlayerData playerData;
        try {
            playerData = playerLookup.getAsync(playerName).get(5, TimeUnit.SECONDS);
            if (playerData == null) {
                sendResponse(exchange, String.format("{\"error\":\"Player %s not found\"}", playerName));
                return;
            }
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\":\"Lookup timed out or failed\"}");
            return;
        }

        switch (query.toLowerCase()) {
            case "inventory":
                response = getInventoryJson(playerData);
                sendResponse(exchange, response);
                break;
            case "world":
                response = "{\"world\":\"" + plugin.universe.getWorld(UUID.fromString(playerName)).getName() + "\"}";
                sendResponse(exchange, response);
                break;
            default:
                response = "{\"error\":\"Invalid query parameter\"}";
                sendResponse(exchange, response);
                break;
        }
    }


    private String getInventoryJson(PlayerData data) {
        try {
            JsonObject inventoryJson = new JsonObject();

            JsonArray itemsJson = WorldTask.getSync(data.world(), () -> {
                Inventory inventory = data.player().getInventory();
                JsonArray items = new JsonArray();
                inventory.getStorage().forEach((index, item) -> {
                    JsonObject itemJson = new JsonObject();
                    itemJson.addProperty("id", item.getItemId());
                    itemJson.addProperty("slot", index + 1);
                    itemJson.addProperty("quantity", item.getQuantity());
                    itemJson.addProperty("translation_key", item.getItem().getTranslationKey());
                    String icon = getImage(item.getItem().getIcon(), item.getItemId());
                    if (icon != null) itemJson.addProperty("icon", icon);
                    items.add(itemJson);
                });

                return items;

            });

            JsonArray hotbarJson = WorldTask.getSync(data.world(), () -> {
                ItemContainer inventory = data.player().getInventory().getHotbar();
                JsonArray items = new JsonArray();
                inventory.forEach((index, item) -> {
                    JsonObject itemJson = new JsonObject();
                    itemJson.addProperty("id", item.getItemId());
                    itemJson.addProperty("slot", index + 1);
                    itemJson.addProperty("quantity", item.getQuantity());
                    itemJson.addProperty("translation_key", item.getItem().getTranslationKey());
                    String icon = getImage(item.getItem().getIcon(), item.getItemId());
                    if (icon != null) itemJson.addProperty("icon", icon);
                    items.add(itemJson);
                });

                return items;

            });

            JsonArray armorJson = WorldTask.getSync(data.world(), () -> {
                ItemContainer inventory = data.player().getInventory().getArmor();
                JsonArray items = new JsonArray();
                inventory.forEach((index, item) -> {
                    JsonObject itemJson = new JsonObject();
                    itemJson.addProperty("id", item.getItemId());
                    itemJson.addProperty("slot", index + 1);
                    itemJson.addProperty("quantity", item.getQuantity());
                    itemJson.addProperty("translation_key", item.getItem().getTranslationKey());
                    String icon = getImage(item.getItem().getIcon(), item.getItemId());
                    if (icon != null) itemJson.addProperty("icon", icon);
                    items.add(itemJson);
                });

                return items;

            });

            inventoryJson.add("inventoryItems", itemsJson);
            inventoryJson.add("hotbarItems", hotbarJson);
            inventoryJson.add("armorItems", armorJson);

            return inventoryJson.toString();

        } catch (Exception e) {
            return String.format("{\"error\":\"World thread error: %s\"}", e.getMessage());
        }
    }

    private String getImage(String itemIcon, String itemId) {
        try {
            String universePath = plugin.universe.getPath().toAbsolutePath().toString().replace("/universe", "");

            File assetsZipFile = new File(universePath + "/assets.zip"); // dynamic path
            ZipFile zip = new ZipFile(assetsZipFile);

            ZipEntry entry = zip.getEntry("Common/" + itemIcon);
            byte[] bytes = null;
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    if (in == null) return null;
                    bytes = in.readAllBytes(); // now you have the PNG bytes
                    // you can serve these via HTTP, or save to disk, etc.
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ZipItemRegistry.ItemData itemData = ZipItemRegistry.getItem(itemId);
                if (itemData == null) return null;
                return ZipUtils.getIconBase64(itemData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}