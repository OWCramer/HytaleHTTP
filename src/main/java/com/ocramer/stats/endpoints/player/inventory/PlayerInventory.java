package com.ocramer.stats.endpoints.player.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.ocramer.stats.StatsPlugin;
import com.ocramer.stats.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PlayerInventory {

    private final StatsPlugin plugin;

    public PlayerInventory(StatsPlugin plugin) {
        this.plugin = plugin;
    }


    public String getInventoryJson(PlayerData data) {
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
}