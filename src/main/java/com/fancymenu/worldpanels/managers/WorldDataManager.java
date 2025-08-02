package com.fancymenu.worldpanels.managers;

import com.fancymenu.worldpanels.data.WorldInfo;
import com.fancymenu.worldpanels.utils.NBTUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages world data discovery, caching, and updates.
 * 
 * Features:
 * - Async world scanning for performance
 * - Intelligent caching with change detection
 * - Automatic refresh when worlds are added/removed
 * - Memory-efficient operation for large world collections
 */
@Environment(EnvType.CLIENT)
public class WorldDataManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldDataManager.class);
    private static WorldDataManager INSTANCE;
    
    private final Map<String, WorldInfo> worldCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private File savesDirectory;
    private long lastScanTime = 0;
    private boolean initialized = false;
    
    // Configuration
    private static final long SCAN_INTERVAL_MS = 5000; // 5 seconds
    private static final long CACHE_VALIDITY_MS = 30000; // 30 seconds
    
    private WorldDataManager() {}
    
    public static synchronized void initialize() {
        if (INSTANCE == null) {
            INSTANCE = new WorldDataManager();
            INSTANCE.init();
        }
    }
    
    public static WorldDataManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("WorldDataManager not initialized! Call initialize() first.");
        }
        return INSTANCE;
    }
    
    private void init() {
        try {
            // Get Minecraft saves directory
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.runDirectory != null) {
                savesDirectory = new File(client.runDirectory, "saves");
            } else {
                // Fallback to default location
                savesDirectory = new File(System.getProperty("user.home"), ".minecraft/saves");
            }
            
            if (!savesDirectory.exists()) {
                LOGGER.warn("Saves directory not found: {}", savesDirectory.getPath());
                savesDirectory.mkdirs();
            }
            
            LOGGER.info("World data manager initialized with saves directory: {}", savesDirectory.getPath());
            
            // Initial scan
            scanWorldsAsync();
            
            // Schedule periodic scans
            executor.scheduleAtFixedRate(this::scanWorldsAsync, SCAN_INTERVAL_MS, SCAN_INTERVAL_MS, TimeUnit.MILLISECONDS);
            
            initialized = true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize WorldDataManager", e);
            throw new RuntimeException("WorldDataManager initialization failed", e);
        }
    }
    
    /**
     * Get all discovered worlds, sorted by last played time (most recent first).
     */
    public List<WorldInfo> getWorlds() {
        if (!initialized) {
            LOGGER.warn("WorldDataManager not initialized, returning empty list");
            return new ArrayList<>();
        }
        
        // Check if we need to refresh cache
        if (System.currentTimeMillis() - lastScanTime > CACHE_VALIDITY_MS) {
            scanWorldsAsync();
        }
        
        List<WorldInfo> worlds = new ArrayList<>(worldCache.values());
        
        // Sort by last played time (most recent first)
        worlds.sort((a, b) -> Long.compare(b.getLastPlayed(), a.getLastPlayed()));
        
        return worlds;
    }
    
    /**
     * Get a specific world by folder name.
     */
    public WorldInfo getWorld(String folderName) {
        return worldCache.get(folderName);
    }
    
    /**
     * Refresh world data immediately (blocking operation).
     */
    public void refreshWorlds() {
        scanWorldsSync();
    }
    
    /**
     * Get world data as a Map for use by FancyMenu elements
     */
    public Map<String, Object> getWorldDataAsMap() {
        try {
            // Build the map directly from our cached worlds
            List<WorldInfo> worlds = getWorlds();
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", worlds.size());
            data.put("lastUpdated", java.time.Instant.now().toString());
            data.put("exportVersion", "1.0.0");
            
            List<Map<String, Object>> worldsData = new ArrayList<>();
            for (int i = 0; i < worlds.size(); i++) {
                WorldInfo world = worlds.get(i);
                Map<String, Object> worldMap = new LinkedHashMap<>();
                
                worldMap.put("index", i);
                worldMap.put("name", world.getWorldName());
                worldMap.put("folder", world.getFolderName());
                worldMap.put("path", world.getFolderPath());
                worldMap.put("gamemode", world.getGameModeDisplay());
                worldMap.put("gamemodeRaw", world.getGameMode());
                worldMap.put("difficulty", world.getDifficultyDisplay());
                worldMap.put("difficultyRaw", world.getDifficulty());
                worldMap.put("version", world.getVersion());
                worldMap.put("seed", world.getSeed());
                worldMap.put("hardcore", world.isHardcore());
                worldMap.put("cheats", world.hasCheats());
                worldMap.put("lastPlayed", world.getFormattedLastPlayed());
                worldMap.put("lastPlayedRaw", world.getLastPlayed());
                worldMap.put("created", world.getFormattedCreationTime());
                worldMap.put("createdRaw", world.getCreationTime());
                worldMap.put("inUse", world.isInUse());
                worldMap.put("status", world.isInUse() ? "In Use" : "Available");
                worldMap.put("hasIcon", world.hasIcon());
                worldMap.put("size", world.getFormattedWorldSize());
                worldMap.put("sizeBytes", world.getWorldSizeBytes());
                worldMap.put("weather", world.getWeatherDisplay());
                worldMap.put("timeOfDay", world.getTimeOfDayDisplay());
                worldMap.put("raining", world.isRaining());
                worldMap.put("thundering", world.isThundering());
                worldMap.put("worldTime", world.getWorldTime());
                worldMap.put("dayTime", world.getDayTime());
                worldMap.put("iconPath", world.getIconPath());
                
                worldsData.add(worldMap);
            }
            
            data.put("worlds", worldsData);
            return data;
            
        } catch (Exception e) {
            LOGGER.error("Failed to get world data as map", e);
            return Map.of("count", 0, "worlds", List.of());
        }
    }
    
    /**
     * Async world scanning for background updates.
     */
    private void scanWorldsAsync() {
        CompletableFuture.runAsync(this::scanWorldsSync, executor);
    }
    
    /**
     * Synchronous world scanning implementation.
     */
    private void scanWorldsSync() {
        try {
            if (!savesDirectory.exists()) {
                LOGGER.debug("Saves directory does not exist: {}", savesDirectory.getPath());
                return;
            }
            
            File[] worldFolders = savesDirectory.listFiles(File::isDirectory);
            if (worldFolders == null) {
                LOGGER.debug("No world folders found in saves directory");
                return;
            }
            
            Set<String> foundWorlds = new HashSet<>();
            int scannedCount = 0;
            int updatedCount = 0;
            
            for (File worldFolder : worldFolders) {
                try {
                    String folderName = worldFolder.getName();
                    foundWorlds.add(folderName);
                    
                    // Check if we need to update this world's data
                    WorldInfo existingWorld = worldCache.get(folderName);
                    long folderModified = worldFolder.lastModified();
                    
                    if (existingWorld == null || existingWorld.getFolderModified() != folderModified) {
                        WorldInfo worldInfo = scanWorldFolder(worldFolder);
                        if (worldInfo != null) {
                            worldCache.put(folderName, worldInfo);
                            updatedCount++;
                        }
                    }
                    scannedCount++;
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to scan world folder: {}", worldFolder.getName(), e);
                }
            }
            
            // Remove worlds that no longer exist
            Set<String> toRemove = new HashSet<>(worldCache.keySet());
            toRemove.removeAll(foundWorlds);
            
            for (String removedWorld : toRemove) {
                worldCache.remove(removedWorld);
                LOGGER.debug("Removed deleted world from cache: {}", removedWorld);
            }
            
            lastScanTime = System.currentTimeMillis();
            
            if (updatedCount > 0 || !toRemove.isEmpty()) {
                LOGGER.debug("World scan complete: {} scanned, {} updated, {} removed", 
                           scannedCount, updatedCount, toRemove.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to scan worlds", e);
        }
    }
    
    /**
     * Scan a single world folder and extract world information.
     */
    private WorldInfo scanWorldFolder(File worldFolder) {
        try {
            File levelDat = new File(worldFolder, "level.dat");
            if (!levelDat.exists()) {
                LOGGER.debug("No level.dat found in world folder: {}", worldFolder.getName());
                return null;
            }
            
            // Parse NBT data
            WorldInfo.Builder builder = new WorldInfo.Builder()
                .folderName(worldFolder.getName())
                .folderPath(worldFolder.getAbsolutePath())
                .folderModified(worldFolder.lastModified());
            
            // Extract data from level.dat
            NBTUtils.extractWorldData(levelDat, builder);
            
            // Check for world icon
            File iconFile = new File(worldFolder, "icon.png");
            if (iconFile.exists()) {
                builder.iconPath(iconFile.getAbsolutePath());
            }
            
            // Check if world is currently in use
            File sessionLock = new File(worldFolder, "session.lock");
            builder.isInUse(sessionLock.exists());
            
            // Calculate world size
            builder.worldSizeBytes(calculateWorldSize(worldFolder));
            
            return builder.build();
            
        } catch (Exception e) {
            LOGGER.warn("Failed to scan world folder: {}", worldFolder.getName(), e);
            return null;
        }
    }
    
    /**
     * Calculate world size by summing all files.
     */
    private long calculateWorldSize(File worldFolder) {
        try {
            return calculateDirectorySize(worldFolder);
        } catch (Exception e) {
            LOGGER.debug("Failed to calculate world size for {}", worldFolder.getName(), e);
            return 0;
        }
    }
    
    /**
     * Recursively calculate directory size.
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateDirectorySize(file);
                } else {
                    size += file.length();
                }
            }
        }
        
        return size;
    }
    
    /**
     * Shutdown the world data manager and cleanup resources.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        worldCache.clear();
        initialized = false;
        LOGGER.info("WorldDataManager shutdown complete");
    }
    
    /**
     * Get the saves directory being monitored.
     */
    public File getSavesDirectory() {
        return savesDirectory;
    }
    
    /**
     * Check if the manager is properly initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
}