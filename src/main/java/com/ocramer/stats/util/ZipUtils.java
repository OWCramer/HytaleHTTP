package com.ocramer.stats.util;

import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {

    /**
     * Returns the icon of an item as a Base64 PNG string
     *
     * @param item the ItemData containing zipPath and internalIconPath
     * @return Base64 string of the PNG, or null if not found/error
     */
    public static String getIconBase64(ZipItemRegistry.ItemData item) {
        if (item == null || item.zipPath == null || item.internalIconPath == null) return null;

        try (ZipFile zip = new ZipFile(new File(item.zipPath))) {
            ZipEntry entry = zip.getEntry(item.internalIconPath);
            if (entry == null) return null;

            try (InputStream in = zip.getInputStream(entry)) {
                byte[] bytes = in.readAllBytes();
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            System.err.println("Failed reading icon for " + item.id + ": " + e.getMessage());
            return null;
        }
    }
}
