package com.fancymenu.worldpanels.elements;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder for WorldCardImageElement - creates custom image elements for world cards.
 */
@Environment(EnvType.CLIENT)
public class WorldCardImageElementBuilder extends ElementBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardImageElementBuilder.class);
    
    public static final String ELEMENT_TYPE = "world_card_image";
    
    public WorldCardImageElementBuilder() {
        super(ELEMENT_TYPE);
    }
    
    @Override
    public WorldCardImageElement buildDefaultInstance() {
        return new WorldCardImageElement(this);
    }
    
    @Override
    public AbstractElement deserializeElement(SerializedElement serialized) {
        WorldCardImageElement element = new WorldCardImageElement(this);
        
        try {
            // Image type
            if (serialized.getProperties().containsKey("image_type")) {
                String imageTypeName = serialized.getProperties().get("image_type");
                try {
                    WorldCardImageElement.WorldCardImageType imageType = 
                        WorldCardImageElement.WorldCardImageType.valueOf(imageTypeName.toUpperCase());
                    element.setImageType(imageType);
                } catch (Exception e) {
                    LOGGER.warn("Invalid image type: {}", imageTypeName);
                }
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize world card image element properties", e);
        }
        
        return element;
    }
    
    @Override
    public AbstractEditorElement wrapIntoEditorElement(AbstractElement element, LayoutEditorScreen screen) {
        if (element instanceof WorldCardImageElement) {
            return new WorldCardImageEditorElement((WorldCardImageElement) element, this, screen);
        }
        return null;
    }
    
    @Override
    public SerializedElement serializeElement(AbstractElement element, SerializedElement serialized) {
        if (element instanceof WorldCardImageElement) {
            WorldCardImageElement imageElement = (WorldCardImageElement) element;
            try {
                // Image type
                serialized.getProperties().put("image_type", imageElement.getImageType().name());
                
            } catch (Exception e) {
                LOGGER.warn("Failed to serialize world card image element properties", e);
            }
        }
        
        return serialized;
    }
    
    /**
     * FancyMenu v3 expects Text return type for display name
     */
    public Text getDisplayName(AbstractElement element) {
        if (element instanceof WorldCardImageElement) {
            WorldCardImageElement imageElement = (WorldCardImageElement) element;
            return Text.literal("World Card Image: " + imageElement.getImageType().getDisplayName());
        }
        return Text.literal("World Card Image");
    }
    
    /**
     * FancyMenu v3 expects Text[] return type for description
     */
    public Text[] getDescription(AbstractElement element) {
        return new Text[] {
            Text.literal("Dynamic image element for world cards."),
            Text.literal("Automatically shows different images"),
            Text.literal("based on world data and selected type."),
            Text.literal("No placeholder configuration needed!")
        };
    }
    
    public String getCategory() {
        return "World Cards";
    }
    
    public boolean isResizable() {
        return true;
    }
    
    public boolean isMoveable() {
        return true;
    }
}