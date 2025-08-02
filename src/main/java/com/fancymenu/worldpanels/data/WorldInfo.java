package com.fancymenu.worldpanels.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * Immutable data class representing complete information about a Minecraft world.
 * 
 * Contains all data needed for display in world panels:
 * - Basic info: name, folder, paths
 * - Game settings: mode, difficulty, version
 * - Timestamps: creation, last played, modified
 * - Status: in use, icon availability
 */
public class WorldInfo {
    
    // Basic world information
    private final String folderName;
    private final String worldName;
    private final String folderPath;
    private final String iconPath;
    
    // Game information
    private final String gameMode;
    private final String difficulty;
    private final String version;
    private final long seed;
    private final boolean hardcore;
    private final boolean cheats;
    
    // Timestamps (Unix epoch milliseconds)
    private final long lastPlayed;
    private final long creationTime;
    private final long folderModified;
    
    // Status information
    private final boolean isInUse;
    private final boolean hasIcon;
    private final long worldSizeBytes;
    
    // World time information
    private final long worldTime;
    private final long dayTime;
    private final boolean raining;
    private final boolean thundering;
    
    private WorldInfo(Builder builder) {
        this.folderName = builder.folderName;
        this.worldName = builder.worldName;
        this.folderPath = builder.folderPath;
        this.iconPath = builder.iconPath;
        this.gameMode = builder.gameMode;
        this.difficulty = builder.difficulty;
        this.version = builder.version;
        this.seed = builder.seed;
        this.hardcore = builder.hardcore;
        this.cheats = builder.cheats;
        this.lastPlayed = builder.lastPlayed;
        this.creationTime = builder.creationTime;
        this.folderModified = builder.folderModified;
        this.isInUse = builder.isInUse;
        this.hasIcon = builder.iconPath != null;
        this.worldSizeBytes = builder.worldSizeBytes;
        this.worldTime = builder.worldTime;
        this.dayTime = builder.dayTime;
        this.raining = builder.raining;
        this.thundering = builder.thundering;
    }
    
    // Getters
    public String getFolderName() { return folderName; }
    public String getWorldName() { return worldName != null ? worldName : folderName; }
    public String getFolderPath() { return folderPath; }
    public String getIconPath() { return iconPath; }
    public String getGameMode() { return gameMode; }
    public String getDifficulty() { return difficulty; }
    public String getVersion() { return version; }
    public long getSeed() { return seed; }
    public boolean isHardcore() { return hardcore; }
    public boolean hasCheats() { return cheats; }
    public long getLastPlayed() { return lastPlayed; }
    public long getCreationTime() { return creationTime; }
    public long getFolderModified() { return folderModified; }
    public boolean isInUse() { return isInUse; }
    public boolean hasIcon() { return hasIcon; }
    public long getWorldSizeBytes() { return worldSizeBytes; }
    public long getWorldTime() { return worldTime; }
    public long getDayTime() { return dayTime; }
    public boolean isRaining() { return raining; }
    public boolean isThundering() { return thundering; }
    
    // Formatted getters for display
    public String getFormattedLastPlayed() {
        if (lastPlayed == 0) return "Never";
        return new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date(lastPlayed));
    }
    
    public String getFormattedCreationTime() {
        if (creationTime == 0) return "Unknown";
        return new SimpleDateFormat("MMM dd, yyyy").format(new Date(creationTime));
    }
    
    public String getFormattedWorldSize() {
        if (worldSizeBytes == 0) return "Unknown";
        
        double bytes = worldSizeBytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        
        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", bytes, units[unitIndex]);
    }
    
    public String getGameModeDisplay() {
        if (gameMode == null) return "Unknown";
        switch (gameMode.toLowerCase()) {
            case "survival": return hardcore ? "Hardcore" : "Survival";
            case "creative": return "Creative";
            case "adventure": return "Adventure";
            case "spectator": return "Spectator";
            default: return gameMode;
        }
    }
    
    public String getDifficultyDisplay() {
        if (difficulty == null) return "Unknown";
        switch (difficulty.toLowerCase()) {
            case "peaceful": return "Peaceful";
            case "easy": return "Easy";
            case "normal": return "Normal";
            case "hard": return "Hard";
            default: return difficulty;
        }
    }
    
    public String getWeatherDisplay() {
        if (thundering) return "Thunderstorm";
        if (raining) return "Rain";
        return "Clear";
    }
    
    public String getTimeOfDayDisplay() {
        long time = dayTime % 24000;
        int hours = (int) ((time + 6000) / 1000) % 24;
        int minutes = (int) (((time + 6000) % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldInfo worldInfo = (WorldInfo) o;
        return Objects.equals(folderName, worldInfo.folderName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(folderName);
    }
    
    @Override
    public String toString() {
        return String.format("WorldInfo{name='%s', folder='%s', mode='%s', lastPlayed=%s}", 
                           getWorldName(), folderName, getGameModeDisplay(), getFormattedLastPlayed());
    }
    
    /**
     * Builder pattern for creating WorldInfo instances.
     */
    public static class Builder {
        private String folderName;
        private String worldName;
        private String folderPath;
        private String iconPath;
        private String gameMode = "Unknown";
        private String difficulty = "Unknown";
        private String version = "Unknown";
        private long seed = 0;
        private boolean hardcore = false;
        private boolean cheats = false;
        private long lastPlayed = 0;
        private long creationTime = 0;
        private long folderModified = 0;
        private boolean isInUse = false;
        private long worldSizeBytes = 0;
        private long worldTime = 0;
        private long dayTime = 0;
        private boolean raining = false;
        private boolean thundering = false;
        
        public Builder folderName(String folderName) {
            this.folderName = folderName;
            return this;
        }
        
        public Builder worldName(String worldName) {
            this.worldName = worldName;
            return this;
        }
        
        public Builder folderPath(String folderPath) {
            this.folderPath = folderPath;
            return this;
        }
        
        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath;
            return this;
        }
        
        public Builder gameMode(String gameMode) {
            this.gameMode = gameMode;
            return this;
        }
        
        public Builder difficulty(String difficulty) {
            this.difficulty = difficulty;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }
        
        public Builder hardcore(boolean hardcore) {
            this.hardcore = hardcore;
            return this;
        }
        
        public Builder cheats(boolean cheats) {
            this.cheats = cheats;
            return this;
        }
        
        public Builder lastPlayed(long lastPlayed) {
            this.lastPlayed = lastPlayed;
            return this;
        }
        
        public Builder creationTime(long creationTime) {
            this.creationTime = creationTime;
            return this;
        }
        
        public Builder folderModified(long folderModified) {
            this.folderModified = folderModified;
            return this;
        }
        
        public Builder isInUse(boolean isInUse) {
            this.isInUse = isInUse;
            return this;
        }
        
        public Builder worldSizeBytes(long worldSizeBytes) {
            this.worldSizeBytes = worldSizeBytes;
            return this;
        }
        
        public Builder worldTime(long worldTime) {
            this.worldTime = worldTime;
            return this;
        }
        
        public Builder dayTime(long dayTime) {
            this.dayTime = dayTime;
            return this;
        }
        
        public Builder raining(boolean raining) {
            this.raining = raining;
            return this;
        }
        
        public Builder thundering(boolean thundering) {
            this.thundering = thundering;
            return this;
        }
        
        public WorldInfo build() {
            Objects.requireNonNull(folderName, "Folder name is required");
            Objects.requireNonNull(folderPath, "Folder path is required");
            return new WorldInfo(this);
        }
    }
}