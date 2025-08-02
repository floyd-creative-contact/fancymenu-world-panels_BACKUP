package com.fancymenu.worldpanels.template;

import com.fancymenu.worldpanels.data.WorldInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified WorldCardTemplate for anchor-based system.
 * This will be enhanced later to work with FancyMenu's anchor system.
 */
@Environment(EnvType.CLIENT)
public class WorldCardTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardTemplate.class);
    
    private String templateName;
    
    // Template bounds
    private int templateWidth = 200;
    private int templateHeight = 100;
    
    public WorldCardTemplate(String templateName) {
        this.templateName = templateName != null ? templateName : "Default Template";
    }
    
    /**
     * Render this template for a specific world (simplified version)
     */
    public void renderForWorld(WorldInfo world, DrawContext drawContext, 
                              int cardX, int cardY, int mouseX, int mouseY) {
        if (world == null) {
            renderFallback(drawContext, world, cardX, cardY);
            return;
        }
        
        try {
            // Render card background
            renderCardBackground(drawContext, cardX, cardY);
            
            // Simple text rendering for now
            renderSimpleWorldInfo(drawContext, world, cardX, cardY);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to render template for world: {}", world.getWorldName(), e);
            renderFallback(drawContext, world, cardX, cardY);
        }
    }
    
    /**
     * Render the card background
     */
    private void renderCardBackground(DrawContext drawContext, int cardX, int cardY) {
        // Simple background
        drawContext.fill(cardX, cardY, cardX + templateWidth, cardY + templateHeight, 0x88222222);
        drawContext.fill(cardX - 1, cardY - 1, cardX + templateWidth + 1, cardY + templateHeight + 1, 0xFF666666);
    }
    
    /**
     * Render simple world information
     */
    private void renderSimpleWorldInfo(DrawContext drawContext, WorldInfo world, int cardX, int cardY) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            // World name
            String worldName = world.getWorldName();
            drawContext.drawText(client.textRenderer, Text.literal(worldName),
                               cardX + 5, cardY + 5, 0xFFFFFFFF, true);
            
            // Game mode
            String gameMode = world.getGameMode();
            drawContext.drawText(client.textRenderer, Text.literal("Mode: " + gameMode),
                               cardX + 5, cardY + 20, 0xFFCCCCCC, false);
            
            // Process last played with placeholder system
            String lastPlayed = PlaceholderProcessor.processPlaceholders("{last_played}", world);
            drawContext.drawText(client.textRenderer, Text.literal("Last: " + lastPlayed),
                               cardX + 5, cardY + 35, 0xFFCCCCCC, false);
        }
    }
    
    /**
     * Render fallback when template fails
     */
    private void renderFallback(DrawContext drawContext, WorldInfo world, int cardX, int cardY) {
        // Simple fallback rendering
        drawContext.fill(cardX, cardY, cardX + templateWidth, cardY + templateHeight, 0x88444444);
        
        if (world != null) {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                drawContext.drawText(client.textRenderer, Text.literal(world.getWorldName()),
                                   cardX + 5, cardY + 5, 0xFFFFFFFF, true);
            }
        }
    }
    
    /**
     * Get default template instance
     */
    public static WorldCardTemplate getDefault() {
        return new WorldCardTemplate("Default Template");
    }
    
    /**
     * Create default template instance
     */
    public static WorldCardTemplate createDefault() {
        return new WorldCardTemplate("Default Template");
    }
    
    /**
     * Create a copy of this template
     */
    public WorldCardTemplate copy() {
        WorldCardTemplate copy = new WorldCardTemplate(templateName + " (Copy)");
        copy.templateWidth = this.templateWidth;
        copy.templateHeight = this.templateHeight;
        return copy;
    }
    
    /**
     * Copy from another template
     */
    public void copyFrom(WorldCardTemplate other) {
        this.templateName = other.templateName;
        this.templateWidth = other.templateWidth;
        this.templateHeight = other.templateHeight;
    }
    
    // Getters and setters
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public int getTemplateWidth() { return templateWidth; }
    public int getTemplateHeight() { return templateHeight; }
    
    public void setTemplateDimensions(int width, int height) {
        this.templateWidth = Math.max(50, width);
        this.templateHeight = Math.max(30, height);
    }
    
    public int getElementCount() { return 3; } // Placeholder count
    
    // Legacy color methods for compatibility with WorldCardElement
    private int backgroundColor = 0x88222222;
    private int hoverColor = 0x88444444;
    private int borderColor = 0xFF666666;
    private int activeColor = 0xFFFFAA00;
    
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int color) { this.backgroundColor = color; }
    
    public int getHoverColor() { return hoverColor; }
    public void setHoverColor(int color) { this.hoverColor = color; }
    
    public int getBorderColor() { return borderColor; }
    public void setBorderColor(int color) { this.borderColor = color; }
    
    public int getActiveColor() { return activeColor; }
    public void setActiveColor(int color) { this.activeColor = color; }
}