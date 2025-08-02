package com.fancymenu.worldpanels.elements;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorldCardTemplate Element - Acts as an anchor container for template elements.
 * This element is visible in the FancyMenu editor but invisible during gameplay.
 * Any elements anchored to this template will be collected and used as the world card template.
 */
@Environment(EnvType.CLIENT)
public class WorldCardTemplateElement extends AbstractElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardTemplateElement.class);
    
    // Template properties
    private String templateName = "World Card Template";
    private boolean showInEditor = true;
    private boolean dynamicImagesEnabled = false;
    private String imagePlaceholder = "{world_screenshot}";
    
    // Visual properties for editor
    private int borderColor = 0xFF00AAFF;
    private int backgroundColor = 0x4000AAFF;
    private int textColor = 0xFFFFFFFF;
    
    public WorldCardTemplateElement(ElementBuilder<?, ?> builder) {
        super(builder);
        
        // Set default size for template container
        this.baseWidth = 200;
        this.baseHeight = 100;
        
        LOGGER.info("WorldCardTemplateElement created: {}", templateName);
        
        // Register with static registry for testing
        WorldCardElement.WorldCardTemplateRegistry.registerTemplate(this);
        LOGGER.info("WorldCardTemplateElement created and registered: {}", templateName);
    }
    
    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // Only render in editor mode, invisible during gameplay
        if (!showInEditor || !isInEditor()) {
            return;
        }
        
        int x = getAbsoluteX();
        int y = getAbsoluteY();
        int width = getAbsoluteWidth();
        int height = getAbsoluteHeight();
        
        // Template container background
        drawContext.fill(x, y, x + width, y + height, backgroundColor);
        
        // Template container border
        drawContext.fill(x - 1, y - 1, x + width + 1, y, borderColor); // Top
        drawContext.fill(x - 1, y + height, x + width + 1, y + height + 1, borderColor); // Bottom
        drawContext.fill(x - 1, y - 1, x, y + height + 1, borderColor); // Left
        drawContext.fill(x + width, y - 1, x + width + 1, y + height + 1, borderColor); // Right
        
        // Template name in center
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            String displayText = templateName;
            int textWidth = client.textRenderer.getWidth(displayText);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - client.textRenderer.fontHeight) / 2;
            
            drawContext.drawText(client.textRenderer, Text.literal(displayText), 
                               textX, textY, textColor, false);
            
            // Helper text
            String helperText = dynamicImagesEnabled ? "Dynamic images: ON" : "Anchor elements to this template";
            int helperWidth = client.textRenderer.getWidth(helperText);
            int helperX = x + (width - helperWidth) / 2;
            int helperY = textY + client.textRenderer.fontHeight + 5;
            
            if (helperY + client.textRenderer.fontHeight <= y + height - 5) {
                int helperColor = dynamicImagesEnabled ? 0xFF99FF99 : 0xFFCCCCCC;
                drawContext.drawText(client.textRenderer, Text.literal(helperText), 
                                   helperX, helperY, helperColor, false);
            }
            
            // Show image placeholder if dynamic images enabled
            if (dynamicImagesEnabled && imagePlaceholder != null) {
                String imageText = "Image: " + imagePlaceholder;
                int imageWidth = client.textRenderer.getWidth(imageText);
                int imageX = x + (width - imageWidth) / 2;
                int imageY = helperY + client.textRenderer.fontHeight + 2;
                
                if (imageY + client.textRenderer.fontHeight <= y + height - 5) {
                    drawContext.drawText(client.textRenderer, Text.literal(imageText), 
                                       imageX, imageY, 0xFF99CCFF, false);
                }
            }
        }
        
        // Corner resize handles (standard FancyMenu behavior)
        renderResizeHandles(drawContext, x, y, width, height);
    }
    
    /**
     * Render resize handles for the template container
     */
    private void renderResizeHandles(DrawContext drawContext, int x, int y, int width, int height) {
        int handleSize = 6;
        int handleColor = 0xFFFFFFFF;
        
        // Corner handles
        drawContext.fill(x - handleSize/2, y - handleSize/2, x + handleSize/2, y + handleSize/2, handleColor);
        drawContext.fill(x + width - handleSize/2, y - handleSize/2, x + width + handleSize/2, y + handleSize/2, handleColor);
        drawContext.fill(x - handleSize/2, y + height - handleSize/2, x + handleSize/2, y + height + handleSize/2, handleColor);
        drawContext.fill(x + width - handleSize/2, y + height - handleSize/2, x + width + handleSize/2, y + height + handleSize/2, handleColor);
    }
    
    /**
     * Check if we're currently in the FancyMenu editor
     */
    private boolean isInEditor() {
        // This will be true when in FancyMenu's layout editor, false during normal gameplay
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.currentScreen != null) {
            String screenClass = client.currentScreen.getClass().getSimpleName();
            return screenClass.contains("LayoutEditor") || screenClass.contains("Editor");
        }
        return false;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle clicks only in editor mode
        if (!isInEditor()) {
            return false;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Handle dragging only in editor mode
        if (!isInEditor()) {
            return false;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    // Getters and setters for template properties
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName != null ? templateName : "World Card Template";
    }
    
    public boolean isShowInEditor() {
        return showInEditor;
    }
    
    public void setShowInEditor(boolean showInEditor) {
        this.showInEditor = showInEditor;
    }
    
    public int getBorderColor() {
        return borderColor;
    }
    
    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }
    
    public int getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    
    public boolean isDynamicImagesEnabled() {
        return dynamicImagesEnabled;
    }
    
    public void setDynamicImagesEnabled(boolean dynamicImagesEnabled) {
        this.dynamicImagesEnabled = dynamicImagesEnabled;
    }
    
    public String getImagePlaceholder() {
        return imagePlaceholder;
    }
    
    public void setImagePlaceholder(String imagePlaceholder) {
        this.imagePlaceholder = imagePlaceholder != null ? imagePlaceholder : "{world_screenshot}";
    }
    
    @Override
    public Text getDisplayName() {
        String imageStatus = dynamicImagesEnabled ? " [Images: ON]" : "";
        return Text.literal("World Card Template: " + templateName + imageStatus);
    }
    
    /**
     * Get the template bounds for anchored elements
     */
    public int getTemplateX() {
        return getAbsoluteX();
    }
    
    public int getTemplateY() {
        return getAbsoluteY();
    }
    
    public int getTemplateWidth() {
        return getAbsoluteWidth();
    }
    
    public int getTemplateHeight() {
        return getAbsoluteHeight();
    }
}