package com.fancymenu.worldpanels.placeholders;

import com.fancymenu.worldpanels.data.WorldInfo;
import com.fancymenu.worldpanels.managers.WorldDataManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses world-related placeholders for FancyMenu integration.
 * Compatible with existing WorldDataManager and WorldInfo architecture.
 */
public class WorldPlaceholderParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldPlaceholderParser.class);
    
    // Placeholder patterns
    private static final Pattern WORLD_INDEX_PATTERN = Pattern.compile("worldpanels_world_(\\d+)_(.+)");
    private static final Pattern WORLD_NAME_PATTERN = Pattern.compile("worldpanels_world_([^_]+)_(.+)");
    private static final Pattern WORLD_LIST_PATTERN = Pattern.compile("worldpanels_list_(.+)");
    private static final Pattern WORLD_COUNT_PATTERN = Pattern.compile("worldpanels_count");
    
    private static Map<String, Object> cachedWorldData = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 5000; // 5 seconds
    
    /**
     * Parse a placeholder and return its value
     */
    public static String parsePlaceholder(String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return placeholder;
        }
        
        // Remove prefix/suffix if present
        String cleanPlaceholder = placeholder.replace("%", "").replace("{", "").replace("}", "");
        
        try {
            // Update cache if needed
            updateCacheIfNeeded();
            
            // Try different placeholder patterns
            String result = parseWorldIndexPlaceholder(cleanPlaceholder);
            if (result != null) return result;
            
            result = parseWorldNamePlaceholder(cleanPlaceholder);
            if (result != null) return result;
            
            result = parseWorldListPlaceholder(cleanPlaceholder);
            if (result != null) return result;
            
            result = parseWorldCountPlaceholder(cleanPlaceholder);
            if (result != null) return result;
            
            // Try loading from JSON files
            result = parseFromJsonFiles(cleanPlaceholder);
            if (result != null) return result;
            
        } catch (Exception e) {
            LOGGER.debug("Failed to parse placeholder: {}", cleanPlaceholder, e);
        }
        
        // Return original placeholder if not found
        return "%" + cleanPlaceholder + "%";
    }
    
    /**
     * Parse world placeholders by index (worldpanels_world_1_name)
     */
    private static String parseWorldIndexPlaceholder(String placeholder) {
        Matcher matcher = WORLD_INDEX_PATTERN.matcher(placeholder);
        if (!matcher.matches()) return null;
        
        try {
            int worldIndex = Integer.parseInt(matcher.group(1)) - 1; // Convert to 0-based
            String property = matcher.group(2);
            
            Object value = getWorldPropertyByIndex(worldIndex, property);
            return value != null ? value.toString() : "";
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Parse world placeholders by name (worldpanels_world_MyWorld_playtime)
     */
    private static String parseWorldNamePlaceholder(String placeholder) {
        Matcher matcher = WORLD_NAME_PATTERN.matcher(placeholder);
        if (!matcher.matches()) return null;
        
        String worldName = matcher.group(1);
        String property = matcher.group(2);
        
        Object value = getWorldPropertyByName(worldName, property);
        return value != null ? value.toString() : "";
    }
    
    /**
     * Parse world list placeholders (worldpanels_list_names)
     */
    private static String parseWorldListPlaceholder(String placeholder) {
        Matcher matcher = WORLD_LIST_PATTERN.matcher(placeholder);
        if (!matcher.matches()) return null;
        
        String listType = matcher.group(1);
        
        try {
            WorldDataManager manager = WorldDataManager.getInstance();
            List<WorldInfo> worlds = manager.getWorlds();
            
            switch (listType.toLowerCase()) {
                case "names":
                    return worlds.stream()
                            .map(WorldInfo::getWorldName)
                            .collect(Collectors.joining(", "));
                case "count":
                    return String.valueOf(worlds.size());
                case "recent":
                    return worlds.stream()
                            .limit(5)
                            .map(WorldInfo::getWorldName)
                            .collect(Collectors.joining(", "));
                default:
                    return null;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get world list for type: {}", listType, e);
            return null;
        }
    }
    
    /**
     * Parse world count placeholder (worldpanels_count)
     */
    private static String parseWorldCountPlaceholder(String placeholder) {
        if (WORLD_COUNT_PATTERN.matcher(placeholder).matches()) {
            try {
                WorldDataManager manager = WorldDataManager.getInstance();
                return String.valueOf(manager.getWorlds().size());
            } catch (Exception e) {
                LOGGER.debug("Failed to get world count", e);
                return "0";
            }
        }
        return null;
    }
    
    /**
     * Try to parse placeholder from JSON files
     */
    private static String parseFromJsonFiles(String placeholder) {
        try {
            // Try different JSON files
            String[] jsonFiles = {
                "config/fancymenu/assets/worlddata.json",
                "config/fancymenu/assets/worldcards.json",
                "config/fancymenu/assets/worldtemplate.json"
            };
            
            for (String jsonFile : jsonFiles) {
                File file = new File(jsonFile);
                if (file.exists()) {
                    String content = Files.readString(file.toPath());
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    
                    String value = extractValueFromJson(json, placeholder);
                    if (value != null) return value;
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to parse from JSON files", e);
        }
        
        return null;
    }
    
    /**
     * Extract value from JSON using dot notation
     */
    private static String extractValueFromJson(JsonObject json, String placeholder) {
        try {
            // Convert placeholder to JSON path (worldpanels_world_1_name -> worlds.0.name)
            String jsonPath = convertPlaceholderToJsonPath(placeholder);
            if (jsonPath == null) return null;
            
            String[] parts = jsonPath.split("\\.");
            JsonObject current = json;
            
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                
                if (part.matches("\\d+")) {
                    // Array index
                    int index = Integer.parseInt(part);
                    if (current.has("worlds") && current.get("worlds").isJsonArray()) {
                        var array = current.getAsJsonArray("worlds");
                        if (index < array.size()) {
                            current = array.get(index).getAsJsonObject();
                        } else {
                            return null;
                        }
                    } else if (current.has("cards") && current.get("cards").isJsonArray()) {
                        var array = current.getAsJsonArray("cards");
                        if (index < array.size()) {
                            current = array.get(index).getAsJsonObject();
                        } else {
                            return null;
                        }
                    }
                } else {
                    // Object property
                    if (current.has(part)) {
                        current = current.getAsJsonObject(part);
                    } else {
                        return null;
                    }
                }
            }
            
            // Get final value
            String finalKey = parts[parts.length - 1];
            if (current.has(finalKey)) {
                return current.get(finalKey).getAsString();
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to extract JSON value for: {}", placeholder, e);
        }
        
        return null;
    }
    
    /**
     * Convert placeholder to JSON path
     */
    private static String convertPlaceholderToJsonPath(String placeholder) {
        // worldpanels_world_1_name -> worlds.0.name
        if (placeholder.startsWith("worldpanels_world_")) {
            String remaining = placeholder.substring("worldpanels_world_".length());
            String[] parts = remaining.split("_", 2);
            
            if (parts.length == 2) {
                try {
                    int index = Integer.parseInt(parts[0]) - 1; // Convert to 0-based
                    return "worlds." + index + "." + parts[1];
                } catch (NumberFormatException e) {
                    // Might be world name instead of index
                    return "worlds." + parts[0] + "." + parts[1];
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get world property by index using existing WorldDataManager
     */
    private static Object getWorldPropertyByIndex(int index, String property) {
        try {
            WorldDataManager manager = WorldDataManager.getInstance();
            List<WorldInfo> worlds = manager.getWorlds();
            
            if (index >= 0 && index < worlds.size()) {
                WorldInfo worldInfo = worlds.get(index);
                return getWorldProperty(worldInfo, property);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get world property by index: {} {}", index, property, e);
        }
        return null;
    }
    
    /**
     * Get world property by name using existing WorldDataManager
     */
    private static Object getWorldPropertyByName(String worldName, String property) {
        try {
            WorldDataManager manager = WorldDataManager.getInstance();
            WorldInfo worldInfo = manager.getWorld(worldName);
            
            if (worldInfo != null) {
                return getWorldProperty(worldInfo, property);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get world property by name: {} {}", worldName, property, e);
        }
        return null;
    }
    
    /**
     * Extract specific property from WorldInfo using existing methods
     */
    private static Object getWorldProperty(WorldInfo worldInfo, String property) {
        if (worldInfo == null) return null;
        
        switch (property.toLowerCase()) {
            case "name":
                return worldInfo.getWorldName();
            case "display_name":
            case "displayname":
                return worldInfo.getWorldName();
            case "folder":
                return worldInfo.getFolderName();
            case "path":
                return worldInfo.getFolderPath();
            case "gamemode":
                return worldInfo.getGameModeDisplay();
            case "gamemode_raw":
            case "gamemoderaw":
                return worldInfo.getGameMode();
            case "difficulty":
                return worldInfo.getDifficultyDisplay();
            case "difficulty_raw":
            case "difficultyraw":
                return worldInfo.getDifficulty();
            case "version":
                return worldInfo.getVersion();
            case "seed":
                return String.valueOf(worldInfo.getSeed());
            case "hardcore":
                return worldInfo.isHardcore() ? "Yes" : "No";
            case "cheats":
            case "cheats_enabled":
                return worldInfo.hasCheats() ? "Enabled" : "Disabled";
            case "last_played":
            case "lastplayed":
                return worldInfo.getFormattedLastPlayed();
            case "last_played_raw":
            case "lastplayedraw":
                return String.valueOf(worldInfo.getLastPlayed());
            case "created":
            case "creation_time":
                return worldInfo.getFormattedCreationTime();
            case "created_raw":
            case "createdraw":
                return String.valueOf(worldInfo.getCreationTime());
            case "size":
            case "size_formatted":
                return worldInfo.getFormattedWorldSize();
            case "size_bytes":
            case "sizebytes":
                return String.valueOf(worldInfo.getWorldSizeBytes());
            case "in_use":
            case "inuse":
                return worldInfo.isInUse() ? "In Use" : "Available";
            case "status":
                return worldInfo.isInUse() ? "In Use" : "Available";
            case "has_icon":
            case "hasicon":
                return worldInfo.hasIcon() ? "Yes" : "No";
            case "weather":
                return worldInfo.getWeatherDisplay();
            case "time_of_day":
            case "timeofday":
                return worldInfo.getTimeOfDayDisplay();
            case "raining":
                return worldInfo.isRaining() ? "Yes" : "No";
            case "thundering":
                return worldInfo.isThundering() ? "Yes" : "No";
            case "world_time":
            case "worldtime":
                return String.valueOf(worldInfo.getWorldTime());
            case "day_time":
            case "daytime":
                return String.valueOf(worldInfo.getDayTime());
            case "icon_path":
            case "iconpath":
                return worldInfo.getIconPath() != null ? worldInfo.getIconPath() : "";
            default:
                return null;
        }
    }
    
    /**
     * Update cached world data if needed
     */
    private static void updateCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            try {
                WorldDataManager manager = WorldDataManager.getInstance();
                manager.refreshWorlds();
                lastCacheUpdate = currentTime;
            } catch (Exception e) {
                LOGGER.debug("Failed to update world cache", e);
            }
        }
    }
}