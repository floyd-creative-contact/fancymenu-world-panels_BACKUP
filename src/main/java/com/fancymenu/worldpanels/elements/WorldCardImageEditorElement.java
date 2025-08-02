package com.fancymenu.worldpanels.elements;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for WorldCardImageElement - provides configuration options for world card images.
 */
@Environment(EnvType.CLIENT)
public class WorldCardImageEditorElement extends AbstractEditorElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardImageEditorElement.class);
    
    private final WorldCardImageElement imageElement;
    private final WorldCardImageElementBuilder builder;
    
    public WorldCardImageEditorElement(WorldCardImageElement element, WorldCardImageElementBuilder builder, LayoutEditorScreen screen) {
        super(element, screen);
        this.imageElement = element;
        this.builder = builder;
    }
    
    @Override
    public void init() {
        // Call parent init first
        super.init();
        
        // Add separator before our options
        this.rightClickMenu.addSeparatorEntry("world_image_separator");
        
        // Create image type selection menu
        createImageTypeMenu();
        
        // Create help menu
        createHelpMenu();
    }
    
    /**
     * Create image type selection submenu
     */
    private void createImageTypeMenu() {
        ContextMenu imageTypeMenu = new ContextMenu();
        
        // Add each image type option
        for (WorldCardImageElement.WorldCardImageType type : WorldCardImageElement.WorldCardImageType.values()) {
            String entryId = "image_type_" + type.name().toLowerCase();
            
            imageTypeMenu.addClickableEntry(entryId,
                Text.literal(type.getDisplayName()),
                (menu, entry) -> {
                    this.editor.history.saveSnapshot();
                    imageElement.setImageType(type);
                    LOGGER.info("Changed image type to: {}", type.getDisplayName());
                    menu.closeMenu();
                })
                .setTooltipSupplier((menu, entry) -> 
                    de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                        de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines(type.getDescription())
                    ))
                .setIcon(ContextMenu.IconFactory.getIcon("image"));
        }
        
        // Add the image type submenu
        this.rightClickMenu.addSubMenuEntry("image_type_selection",
            Text.literal("Image Type"),
            imageTypeMenu)
            .setTooltipSupplier((menu, entry) -> 
                de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                    de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines(
                        "Choose what type of image to display for each world")))
            .setIcon(ContextMenu.IconFactory.getIcon("edit"));
    }
    
    /**
     * Create help and information submenu
     */
    private void createHelpMenu() {
        ContextMenu helpMenu = new ContextMenu();
        
        // Show image type information
        helpMenu.addClickableEntry("show_image_info",
            Text.literal("Image Type Info"),
            (menu, entry) -> {
                showImageTypeInfo();
                menu.closeMenu();
            })
            .setTooltipSupplier((menu, entry) -> 
                de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                    de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines(
                        "Learn about different world card image types")))
            .setIcon(ContextMenu.IconFactory.getIcon("info"));
        
        // Usage instructions
        helpMenu.addClickableEntry("usage_instructions",
            Text.literal("Usage Instructions"),
            (menu, entry) -> {
                showUsageInstructions();
                menu.closeMenu();
            })
            .setTooltipSupplier((menu, entry) -> 
                de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                    de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines(
                        "Learn how to use world card images")))
            .setIcon(ContextMenu.IconFactory.getIcon("help"));
        
        // Add help submenu
        this.rightClickMenu.addSubMenuEntry("world_image_help",
            Text.literal("Help"),
            helpMenu)
            .setTooltipSupplier((menu, entry) -> 
                de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                    de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines(
                        "Get help with world card images")))
            .setIcon(ContextMenu.IconFactory.getIcon("help"));
    }
    
    /**
     * Show image type information
     */
    private void showImageTypeInfo() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§6=== WORLD CARD IMAGE TYPES ==="), false);
            client.player.sendMessage(Text.literal(""), false);
            
            for (WorldCardImageElement.WorldCardImageType type : WorldCardImageElement.WorldCardImageType.values()) {
                client.player.sendMessage(Text.literal("§e" + type.getDisplayName()), false);
                client.player.sendMessage(Text.literal("§7" + type.getDescription()), false);
                client.player.sendMessage(Text.literal(""), false);
            }
            
            client.player.sendMessage(Text.literal("§a✨ Right-click this element and choose 'Image Type' to change!"), false);
        }
    }
    
    /**
     * Show usage instructions
     */
    private void showUsageInstructions() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§6World Card Image Usage:"), false);
            client.player.sendMessage(Text.literal("§71. This element automatically shows different images for each world"), false);
            client.player.sendMessage(Text.literal("§72. Right-click and choose 'Image Type' to select what to show"), false);
            client.player.sendMessage(Text.literal("§73. Anchor this element to a World Card Template"), false);
            client.player.sendMessage(Text.literal("§74. Add a World Cards element to display dynamic cards"), false);
            client.player.sendMessage(Text.literal("§75. Each world card will show its own image automatically!"), false);
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(Text.literal("§a🎯 No placeholder configuration needed!"), false);
            client.player.sendMessage(Text.literal("§7This element handles everything automatically."), false);
        }
    }
    
    /**
     * Get tooltip text for the editor element
     */
    public List<Text> getTooltipText() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("§6World Card Image Element"));
        tooltip.add(Text.literal("§7Type: §f" + imageElement.getImageType().getDisplayName()));
        tooltip.add(Text.literal("§7Size: §f" + imageElement.getAbsoluteWidth() + "x" + imageElement.getAbsoluteHeight()));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§eShows different images for each world"));
        tooltip.add(Text.literal("§eRight-click to change image type"));
        return tooltip;
    }
    
    /**
     * Get display name for the editor element
     */
    public Text getDisplayName() {
        return Text.literal("World Image: " + imageElement.getImageType().getDisplayName());
    }
}