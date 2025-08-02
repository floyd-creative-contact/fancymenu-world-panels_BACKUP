package com.fancymenu.worldpanels.elements;

import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Dynamic World Card Elements - FancyMenu v3 Compatible.
 * Handles serialization/deserialization and registration with FancyMenu.
 */
@Environment(EnvType.CLIENT)
public class WorldCardElementBuilder extends ElementBuilder<WorldCardElement, WorldCardEditorElement> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardElementBuilder.class);
    
    public WorldCardElementBuilder() {
        super("worldcard");
    }
    
    @Override
    public WorldCardElement buildDefaultInstance() {
        return new WorldCardElement(this);
    }
    
    @Override
    public WorldCardElement deserializeElement(SerializedElement serialized) {
        WorldCardElement element = new WorldCardElement(this);
        
        try {
            // Layout settings
            if (serialized.getProperties().containsKey("cards_per_row")) {
                element.setCardsPerRow(Integer.parseInt(serialized.getProperties().get("cards_per_row")));
            }
            if (serialized.getProperties().containsKey("card_spacing")) {
                element.setCardSpacing(Integer.parseInt(serialized.getProperties().get("card_spacing")));
            }
            if (serialized.getProperties().containsKey("card_width") && 
                serialized.getProperties().containsKey("card_height")) {
                element.setCardSize(
                    Integer.parseInt(serialized.getProperties().get("card_width")),
                    Integer.parseInt(serialized.getProperties().get("card_height"))
                );
            }
            if (serialized.getProperties().containsKey("auto_layout")) {
                element.setAutoLayout(Boolean.parseBoolean(serialized.getProperties().get("auto_layout")));
            }
            
            // Appearance settings
            if (serialized.getProperties().containsKey("background_color")) {
                element.setBackgroundColor(Integer.parseInt(serialized.getProperties().get("background_color")));
            }
            if (serialized.getProperties().containsKey("text_color")) {
                element.setTextColor(Integer.parseInt(serialized.getProperties().get("text_color")));
            }
            if (serialized.getProperties().containsKey("font_size")) {
                element.setFontSize(Integer.parseInt(serialized.getProperties().get("font_size")));
            }
            
            // Information display
            if (serialized.getProperties().containsKey("show_last_played")) {
                element.setShowLastPlayed(Boolean.parseBoolean(serialized.getProperties().get("show_last_played")));
            }
            if (serialized.getProperties().containsKey("show_world_size")) {
                element.setShowWorldSize(Boolean.parseBoolean(serialized.getProperties().get("show_world_size")));
            }
            if (serialized.getProperties().containsKey("show_game_mode")) {
                element.setShowGameMode(Boolean.parseBoolean(serialized.getProperties().get("show_game_mode")));
            }
            
            // Button configuration
            if (serialized.getProperties().containsKey("show_buttons")) {
                element.setShowButtons(Boolean.parseBoolean(serialized.getProperties().get("show_buttons")));
            }
            if (serialized.getProperties().containsKey("show_play_button")) {
                element.setShowPlayButton(Boolean.parseBoolean(serialized.getProperties().get("show_play_button")));
            }
            if (serialized.getProperties().containsKey("show_edit_button")) {
                element.setShowEditButton(Boolean.parseBoolean(serialized.getProperties().get("show_edit_button")));
            }
            if (serialized.getProperties().containsKey("show_delete_button")) {
                element.setShowDeleteButton(Boolean.parseBoolean(serialized.getProperties().get("show_delete_button")));
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize world card element properties", e);
        }
        
        return element;
    }
    
    @Override
    public WorldCardEditorElement wrapIntoEditorElement(WorldCardElement element, LayoutEditorScreen screen) {
        return new WorldCardEditorElement(element, this, screen);
    }
    
    @Override
    public SerializedElement serializeElement(WorldCardElement element, SerializedElement serialized) {
        try {
            // Layout configuration
            serialized.getProperties().put("cards_per_row", String.valueOf(element.getCardsPerRow()));
            serialized.getProperties().put("card_spacing", String.valueOf(element.getCardSpacing()));
            serialized.getProperties().put("card_width", String.valueOf(element.getCardWidth()));
            serialized.getProperties().put("card_height", String.valueOf(element.getCardHeight()));
            serialized.getProperties().put("auto_layout", String.valueOf(element.isAutoLayout()));
            
            // Appearance settings
            serialized.getProperties().put("background_color", String.valueOf(element.getBackgroundColor()));
            serialized.getProperties().put("text_color", String.valueOf(element.getTextColor()));
            serialized.getProperties().put("font_size", String.valueOf(element.getFontSize()));
            
            // Information display
            serialized.getProperties().put("show_last_played", String.valueOf(element.isShowLastPlayed()));
            serialized.getProperties().put("show_world_size", String.valueOf(element.isShowWorldSize()));
            serialized.getProperties().put("show_game_mode", String.valueOf(element.isShowGameMode()));
            
            // Button configuration
            serialized.getProperties().put("show_buttons", String.valueOf(element.isShowButtons()));
            serialized.getProperties().put("show_play_button", String.valueOf(element.isShowPlayButton()));
            serialized.getProperties().put("show_edit_button", String.valueOf(element.isShowEditButton()));
            serialized.getProperties().put("show_delete_button", String.valueOf(element.isShowDeleteButton()));
            
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize world card element properties", e);
        }
        
        return serialized;
    }
    
    /**
     * FancyMenu v3 expects Text return type
     */
    public Text getDisplayName(de.keksuccino.fancymenu.customization.element.AbstractElement element) {
        if (element instanceof WorldCardElement) {
            WorldCardElement worldCard = (WorldCardElement) element;
            return Text.literal("World Cards (" + worldCard.getWorldCount() + " worlds)");
        } else {
            return Text.literal("Dynamic World Cards");
        }
    }
    
    /**
     * FancyMenu v3 expects Text[] return type
     */
    public Text[] getDescription(de.keksuccino.fancymenu.customization.element.AbstractElement element) {
        return new Text[] {
            Text.literal("Automatically creates cards for all worlds"),
            Text.literal("Dynamically adjusts layout based on world count"),
            Text.literal("Shows world name, gamemode, last played, size"),
            Text.literal("Configurable grid layout and spacing"),
            Text.literal("Live updates when worlds change")
        };
    }
    
    /**
     * Get custom properties for the right-click context menu
     * This should integrate with FancyMenu's property system
     */
    public List<String> getCustomProperties() {
        List<String> properties = new ArrayList<>();
        properties.add("Card Layout");
        properties.add("Card Appearance"); 
        properties.add("Information Display");
        properties.add("Button Configuration");
        properties.add("Refresh Data");
        return properties;
    }
    
    /**
     * Handle when a custom property is selected from the right-click menu
     */
    public void handleCustomProperty(String propertyName, WorldCardElement element) {
        switch (propertyName) {
            case "Card Layout":
                // Open layout configuration
                configureLayout(element);
                break;
            case "Card Appearance":
                // Open appearance configuration
                configureAppearance(element);
                break;
            case "Information Display":
                // Open information display configuration
                configureInformation(element);
                break;
            case "Button Configuration":
                // Open button configuration
                configureButtons(element);
                break;
            case "Refresh Data":
                // Refresh world data
                refreshWorldData();
                break;
        }
    }
    
    private void configureLayout(WorldCardElement element) {
        // Cycle through layout options
        if (element.isAutoLayout()) {
            element.setAutoLayout(false);
            element.setCardsPerRow(2);
        } else {
            int current = element.getCardsPerRow();
            if (current >= 6) {
                element.setAutoLayout(true);
            } else {
                element.setCardsPerRow(current + 1);
            }
        }
    }
    
    private void configureAppearance(WorldCardElement element) {
        // Cycle through themes
        int currentBg = element.getBackgroundColor();
        if (currentBg == 0x88222222) {
            // Default -> Dark
            element.setBackgroundColor(0x88333333);
            element.setTextColor(0xFFFFFFFF);
        } else if (currentBg == 0x88333333) {
            // Dark -> Blue
            element.setBackgroundColor(0x88334455);
            element.setTextColor(0xFFFFFFFF);
        } else {
            // Blue -> Default
            element.setBackgroundColor(0x88222222);
            element.setTextColor(0xFFFFFFFF);
        }
    }
    
    private void configureInformation(WorldCardElement element) {
        // Toggle information display modes
        if (element.isShowLastPlayed() && element.isShowWorldSize()) {
            // All -> Essential
            element.setShowLastPlayed(true);
            element.setShowWorldSize(false);
        } else if (element.isShowLastPlayed()) {
            // Essential -> Minimal
            element.setShowLastPlayed(false);
            element.setShowWorldSize(false);
        } else {
            // Minimal -> All
            element.setShowLastPlayed(true);
            element.setShowWorldSize(true);
        }
    }
    
    private void configureButtons(WorldCardElement element) {
        // Toggle button display
        element.setShowButtons(!element.isShowButtons());
    }
    
    private void refreshWorldData() {
        try {
            com.fancymenu.worldpanels.managers.WorldDataManager.getInstance().refreshWorlds();
        } catch (Exception e) {
            // Handle error
        }
    }
    
    public boolean isResizable() {
        return true;
    }
    
    public boolean isMoveable() {
        return true;
    }
}