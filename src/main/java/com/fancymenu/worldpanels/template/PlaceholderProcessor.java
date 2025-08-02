package com.fancymenu.worldpanels.template;

import com.fancymenu.worldpanels.data.WorldInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced placeholder processor with comprehensive world data support including dynamic images.
 * Supports ALL available WorldInfo fields and provides rich formatting options.
 */
@Environment(EnvType.CLIENT)
public class PlaceholderProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderProcessor.class);
    
    // Placeholder pattern - matches {placeholder_name}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    
    // Date formatters for different placeholder types
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
    
    /**
     * Process a text string and replace all placeholders with world data
     */
    public static String processPlaceholders(String text, WorldInfo world) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        if (world == null) {
            // Show placeholder names in brackets when no world data available
            return text.replaceAll("\\{([^{}]+)\\}", "[$1]");
        }
        
        // Build comprehensive placeholder value map
        Map<String, String> placeholders = buildComprehensivePlaceholderMap(world);
        
        // Replace all placeholders
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholderName = matcher.group(1).toLowerCase();
            String replacement = placeholders.getOrDefault(placeholderName, "{" + matcher.group(1) + "}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Build comprehensive map of ALL available placeholders
     */
    private static Map<String, String> buildComprehensivePlaceholderMap(WorldInfo world) {
        Map<String, String> placeholders = new HashMap<>();
        
        try {
            // ======================================
            // BASIC WORLD INFORMATION
            // ======================================
            placeholders.put("world_name", safeString(world.getWorldName(), "Unnamed World"));
            placeholders.put("folder_name", safeString(world.getFolderName(), "unknown"));
            placeholders.put("folder_path", safeString(world.getFolderPath(), "unknown"));
            
            // ======================================
            // GAME SETTINGS
            // ======================================
            placeholders.put("game_mode", safeString(world.getGameMode(), "Unknown"));
            placeholders.put("game_mode_display", safeString(world.getGameModeDisplay(), "Unknown"));
            placeholders.put("difficulty", safeString(world.getDifficulty(), "Unknown"));
            placeholders.put("difficulty_display", safeString(world.getDifficultyDisplay(), "Unknown"));
            placeholders.put("version", safeString(world.getVersion(), "Unknown"));
            placeholders.put("hardcore", world.isHardcore() ? "Yes" : "No");
            placeholders.put("hardcore_symbol", world.isHardcore() ? "💀" : "❤");
            placeholders.put("cheats", world.hasCheats() ? "Enabled" : "Disabled");
            placeholders.put("cheats_symbol", world.hasCheats() ? "⚡" : "🚫");
            
            // ======================================
            // WORLD SEED
            // ======================================
            placeholders.put("seed", String.valueOf(world.getSeed()));
            placeholders.put("seed_short", formatSeedShort(world.getSeed()));
            
            // ======================================
            // TIMESTAMPS - LAST PLAYED
            // ======================================
            placeholders.put("last_played", formatLastPlayedRelative(world.getLastPlayed()));
            placeholders.put("last_played_date", formatDate(world.getLastPlayed()));
            placeholders.put("last_played_time", formatTime(world.getLastPlayed()));
            placeholders.put("last_played_datetime", formatDateTime(world.getLastPlayed()));
            placeholders.put("last_played_short", formatDateShort(world.getLastPlayed()));
            placeholders.put("last_played_formatted", world.getFormattedLastPlayed());
            
            // ======================================
            // TIMESTAMPS - CREATION TIME
            // ======================================
            placeholders.put("creation_time", formatDate(world.getCreationTime()));
            placeholders.put("creation_date", formatDate(world.getCreationTime()));
            placeholders.put("creation_datetime", formatDateTime(world.getCreationTime()));
            placeholders.put("creation_formatted", world.getFormattedCreationTime());
            
            // ======================================
            // TIMESTAMPS - FOLDER MODIFIED
            // ======================================
            placeholders.put("folder_modified", formatDate(world.getFolderModified()));
            placeholders.put("folder_modified_datetime", formatDateTime(world.getFolderModified()));
            placeholders.put("folder_modified_relative", formatRelativeTime(world.getFolderModified()));
            
            // ======================================
            // WORLD STATUS
            // ======================================
            placeholders.put("in_use", world.isInUse() ? "Yes" : "No");
            placeholders.put("in_use_symbol", world.isInUse() ? "●" : "○");
            placeholders.put("has_icon", world.hasIcon() ? "Yes" : "No");
            placeholders.put("icon_symbol", world.hasIcon() ? "🖼" : "📷");
            
            // ======================================
            // WORLD SIZE
            // ======================================
            placeholders.put("world_size", String.valueOf(world.getWorldSizeBytes()));
            placeholders.put("world_size_formatted", world.getFormattedWorldSize());
            placeholders.put("world_size_mb", formatSizeInMB(world.getWorldSizeBytes()));
            placeholders.put("world_size_gb", formatSizeInGB(world.getWorldSizeBytes()));
            
            // ======================================
            // WORLD TIME & WEATHER
            // ======================================
            placeholders.put("world_time", String.valueOf(world.getWorldTime()));
            placeholders.put("day_time", String.valueOf(world.getDayTime()));
            placeholders.put("time_of_day", world.getTimeOfDayDisplay());
            placeholders.put("weather", world.getWeatherDisplay());
            placeholders.put("weather_symbol", getWeatherSymbol(world));
            placeholders.put("raining", world.isRaining() ? "Yes" : "No");
            placeholders.put("raining_symbol", world.isRaining() ? "🌧" : "☀");
            placeholders.put("thundering", world.isThundering() ? "Yes" : "No");
            placeholders.put("thundering_symbol", world.isThundering() ? "⛈" : "🌤");
            
            // ======================================
            // FILE SYSTEM & IMAGES
            // ======================================
            placeholders.put("icon_path", safeString(world.getIconPath(), "No Icon"));
            placeholders.put("world_screenshot", getWorldScreenshotPath(world));
            placeholders.put("world_icon", getWorldIconPath(world));
            placeholders.put("world_image", getSmartWorldImagePath(world));
            placeholders.put("game_mode_icon", getGameModeIconPath(world));
            placeholders.put("status_icon", getStatusIconPath(world));
            
            // ======================================
            // COMPUTED VALUES
            // ======================================
            placeholders.put("world_age", calculateWorldAge(world.getCreationTime()));
            placeholders.put("days_since_played", calculateDaysSincePlayed(world.getLastPlayed()));
            placeholders.put("world_type", determineWorldType(world));
            placeholders.put("play_status", determinePlayStatus(world));
            
            // ======================================
            // VISUAL SYMBOLS & INDICATORS
            // ======================================
            placeholders.put("status_dot", world.isInUse() ? "🟢" : "🔴");
            placeholders.put("mode_symbol", getGameModeSymbol(world.getGameMode()));
            placeholders.put("difficulty_symbol", getDifficultySymbol(world.getDifficulty()));
            
        } catch (Exception e) {
            LOGGER.warn("Error building comprehensive placeholder map for world: {}", world.getWorldName(), e);
        }
        
        return placeholders;
    }
    
    // ======================================
    // IMAGE PLACEHOLDER METHODS
    // ======================================
    
    /**
     * Get world screenshot path (Minecraft's auto-generated world image)
     */
    private static String getWorldScreenshotPath(WorldInfo world) {
        try {
            // Minecraft stores world screenshots as icon.png in the world folder
            String worldFolder = world.getFolderPath();
            if (worldFolder != null && !worldFolder.isEmpty()) {
                File screenshotFile = new File(worldFolder, "icon.png");
                if (screenshotFile.exists() && screenshotFile.isFile()) {
                    return screenshotFile.getAbsolutePath();
                }
            }
            
            // Fall back to default world icon
            return getDefaultWorldIcon();
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get world screenshot path for: {}", world.getWorldName(), e);
            return getDefaultWorldIcon();
        }
    }
    
    /**
     * Get world icon path (custom or screenshot)
     */
    private static String getWorldIconPath(WorldInfo world) {
        try {
            // First try custom icon if available
            String iconPath = world.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                File iconFile = new File(iconPath);
                if (iconFile.exists() && iconFile.isFile()) {
                    return iconFile.getAbsolutePath();
                }
            }
            
            // Fall back to screenshot
            return getWorldScreenshotPath(world);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get world icon path for: {}", world.getWorldName(), e);
            return getDefaultWorldIcon();
        }
    }
    
    /**
     * Smart world image selection - chooses best available image
     */
    private static String getSmartWorldImagePath(WorldInfo world) {
        try {
            // Priority order: custom icon -> screenshot -> game mode icon -> default
            
            // 1. Try custom icon
            String iconPath = world.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                File iconFile = new File(iconPath);
                if (iconFile.exists() && iconFile.isFile()) {
                    return iconFile.getAbsolutePath();
                }
            }
            
            // 2. Try screenshot
            String screenshotPath = getWorldScreenshotPath(world);
            if (screenshotPath != null && !screenshotPath.equals(getDefaultWorldIcon())) {
                return screenshotPath;
            }
            
            // 3. Try game mode icon
            return getGameModeIconPath(world);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get smart world image path for: {}", world.getWorldName(), e);
            return getDefaultWorldIcon();
        }
    }
    
    /**
     * Get game mode specific icon path
     */
    private static String getGameModeIconPath(WorldInfo world) {
        try {
            String gameMode = world.getGameMode();
            if (gameMode == null) gameMode = "survival";
            
            // Try to find game mode specific icons in resources
            String iconName = "gamemode_" + gameMode.toLowerCase() + ".png";
            return getResourceIconPath(iconName);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get game mode icon path for: {}", world.getWorldName(), e);
            return getDefaultWorldIcon();
        }
    }
    
    /**
     * Get status specific icon path
     */
    private static String getStatusIconPath(WorldInfo world) {
        try {
            String iconName;
            
            if (world.isInUse()) {
                iconName = "world_active.png";
            } else if (world.isHardcore()) {
                iconName = "world_hardcore.png";
            } else {
                long daysSince = (System.currentTimeMillis() - world.getLastPlayed()) / (24 * 60 * 60 * 1000);
                if (daysSince < 1) {
                    iconName = "world_recent.png";
                } else if (daysSince < 7) {
                    iconName = "world_week.png";
                } else {
                    iconName = "world_old.png";
                }
            }
            
            return getResourceIconPath(iconName);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get status icon path for: {}", world.getWorldName(), e);
            return getDefaultWorldIcon();
        }
    }
    
    /**
     * Get path to a resource icon
     */
    private static String getResourceIconPath(String iconName) {
        try {
            // Try mod's resource directory first
            File modIconsDir = new File("config/fancymenu/assets/worldpanels/icons");
            File iconFile = new File(modIconsDir, iconName);
            
            if (iconFile.exists() && iconFile.isFile()) {
                return iconFile.getAbsolutePath();
            }
            
            // Fall back to Minecraft resource
            return "assets/minecraft/textures/gui/world_selection.png";
            
        } catch (Exception e) {
            LOGGER.debug("Failed to get resource icon path: {}", iconName, e);
            return getDefaultWorldIcon();
        }
    }
    
    /**
     * Get default world icon path
     */
    private static String getDefaultWorldIcon() {
        return "assets/minecraft/textures/gui/world_selection.png";
    }
    
    // ======================================
    // HELPER METHODS
    // ======================================
    
    private static String safeString(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value : fallback;
    }
    
    private static String formatDate(long timestamp) {
        return timestamp > 0 ? DATE_FORMAT.format(new Date(timestamp)) : "Never";
    }
    
    private static String formatTime(long timestamp) {
        return timestamp > 0 ? TIME_FORMAT.format(new Date(timestamp)) : "Never";
    }
    
    private static String formatDateTime(long timestamp) {
        return timestamp > 0 ? DATETIME_FORMAT.format(new Date(timestamp)) : "Never";
    }
    
    private static String formatDateShort(long timestamp) {
        return timestamp > 0 ? SHORT_DATE_FORMAT.format(new Date(timestamp)) : "Never";
    }
    
    private static String formatLastPlayedRelative(long lastPlayed) {
        if (lastPlayed <= 0) return "Never";
        
        try {
            long now = System.currentTimeMillis();
            long timeDiff = now - lastPlayed;
            
            if (timeDiff < 60 * 1000) return "Just now";
            if (timeDiff < 60 * 60 * 1000) {
                long minutes = timeDiff / (60 * 1000);
                return minutes + "m ago";
            }
            if (timeDiff < 24 * 60 * 60 * 1000) {
                long hours = timeDiff / (60 * 60 * 1000);
                return hours + "h ago";
            }
            if (timeDiff < 7 * 24 * 60 * 60 * 1000) {
                long days = timeDiff / (24 * 60 * 60 * 1000);
                return days + "d ago";
            }
            return DATE_FORMAT.format(new Date(lastPlayed));
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private static String formatRelativeTime(long timestamp) {
        if (timestamp <= 0) return "Never";
        
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60 * 1000) return "Just now";
        if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + "m ago";
        if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + "h ago";
        if (diff < 30 * 24 * 60 * 60 * 1000) return (diff / (24 * 60 * 60 * 1000)) + "d ago";
        
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    private static String formatSeedShort(long seed) {
        String seedStr = String.valueOf(Math.abs(seed));
        return seedStr.length() > 8 ? seedStr.substring(0, 8) + "..." : seedStr;
    }
    
    private static String formatSizeInMB(long bytes) {
        if (bytes <= 0) return "0 MB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    private static String formatSizeInGB(long bytes) {
        if (bytes <= 0) return "0 GB";
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private static String calculateWorldAge(long creationTime) {
        if (creationTime <= 0) return "Unknown";
        
        long now = System.currentTimeMillis();
        long age = now - creationTime;
        long days = age / (24 * 60 * 60 * 1000);
        
        if (days == 0) return "Today";
        if (days == 1) return "1 day";
        if (days < 30) return days + " days";
        if (days < 365) return (days / 30) + " months";
        return (days / 365) + " years";
    }
    
    private static String calculateDaysSincePlayed(long lastPlayed) {
        if (lastPlayed <= 0) return "Never";
        
        long now = System.currentTimeMillis();
        long diff = now - lastPlayed;
        long days = diff / (24 * 60 * 60 * 1000);
        
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }
    
    private static String determineWorldType(WorldInfo world) {
        if (world.isHardcore()) return "Hardcore";
        return safeString(world.getGameMode(), "Unknown");
    }
    
    private static String determinePlayStatus(WorldInfo world) {
        if (world.isInUse()) return "Currently Playing";
        
        long lastPlayed = world.getLastPlayed();
        if (lastPlayed <= 0) return "Never Played";
        
        long now = System.currentTimeMillis();
        long diff = now - lastPlayed;
        
        if (diff < 24 * 60 * 60 * 1000) return "Recently Played";
        if (diff < 7 * 24 * 60 * 60 * 1000) return "Played This Week";
        if (diff < 30 * 24 * 60 * 60 * 1000) return "Played This Month";
        
        return "Not Recently Played";
    }
    
    private static String getWeatherSymbol(WorldInfo world) {
        if (world.isThundering()) return "⛈";
        if (world.isRaining()) return "🌧";
        return "☀";
    }
    
    private static String getGameModeSymbol(String gameMode) {
        if (gameMode == null) return "❓";
        switch (gameMode.toLowerCase()) {
            case "survival": return "⚔";
            case "creative": return "🎨";
            case "adventure": return "🗺";
            case "spectator": return "👻";
            default: return "❓";
        }
    }
    
    private static String getDifficultySymbol(String difficulty) {
        if (difficulty == null) return "❓";
        switch (difficulty.toLowerCase()) {
            case "peaceful": return "🕊";
            case "easy": return "😊";
            case "normal": return "😐";
            case "hard": return "😰";
            default: return "❓";
        }
    }
    
    // ======================================
    // UTILITY METHODS
    // ======================================
    
    /**
     * Check if a text contains any placeholders
     */
    public static boolean containsPlaceholders(String text) {
        return text != null && !text.isEmpty() && PLACEHOLDER_PATTERN.matcher(text).find();
    }
    
    /**
     * Get all placeholder names found in a text
     */
    public static java.util.List<String> findPlaceholders(String text) {
        java.util.List<String> placeholders = new java.util.ArrayList<>();
        
        if (text == null || text.isEmpty()) return placeholders;
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            if (!placeholders.contains(placeholder)) {
                placeholders.add(placeholder);
            }
        }
        
        return placeholders;
    }
    
    /**
     * Get ALL available placeholder names (comprehensive list)
     */
    public static java.util.List<String> getAllAvailablePlaceholders() {
        java.util.List<String> placeholders = new java.util.ArrayList<>();
        
        // Basic world info
        placeholders.add("world_name");
        placeholders.add("folder_name");
        placeholders.add("folder_path");
        
        // Game settings
        placeholders.add("game_mode");
        placeholders.add("game_mode_display");
        placeholders.add("difficulty");
        placeholders.add("difficulty_display");
        placeholders.add("version");
        placeholders.add("hardcore");
        placeholders.add("hardcore_symbol");
        placeholders.add("cheats");
        placeholders.add("cheats_symbol");
        
        // Seed
        placeholders.add("seed");
        placeholders.add("seed_short");
        
        // Last played timestamps
        placeholders.add("last_played");
        placeholders.add("last_played_date");
        placeholders.add("last_played_time");
        placeholders.add("last_played_datetime");
        placeholders.add("last_played_short");
        placeholders.add("last_played_formatted");
        
        // Creation timestamps
        placeholders.add("creation_time");
        placeholders.add("creation_date");
        placeholders.add("creation_datetime");
        placeholders.add("creation_formatted");
        
        // Folder modified
        placeholders.add("folder_modified");
        placeholders.add("folder_modified_datetime");
        placeholders.add("folder_modified_relative");
        
        // Status
        placeholders.add("in_use");
        placeholders.add("in_use_symbol");
        placeholders.add("has_icon");
        placeholders.add("icon_symbol");
        
        // World size
        placeholders.add("world_size");
        placeholders.add("world_size_formatted");
        placeholders.add("world_size_mb");
        placeholders.add("world_size_gb");
        
        // Time & weather
        placeholders.add("world_time");
        placeholders.add("day_time");
        placeholders.add("time_of_day");
        placeholders.add("weather");
        placeholders.add("weather_symbol");
        placeholders.add("raining");
        placeholders.add("raining_symbol");
        placeholders.add("thundering");
        placeholders.add("thundering_symbol");
        
        // File system & Images
        placeholders.add("icon_path");
        placeholders.add("world_screenshot");
        placeholders.add("world_icon");
        placeholders.add("world_image");
        placeholders.add("game_mode_icon");
        placeholders.add("status_icon");
        
        // Computed values
        placeholders.add("world_age");
        placeholders.add("days_since_played");
        placeholders.add("world_type");
        placeholders.add("play_status");
        
        // Visual symbols
        placeholders.add("status_dot");
        placeholders.add("mode_symbol");
        placeholders.add("difficulty_symbol");
        
        return placeholders;
    }
}