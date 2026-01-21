package com.ocramer.stats.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipItemRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ItemData> ITEMS = new HashMap<>();
    private static final String CACHE_DIR = "cache";
    private static final String CACHE_FILE = "item_registry_cache.json";
    private static String SERVER_ROOT;

    private ZipItemRegistry() {
    }

    // =========================================================
    // Public API
    // =========================================================

    public static Map<String, ItemData> loadItemsFromZips(String serverRoot) {
        SERVER_ROOT = serverRoot;
        ITEMS.clear();

        try {
            Path cachePath = getCachePath();
            String fingerprint = computeModsFingerprint();

            if (Files.exists(cachePath)) {
                CacheFile cache = readCache(cachePath);
                if (cache != null && fingerprint.equals(cache.modsFingerprint)) {
                    restoreFromCache(cache);
                    System.out.println("Loaded item registry from cache.");
                    return ITEMS;
                }
            }

            scanMods();
            writeCache(fingerprint);

        } catch (Exception e) {
            System.err.println("Failed loading item registry, falling back to ZIP scan.");
            scanMods();
        }

        System.out.println("Loaded " + ITEMS.size() + " items.");
        return ITEMS;
    }

    public static ItemData getItem(String id) {
        return ITEMS.get(id);
    }

    // =========================================================
    // ZIP SCANNING
    // =========================================================

    private static void scanMods() {
        Path modsPath = Paths.get(SERVER_ROOT, "mods");
        File modsFolder = modsPath.toFile();

        if (!modsFolder.exists() || !modsFolder.isDirectory()) {
            System.err.println("Mods directory missing.");
            return;
        }

        File[] zips = modsFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".zip"));
        if (zips == null) return;

        for (File zipFile : zips) {
            loadZip(zipFile);
        }
    }

    private static void loadZip(File zipFile) {
        String modId = zipFile.getName().replace(".zip", "");

        try (ZipFile zip = new ZipFile(zipFile)) {
            zip.stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> isItemJson(e.getName()))
                    .forEach(e -> parseItem(zip, e, modId, zipFile.getAbsolutePath()));
        } catch (IOException e) {
            System.err.println("Failed reading zip: " + zipFile.getName());
        }
    }

    private static boolean isItemJson(String path) {
        String p = path.replace('\\', '/');
        return p.endsWith(".json") &&
                p.contains("/Item/Items/") &&
                !p.startsWith("__MACOSX/");
    }

    private static void parseItem(ZipFile zip, ZipEntry entry, String modId, String zipAbsolutePath) {
        try (InputStream in = zip.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            JsonReader jr = new JsonReader(reader);
            jr.setLenient(true);

            JsonObject obj = JsonParser.parseReader(jr).getAsJsonObject();

            String itemId = obj.has("Id")
                    ? obj.get("Id").getAsString()
                    : entry.getName()
                    .substring(entry.getName().lastIndexOf('/') + 1)
                    .replace(".json", "");

            JsonObject tp = obj.has("TranslationProperties")
                    ? obj.getAsJsonObject("TranslationProperties")
                    : null;

            String nameKey = tp != null && tp.has("Name")
                    ? tp.get("Name").getAsString()
                    : itemId;

            String displayName = nameKey; // raw from JSON

            String iconPath = obj.has("Icon") ? obj.get("Icon").getAsString() : null;
            String internalIconPath = iconPath != null ? "Common/" + iconPath : null;

            ItemData data = new ItemData(
                    itemId,
                    modId,
                    entry.getName().replace('\\', '/'),
                    nameKey,
                    displayName,
                    internalIconPath,
                    zipAbsolutePath,
                    obj
            );

            ITEMS.put(itemId, data);

        } catch (Exception e) {
            System.out.println("Skipping invalid JSON: " + entry.getName());
        }
    }

    // =========================================================
    // CACHE
    // =========================================================

    private static Path getCachePath() throws IOException {
        Path dir = Paths.get(SERVER_ROOT, CACHE_DIR);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir.resolve(CACHE_FILE);
    }

    private static void writeCache(String fingerprint) throws IOException {
        CacheFile cache = new CacheFile();
        cache.modsFingerprint = fingerprint;
        cache.items = ITEMS;

        Files.writeString(
                getCachePath(),
                GSON.toJson(cache),
                StandardCharsets.UTF_8
        );
    }

    private static CacheFile readCache(Path path) {
        try {
            return GSON.fromJson(Files.readString(path), CacheFile.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static void restoreFromCache(CacheFile cache) {
        ITEMS.clear();
        ITEMS.putAll(cache.items);
    }

    private static String computeModsFingerprint() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        Path mods = Paths.get(SERVER_ROOT, "mods");

        Files.list(mods)
                .filter(p -> p.toString().endsWith(".zip"))
                .sorted()
                .forEach(p -> {
                    try {
                        md.update(p.getFileName().toString().getBytes());
                        md.update(Long.toString(p.toFile().length()).getBytes());
                        md.update(Long.toString(p.toFile().lastModified()).getBytes());
                    } catch (Exception ignored) {
                    }
                });

        return Base64.getEncoder().encodeToString(md.digest());
    }

    // =========================================================
    // DATA MODELS
    // =========================================================

    private static final class CacheFile {
        String modsFingerprint;
        Map<String, ItemData> items;
    }

    public static final class ItemData {
        public final String id;
        public final String modId;
        public final String jsonPath;

        public final String nameKey;
        public final String displayName;

        // Path inside ZIP (for your ZipFile reads)
        public final String internalIconPath;

        // Real ZIP file on disk
        public final String zipPath;

        public final JsonObject raw;

        public ItemData(
                String id,
                String modId,
                String jsonPath,
                String nameKey,
                String displayName,
                String internalIconPath,
                String zipPath,
                JsonObject raw
        ) {
            this.id = id;
            this.modId = modId;
            this.jsonPath = jsonPath;
            this.nameKey = nameKey;
            this.displayName = displayName;
            this.internalIconPath = internalIconPath;
            this.zipPath = zipPath;
            this.raw = raw;
        }

        public ItemData() {
            this.id = null;
            this.modId = null;
            this.jsonPath = null;
            this.nameKey = null;
            this.displayName = null;
            this.internalIconPath = null;
            this.zipPath = null;
            this.raw = null;
        }
    }
}
