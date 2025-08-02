package com.fancymenu.worldpanels.elements;

import com.fancymenu.worldpanels.data.WorldInfo;
import com.fancymenu.worldpanels.managers.WorldDataManager;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom image element specifically designed for world card images.
 * Handles dynamic image loading without placeholder validation issues.
 */
@Environment(EnvType.CLIENT)
public class WorldCardImageElement extends AbstractElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardImageElement.class);
    
    // World card image types
    public enum WorldCardImageType {
        SCREENSHOT("world_screenshot", "World Screenshot", "World's auto-generated screenshot"),
        ICON("world_icon", "World Icon", "Custom world icon"),
        SMART("world_image", "Smart Selection", "Automatically choose best image"),
        GAME_MODE("game_mode_icon", "Game Mode Icon", "Icon based on game mode"),
        STATUS("status_icon", "Status Icon", "Icon based on world status");
        
        private final String placeholder;
        private final String displayName;
        private final String description;
        
        WorldCardImageType(String placeholder, String displayName, String description) {
            this.placeholder = placeholder;
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getPlaceholder() { return placeholder; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Image type and caching
    private WorldCardImageType imageType = WorldCardImageType.SCREENSHOT;
    private String currentImagePath = null;
    private boolean imageLoaded = false;
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 2000; // Update every 2 seconds
    
    // Static cache to avoid reloading same images (simplified for now)
    private static final Map<String, Boolean> IMAGE_CACHE = new HashMap<>();
    
    // Current world context (set by WorldCardElement)
    private WorldInfo currentWorld = null;
    
    public WorldCardImageElement(ElementBuilder<?, ?> builder) {
        super(builder);
        
        // Set default size for world images
        this.baseWidth = 64;
        this.baseHeight = 64;
        
        LOGGER.info("WorldCardImageElement created with type: {}", imageType.getDisplayName());
    }
    
    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        try {
            // Update image periodically or when world changes
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdate > UPDATE_INTERVAL || shouldUpdateImage()) {
                updateImage();
                lastUpdate = currentTime;
            }
            
            // Render the image if loaded
            if (imageLoaded) {
                renderWorldImage(drawContext);
            } else {
                renderPlaceholder(drawContext);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render WorldCardImageElement", e);
            renderErrorPlaceholder(drawContext);
        }
    }
    
    /**
     * Check if image should be updated (world changed, etc.)
     */
    private boolean shouldUpdateImage() {
        try {
            // Get current world from context
            WorldInfo newWorld = getCurrentWorldFromContext();
            
            // Update if world changed
            if (newWorld != currentWorld) {
                currentWorld = newWorld;
                return true;
            }
            
            // Update if no image loaded yet
            return !imageLoaded;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get current world from context (nearby WorldCardElement or global)
     */
    private WorldInfo getCurrentWorldFromContext() {
        try {
            // First priority: use the world that was set by WorldCardElement
            if (currentWorld != null) {
                LOGGER.debug("Using world set by WorldCardElement: {}", currentWorld.getWorldName());
                return currentWorld;
            }
            
            // Fallback: get from WorldDataManager (for testing in editor)
            if (WorldDataManager.getInstance().isInitialized()) {
                var worlds = WorldDataManager.getInstance().getWorlds();
                if (worlds != null && !worlds.isEmpty()) {
                    LOGGER.debug("Using first world from WorldDataManager: {}", worlds.get(0).getWorldName());
                    return worlds.get(0);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get current world from context", e);
        }
        
        LOGGER.debug("No world available - using null");
        return null;
    }
    
    /**
     * Update the image based on current world and image type
     */
    private void updateImage() {
        try {
            if (currentWorld == null) {
                LOGGER.debug("No current world - using default image");
                loadDefaultImage();
                return;
            }
            
            // Get image path based on type
            String imagePath = getImagePathForType(currentWorld, imageType);
            
            // Only update if path changed
            if (imagePath != null && !imagePath.equals(currentImagePath)) {
                currentImagePath = imagePath;
                loadImage(imagePath);
                LOGGER.debug("Updated image for world '{}' to: {}", currentWorld.getWorldName(), imagePath);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to update image", e);
            loadDefaultImage();
        }
    }
    
    /**
     * Get image path for the specified type and world
     */
    private String getImagePathForType(WorldInfo world, WorldCardImageType type) {
        try {
            switch (type) {
                case SCREENSHOT:
                    return getWorldScreenshotPath(world);
                case ICON:
                    return getWorldIconPath(world);
                case SMART:
                    return getSmartWorldImagePath(world);
                case GAME_MODE:
                    return getGameModeIconPath(world);
                case STATUS:
                    return getStatusIconPath(world);
                default:
                    return getDefaultImagePath();
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get image path for type: {}", type, e);
            return getDefaultImagePath();
        }
    }
    
    /**
     * Load image from file path
     */
    private void loadImage(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                loadDefaultImage();
                return;
            }
            
            // Check cache first
            if (IMAGE_CACHE.containsKey(imagePath)) {
                imageLoaded = IMAGE_CACHE.get(imagePath);
                LOGGER.debug("Loaded cached image status: {}", imagePath);
                return;
            }
            
            // Load new image
            File imageFile = new File(imagePath);
            if (imageFile.exists() && imageFile.isFile()) {
                loadImageFromFile(imageFile, imagePath);
            } else {
                LOGGER.debug("Image file not found: {}", imagePath);
                loadDefaultImage();
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to load image: {}", imagePath, e);
            loadDefaultImage();
        }
    }
    
    /**
     * Load image from file
     */
    private void loadImageFromFile(File imageFile, String imagePath) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;
            
            // For now, let's use a simpler approach - just mark as loaded
            // and render a colored rectangle representing the image type
            imageLoaded = true;
            currentImagePath = imagePath;
            
            LOGGER.debug("Successfully marked image as loaded: {}", imagePath);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to load image from file: {}", imageFile.getAbsolutePath(), e);
            loadDefaultImage();
        }
    }
    
    /**
     * Load default/fallback image
     */
    private void loadDefaultImage() {
        try {
            // Use a simple colored rectangle as fallback
            imageLoaded = false;
            currentImagePath = null;
            LOGGER.debug("Loaded default image placeholder");
        } catch (Exception e) {
            LOGGER.debug("Failed to load default image", e);
        }
    }
    
    /**
     * Render the world image
     */
    private void renderWorldImage(DrawContext drawContext) {
        try {
            int x = getAbsoluteX();
            int y = getAbsoluteY();
            int width = getAbsoluteWidth();
            int height = getAbsoluteHeight();
            
            // For now, render a colored rectangle based on image type
            int color = getColorForImageType();
            drawContext.fill(x, y, x + width, y + height, color);
            drawContext.drawBorder(x, y, width, height, 0xFF000000);
            
            // Draw type indicator
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                String indicator = getIndicatorForImageType();
                
                // Add world name to the indicator for debugging
                if (currentWorld != null) {
                    indicator = indicator + " " + currentWorld.getWorldName().substring(0, Math.min(3, currentWorld.getWorldName().length()));
                }
                
                int textWidth = client.textRenderer.getWidth(indicator);
                int textX = x + (width - textWidth) / 2;
                int textY = y + (height - client.textRenderer.fontHeight) / 2;
                
                drawContext.drawText(client.textRenderer, Text.literal(indicator), textX, textY, 0xFFFFFFFF, false);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render world image", e);
            renderErrorPlaceholder(drawContext);
        }
    }
    
    /**
     * Get color for image type
     */
    private int getColorForImageType() {
        switch (imageType) {
            case SCREENSHOT: return 0xFF4CAF50;  // Green
            case ICON: return 0xFF2196F3;       // Blue  
            case SMART: return 0xFF9C27B0;      // Purple
            case GAME_MODE: return 0xFFFF9800;  // Orange
            case STATUS: return 0xFFF44336;     // Red
            default: return 0xFF757575;         // Gray
        }
    }
    
    /**
     * Get text indicator for image type
     */
    private String getIndicatorForImageType() {
        switch (imageType) {
            case SCREENSHOT: return "📷";
            case ICON: return "🎨";
            case SMART: return "⭐";
            case GAME_MODE: return "🎮";
            case STATUS: return "📊";
            default: return "🖼";
        }
    }
    
    /**
     * Render placeholder when no image loaded
     */
    private void renderPlaceholder(DrawContext drawContext) {
        try {
            int x = getAbsoluteX();
            int y = getAbsoluteY();
            int width = getAbsoluteWidth();
            int height = getAbsoluteHeight();
            
            // Draw background
            drawContext.fill(x, y, x + width, y + height, 0xFF444444);
            drawContext.drawBorder(x, y, width, height, 0xFF888888);
            
            // Draw icon text
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                String text = "🖼";
                int textWidth = client.textRenderer.getWidth(text);
                int textX = x + (width - textWidth) / 2;
                int textY = y + (height - client.textRenderer.fontHeight) / 2;
                
                drawContext.drawText(client.textRenderer, Text.literal(text), textX, textY, 0xFFCCCCCC, false);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render placeholder", e);
        }
    }
    
    /**
     * Render error placeholder
     */
    private void renderErrorPlaceholder(DrawContext drawContext) {
        try {
            int x = getAbsoluteX();
            int y = getAbsoluteY();
            int width = getAbsoluteWidth();
            int height = getAbsoluteHeight();
            
            // Draw error background
            drawContext.fill(x, y, x + width, y + height, 0xFF664444);
            drawContext.drawBorder(x, y, width, height, 0xFFAA6666);
            
            // Draw error text
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                String text = "❌";
                int textWidth = client.textRenderer.getWidth(text);
                int textX = x + (width - textWidth) / 2;
                int textY = y + (height - client.textRenderer.fontHeight) / 2;
                
                drawContext.drawText(client.textRenderer, Text.literal(text), textX, textY, 0xFFFFAAAA, false);
            }
            
        } catch (Exception e) {
            // Silent fail for error placeholder
        }
    }
    
    // Configuration methods
    public WorldCardImageType getImageType() {
        return imageType;
    }
    
    public void setImageType(WorldCardImageType imageType) {
        if (this.imageType != imageType) {
            this.imageType = imageType;
            this.imageLoaded = false; // Force reload
            LOGGER.info("Changed image type to: {}", imageType.getDisplayName());
        }
    }
    
    public void setCurrentWorld(WorldInfo world) {
        if (this.currentWorld != world) {
            this.currentWorld = world;
            this.imageLoaded = false; // Force reload
            LOGGER.debug("World context changed to: {}", world != null ? world.getWorldName() : "null");
        }
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("World Card Image: " + imageType.getDisplayName());
    }
    
    // Image path helper methods
    private String getWorldScreenshotPath(WorldInfo world) {
        try {
            String worldFolder = world.getFolderPath();
            if (worldFolder != null && !worldFolder.isEmpty()) {
                File screenshotFile = new File(worldFolder, "icon.png");
                if (screenshotFile.exists() && screenshotFile.isFile()) {
                    return screenshotFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get screenshot path", e);
        }
        return getDefaultImagePath();
    }
    
    private String getWorldIconPath(WorldInfo world) {
        try {
            String iconPath = world.getIconPath();
            if (iconPath != null && !iconPath.isEmpty()) {
                File iconFile = new File(iconPath);
                if (iconFile.exists() && iconFile.isFile()) {
                    return iconFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get icon path", e);
        }
        return getWorldScreenshotPath(world);
    }
    
    private String getSmartWorldImagePath(WorldInfo world) {
        String iconPath = getWorldIconPath(world);
        if (iconPath != null && !iconPath.equals(getDefaultImagePath())) {
            return iconPath;
        }
        return getWorldScreenshotPath(world);
    }
    
    private String getGameModeIconPath(WorldInfo world) {
        // For now, return default - could add game mode specific icons later
        return getDefaultImagePath();
    }
    
    private String getStatusIconPath(WorldInfo world) {
        // For now, return default - could add status specific icons later
        return getDefaultImagePath();
    }
    
    private String getDefaultImagePath() {
        return null; // Will trigger placeholder rendering
    }
}