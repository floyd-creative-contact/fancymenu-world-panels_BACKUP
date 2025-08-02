package com.fancymenu.worldpanels.exporters;

import com.fancymenu.worldpanels.data.WorldInfo;
import com.fancymenu.worldpanels.managers.WorldDataManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Exports world data to JSON files for FancyMenu v3 integration.
 * 
 * Creates JSON files that can be read by FancyMenu's JSON placeholder:
 * {"placeholder":"json","values":{"source":"/config/fancymenu/assets/worlddata.json","json_path":"$.worlds[0].name"}}
 */
@Environment(EnvType.CLIENT)
public class WorldDataExporter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldDataExporter.class);
    private static WorldDataExporter INSTANCE;
    
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private File exportDirectory;
    private File worldDataFile;
    private boolean initialized = false;
    
    // Configuration
    private static final long EXPORT_INTERVAL_MS = 5000; // 5 seconds
    private static final String EXPORT_FILENAME = "worlddata.json";
    
    private WorldDataExporter() {}
    
    public static synchronized void initialize() {
        if (INSTANCE == null) {
            INSTANCE = new WorldDataExporter();
            INSTANCE.init();
        }
    }
    
    public static WorldDataExporter getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("WorldDataExporter not initialized!");
        }
        return INSTANCE;
    }
    
    private void init() {
        try {
            // Create export directory structure
            MinecraftClient client = MinecraftClient.getInstance();
            File configDir = new File(client.runDirectory, "config");
            File fancyMenuDir = new File(configDir, "fancymenu");
            File assetsDir = new File(fancyMenuDir, "assets");
            
            exportDirectory = assetsDir;
            worldDataFile = new File(exportDirectory, EXPORT_FILENAME);
            
            // Ensure directory exists
            if (!exportDirectory.exists()) {
                exportDirectory.mkdirs();
                LOGGER.info("Created FancyMenu assets directory: {}", exportDirectory.getPath());
            }
            
            LOGGER.info("World data will be exported to: {}", worldDataFile.getPath());
            
            // Initial export
            exportWorldData();
            
            // Schedule periodic exports
            executor.scheduleAtFixedRate(this::exportWorldData, EXPORT_INTERVAL_MS, EXPORT_INTERVAL_MS, TimeUnit.MILLISECONDS);
            
            initialized = true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize WorldDataExporter", e);
            throw new RuntimeException("WorldDataExporter initialization failed", e);
        }
    }
    
    /**
     * Export world data to JSON file.
     */
    private void exportWorldData() {
        try {
            if (!WorldDataManager.getInstance().isInitialized()) {
                return;
            }
            
            List<WorldInfo> worlds = WorldDataManager.getInstance().getWorlds();
            
            // Create JSON structure
            JsonObject root = new JsonObject();
            
            // Metadata
            root.addProperty("count", worlds.size());
            root.addProperty("lastUpdated", Instant.now().toString());
            root.addProperty("exportVersion", "1.0.0");
            
            // Worlds array
            JsonArray worldsArray = new JsonArray();
            
            for (int i = 0; i < worlds.size(); i++) {
                WorldInfo world = worlds.get(i);
                JsonObject worldObj = createWorldJson(world, i);
                worldsArray.add(worldObj);
            }
            
            root.add("worlds", worldsArray);
            
            // Write to file atomically
            File tempFile = new File(worldDataFile.getParent(), EXPORT_FILENAME + ".tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                gson.toJson(root, writer);
            }
            
            // Atomic rename
            if (worldDataFile.exists()) {
                worldDataFile.delete();
            }
            tempFile.renameTo(worldDataFile);
            
            LOGGER.debug("Exported data for {} worlds to {}", worlds.size(), worldDataFile.getName());
            
        } catch (Exception e) {
            LOGGER.error("Failed to export world data", e);
        }
    }
    
    /**
     * Create JSON object for a single world.
     */
    private JsonObject createWorldJson(WorldInfo world, int index) {
        JsonObject worldObj = new JsonObject();
        
        // Basic info
        worldObj.addProperty("index", index);
        worldObj.addProperty("name", world.getWorldName());
        worldObj.addProperty("folder", world.getFolderName());
        worldObj.addProperty("path", world.getFolderPath());
        
        // Game info
        worldObj.addProperty("gamemode", world.getGameModeDisplay());
        worldObj.addProperty("gamemodeRaw", world.getGameMode());
        worldObj.addProperty("difficulty", world.getDifficultyDisplay());
        worldObj.addProperty("difficultyRaw", world.getDifficulty());
        worldObj.addProperty("version", world.getVersion());
        worldObj.addProperty("seed", world.getSeed());
        worldObj.addProperty("hardcore", world.isHardcore());
        worldObj.addProperty("cheats", world.hasCheats());
        
        // Timestamps
        worldObj.addProperty("lastPlayed", world.getFormattedLastPlayed());
        worldObj.addProperty("lastPlayedRaw", world.getLastPlayed());
        worldObj.addProperty("created", world.getFormattedCreationTime());
        worldObj.addProperty("createdRaw", world.getCreationTime());
        
        // Status
        worldObj.addProperty("inUse", world.isInUse());
        worldObj.addProperty("status", world.isInUse() ? "In Use" : "Available");
        worldObj.addProperty("hasIcon", world.hasIcon());
        worldObj.addProperty("size", world.getFormattedWorldSize());
        worldObj.addProperty("sizeBytes", world.getWorldSizeBytes());
        
        // World state
        worldObj.addProperty("weather", world.getWeatherDisplay());
        worldObj.addProperty("timeOfDay", world.getTimeOfDayDisplay());
        worldObj.addProperty("raining", world.isRaining());
        worldObj.addProperty("thundering", world.isThundering());
        worldObj.addProperty("worldTime", world.getWorldTime());
        worldObj.addProperty("dayTime", world.getDayTime());
        
        // Paths
        if (world.getIconPath() != null) {
            worldObj.addProperty("iconPath", world.getIconPath());
        } else {
            worldObj.addProperty("iconPath", "");
        }
        
        return worldObj;
    }
    
    /**
     * Export world data in multiple formats optimized for FancyMenu
     */
    public void exportEnhancedFormats() {
        try {
            List<WorldInfo> worlds = WorldDataManager.getInstance().getWorlds();
            
            // Export standard format (existing functionality)
            exportWorldData();
            
            LOGGER.info("Enhanced world data export completed");
            
        } catch (Exception e) {
            LOGGER.error("Failed to export enhanced world data formats", e);
        }
    }
    
    /**
     * Force immediate export.
     */
    public void forceExport() {
        if (initialized) {
            exportWorldData();
        }
    }
    
    /**
     * Shutdown the exporter.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Final export
        if (initialized) {
            exportWorldData();
            LOGGER.info("Final world data export completed");
        }
        
        initialized = false;
        LOGGER.info("WorldDataExporter shutdown complete");
    }
    
    /**
     * Get the export file path for FancyMenu.
     */
    public String getExportFilePath() {
        return worldDataFile != null ? "/config/fancymenu/assets/" + EXPORT_FILENAME : null;
    }
    
    /**
     * Get the absolute export file path.
     */
    public String getAbsoluteExportFilePath() {
        return worldDataFile != null ? worldDataFile.getAbsolutePath() : null;
    }
    
    /**
     * Check if the exporter is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}