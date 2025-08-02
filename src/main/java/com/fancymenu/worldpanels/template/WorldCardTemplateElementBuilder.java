package com.fancymenu.worldpanels.template;

import com.fancymenu.worldpanels.elements.WorldCardTemplateEditorElement;
import com.fancymenu.worldpanels.elements.WorldCardTemplateElement;
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
 * Builder for WorldCardTemplateElement - creates template containers like ticker/animator.
 * This template element is visible in the FancyMenu editor but hidden during gameplay.
 * Other elements anchored to this template become part of the world card layout.
 */
@Environment(EnvType.CLIENT)
public class WorldCardTemplateElementBuilder extends ElementBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardTemplateElementBuilder.class);
    
    public static final String ELEMENT_TYPE = "world_card_template";
    
    public WorldCardTemplateElementBuilder() {
        super(ELEMENT_TYPE);
    }
    
    @Override
    public WorldCardTemplateElement buildDefaultInstance() {
        return new WorldCardTemplateElement(this);
    }
    
    @Override
    public AbstractElement deserializeElement(SerializedElement serialized) {
        WorldCardTemplateElement element = new WorldCardTemplateElement(this);
        
        try {
            // Template properties
            if (serialized.getProperties().containsKey("template_name")) {
                element.setTemplateName(serialized.getProperties().get("template_name"));
            }
            if (serialized.getProperties().containsKey("show_in_editor")) {
                element.setShowInEditor(Boolean.parseBoolean(serialized.getProperties().get("show_in_editor")));
            }
            
            // Visual properties
            if (serialized.getProperties().containsKey("border_color")) {
                element.setBorderColor(Integer.parseInt(serialized.getProperties().get("border_color")));
            }
            if (serialized.getProperties().containsKey("background_color")) {
                element.setBackgroundColor(Integer.parseInt(serialized.getProperties().get("background_color")));
            }
            if (serialized.getProperties().containsKey("text_color")) {
                element.setTextColor(Integer.parseInt(serialized.getProperties().get("text_color")));
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize world card template element properties", e);
        }
        
        return element;
    }
    
    @Override
    public AbstractEditorElement wrapIntoEditorElement(AbstractElement element, LayoutEditorScreen screen) {
        if (element instanceof WorldCardTemplateElement) {
            return new WorldCardTemplateEditorElement((WorldCardTemplateElement) element, this, screen);
        }
        return null;
    }
    
    @Override
    public SerializedElement serializeElement(AbstractElement element, SerializedElement serialized) {
        if (element instanceof WorldCardTemplateElement) {
            WorldCardTemplateElement templateElement = (WorldCardTemplateElement) element;
            try {
                // Template properties
                serialized.getProperties().put("template_name", templateElement.getTemplateName());
                serialized.getProperties().put("show_in_editor", String.valueOf(templateElement.isShowInEditor()));
                
                // Visual properties
                serialized.getProperties().put("border_color", String.valueOf(templateElement.getBorderColor()));
                serialized.getProperties().put("background_color", String.valueOf(templateElement.getBackgroundColor()));
                serialized.getProperties().put("text_color", String.valueOf(templateElement.getTextColor()));
                
            } catch (Exception e) {
                LOGGER.warn("Failed to serialize world card template element properties", e);
            }
        }
        
        return serialized;
    }
    
    /**
     * FancyMenu v3 expects Text return type for display name
     */
    public Text getDisplayName(AbstractElement element) {
        if (element instanceof WorldCardTemplateElement) {
            WorldCardTemplateElement templateElement = (WorldCardTemplateElement) element;
            return Text.literal("Template: " + templateElement.getTemplateName());
        }
        return Text.literal("World Card Template");
    }
    
    /**
     * FancyMenu v3 expects Text[] return type for description
     */
    public Text[] getDescription(AbstractElement element) {
        return new Text[] {
            Text.literal("A template container for world card elements."),
            Text.literal("Anchor other elements to this template"),
            Text.literal("to create custom world card layouts."),
            Text.literal("Visible in editor, hidden during gameplay.")
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