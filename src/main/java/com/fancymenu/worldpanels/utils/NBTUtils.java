package com.fancymenu.worldpanels.utils;

import com.fancymenu.worldpanels.data.WorldInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class for extracting world data from NBT files.
 * 
 * Uses Minecraft's built-in NBT handling to parse level.dat files
 * and extract all relevant world information for display in world panels.
 */
public class NBTUtils {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NBTUtils.class);
    
    /**
     * Extract world data from level.dat file and populate the WorldInfo builder.
     * 
     * @param levelDat The level.dat file to parse
     * @param builder The WorldInfo.Builder to populate
     */
    public static void extractWorldData(File levelDat, WorldInfo.Builder builder) {
        try (FileInputStream fis = new FileInputStream(levelDat)) {
            
            // Read NBT data from level.dat using Minecraft's NBT system
            NbtCompound root = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes());
            
            if (root == null) {
                LOGGER.warn("Failed to read NBT data from {}", levelDat.getPath());
                return;
            }
            
            // Get the Data compound tag which contains all world information
            NbtCompound dataTag = root.getCompound("Data");
            if (dataTag == null || dataTag.isEmpty()) {
                LOGGER.warn("No Data tag found in level.dat: {}", levelDat.getPath());
                return;
            }
            
            // Extract basic world information
            extractBasicInfo(dataTag, builder);
            
            // Extract game settings
            extractGameSettings(dataTag, builder);
            
            // Extract timestamps
            extractTimestamps(dataTag, builder);
            
            // Extract world state
            extractWorldState(dataTag, builder);
            
            // Calculate world size
            long worldSize = calculateWorldSize(levelDat.getParentFile());
            builder.worldSizeBytes(worldSize);
            
            LOGGER.debug("Successfully extracted world data from {}", levelDat.getPath());
            
        } catch (Exception e) {
            LOGGER.error("Failed to extract world data from {}", levelDat.getPath(), e);
            // Don't throw - we want to continue with partial data
        }
    }
    
    /**
     * Extract basic world information (name, version, seed).
     */
    private static void extractBasicInfo(NbtCompound dataTag, WorldInfo.Builder builder) {
        // World name
        if (dataTag.contains("LevelName")) {
            String levelName = dataTag.getString("LevelName");
            if (levelName != null && !levelName.trim().isEmpty()) {
                builder.worldName(levelName);
            }
        }
        
        // Version information
        if (dataTag.contains("Version")) {
            NbtCompound versionTag = dataTag.getCompound("Version");
            if (versionTag.contains("Name")) {
                String versionName = versionTag.getString("Name");
                if (versionName != null) {
                    builder.version(versionName);
                }
            }
        }
        
        // World seed
        if (dataTag.contains("RandomSeed")) {
            long seed = dataTag.getLong("RandomSeed");
            builder.seed(seed);
        }
    }
    
    /**
     * Extract game settings (mode, difficulty, hardcore, cheats).
     */
    private static void extractGameSettings(NbtCompound dataTag, WorldInfo.Builder builder) {
        // Game mode
        if (dataTag.contains("GameType")) {
            int gameType = dataTag.getInt("GameType");
            String gameMode = convertGameType(gameType);
            builder.gameMode(gameMode);
        }
        
        // Difficulty
        if (dataTag.contains("Difficulty")) {
            byte difficulty = dataTag.getByte("Difficulty");
            String difficultyName = convertDifficulty(difficulty);
            builder.difficulty(difficultyName);
        }
        
        // Hardcore mode
        if (dataTag.contains("hardcore")) {
            boolean hardcore = dataTag.getBoolean("hardcore");
            builder.hardcore(hardcore);
        }
        
        // Cheats/commands allowed
        if (dataTag.contains("allowCommands")) {
            boolean allowCommands = dataTag.getBoolean("allowCommands");
            builder.cheats(allowCommands);
        }
    }
    
    /**
     * Extract timestamp information.
     */
    private static void extractTimestamps(NbtCompound dataTag, WorldInfo.Builder builder) {
        // Last played time
        if (dataTag.contains("LastPlayed")) {
            long lastPlayed = dataTag.getLong("LastPlayed");
            builder.lastPlayed(lastPlayed);
        }
        
        // Creation time (not always available)
        if (dataTag.contains("creationTime")) {
            long creationTime = dataTag.getLong("creationTime");
            builder.creationTime(creationTime);
        }
    }
    
    /**
     * Extract current world state (time, weather).
     */
    private static void extractWorldState(NbtCompound dataTag, WorldInfo.Builder builder) {
        // World time (total ticks)
        if (dataTag.contains("Time")) {
            long worldTime = dataTag.getLong("Time");
            builder.worldTime(worldTime);
        }
        
        // Day time (time of day)
        if (dataTag.contains("DayTime")) {
            long dayTime = dataTag.getLong("DayTime");
            builder.dayTime(dayTime);
        }
        
        // Weather state
        if (dataTag.contains("raining")) {
            boolean raining = dataTag.getBoolean("raining");
            builder.raining(raining);
        }
        
        if (dataTag.contains("thundering")) {
            boolean thundering = dataTag.getBoolean("thundering");
            builder.thundering(thundering);
        }
    }
    
    /**
     * Calculate approximate world size by summing directory contents.
     */
    private static long calculateWorldSize(File worldDir) {
        try {
            return calculateDirectorySize(worldDir);
        } catch (Exception e) {
            LOGGER.debug("Failed to calculate world size for {}", worldDir.getPath(), e);
            return 0;
        }
    }
    
    /**
     * Recursively calculate directory size.
     */
    private static long calculateDirectorySize(File directory) {
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
     * Convert numeric game type to string.
     */
    private static String convertGameType(int gameType) {
        switch (gameType) {
            case 0: return "Survival";
            case 1: return "Creative";
            case 2: return "Adventure";
            case 3: return "Spectator";
            default: return "Unknown";
        }
    }
    
    /**
     * Convert numeric difficulty to string.
     */
    private static String convertDifficulty(byte difficulty) {
        switch (difficulty) {
            case 0: return "Peaceful";
            case 1: return "Easy";
            case 2: return "Normal";
            case 3: return "Hard";
            default: return "Unknown";
        }
    }
}