package com.ocramer.stats;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.ocramer.stats.endpoints.player.PlayerHandler;
import com.ocramer.stats.endpoints.universe.GlobalStatsHandler;
import com.ocramer.stats.util.ZipItemRegistry;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class StatsPlugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("iOSCompanionStats");
    public Universe universe = Universe.get();

    private HttpServer server;

    public StatsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        LOGGER.info("StatsPlugin: Scanning for mod files");
        String filePath = universe.getPath().toAbsolutePath().toString().replace("/universe", "");
        ZipItemRegistry.loadItemsFromZips(filePath);
    }

    @Override
    public void start() {
        try {
            // Start the server on port 8080
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/universe", new GlobalStatsHandler(this));
            server.createContext("/player", new PlayerHandler(this));
            server.setExecutor(null); // creates a default executor
            server.start();

            LOGGER.info("StatsPlugin: HTTP Server started on port 8080. Access at /stats");
        } catch (Exception e) {
            LOGGER.severe("StatsPlugin: Failed to start HTTP server: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("StatsPlugin: HTTP Server stopped.");
        }
        LOGGER.info("StatsPlugin: System stopped.");
    }
}