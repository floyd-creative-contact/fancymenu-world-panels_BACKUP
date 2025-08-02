package com.fancymenu.worldpanels.elements;

import com.fancymenu.worldpanels.data.WorldInfo;
import com.fancymenu.worldpanels.managers.WorldDataManager;
import com.fancymenu.worldpanels.template.PlaceholderProcessor;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic World Card Element for FancyMenu v3 - Template System Integration.
 * This element finds WorldCardTemplateElements in the layout and uses them to render world cards.
 */
@Environment(EnvType.CLIENT)
public class WorldCardElement extends AbstractElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardElement.class);
    
    private final WorldDataManager worldDataManager;
    private List<WorldInfo> cachedWorlds;
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 5000; // Update every 5 seconds
    
    // Template system
    private WorldCardTemplateElement templateElement = null;
    private List<AbstractElement> templateChildren = new ArrayList<>();
    private PlaceholderProcessor placeholderProcessor;
    
    // Card layout settings
    private int cardsPerRow = 3;
    private int cardSpacing = 10;
    private boolean autoLayout = true;
    
    // Fallback appearance (when no template found)
    private int cardWidth = 200;
    private int cardHeight = 100;
    private int backgroundColor = 0x88222222;
    private int textColor = 0xFFFFFFFF;
    
    public WorldCardElement(ElementBuilder<?, ?> builder) {
        super(builder);
        try {
            this.worldDataManager = WorldDataManager.getInstance();
            this.placeholderProcessor = new PlaceholderProcessor();
        } catch (Exception e) {
            LOGGER.error("Failed to get WorldDataManager instance", e);
            throw new RuntimeException("WorldDataManager not available", e);
        }
        
        // Set reasonable defaults
        this.baseWidth = 640;
        this.baseHeight = 400;
        
        // Load initial world data
        updateWorldCache();
        
        LOGGER.info("WorldCardElement initialized - will search for templates when rendering");
    }
    
    /**
     * Enhanced template detection that finds real anchored elements
     */
    private void findTemplateInLayout() {
        try {
            // Get the template from static registry
            WorldCardTemplateElement newTemplate = WorldCardTemplateRegistry.getFirstTemplate();
            
            if (newTemplate != null) {
                LOGGER.info("🎯 Found WorldCardTemplateElement: {}", newTemplate.getTemplateName());
                
                // Only re-scan elements if template changed
                if (templateElement != newTemplate) {
                    LOGGER.info("📝 Template changed - rescanning anchored elements");
                    templateElement = newTemplate;
                    templateChildren = findRealAnchoredElements();
                } else if (templateChildren.isEmpty()) {
                    LOGGER.info("⚠️ Template same but no children - rescanning anchored elements");
                    templateChildren = findRealAnchoredElements();
                } else {
                    LOGGER.debug("✅ Using cached template children ({} elements)", templateChildren.size());
                }
                
                LOGGER.info("🔍 Final anchored elements count: {}", templateChildren.size());
                
                // Log what we found
                for (int i = 0; i < templateChildren.size(); i++) {
                    AbstractElement child = templateChildren.get(i);
                    LOGGER.info("📌 Anchored element {}: {} at {},{}", 
                        i, child.getClass().getSimpleName(), child.getAbsoluteX(), child.getAbsoluteY());
                }
                
                // Update our dimensions based on template
                updateLayoutFromTemplate();
            } else {
                LOGGER.info("❌ No WorldCardTemplateElement found - using fallback rendering");
                templateElement = null;
                templateChildren.clear();
            }
            
        } catch (Exception e) {
            LOGGER.warn("💥 Failed to find template in layout", e);
            templateElement = null;
            templateChildren.clear();
        }
    }
    
    /**
     * Find real anchored elements using FancyMenu's internal structure
     */
    private List<AbstractElement> findRealAnchoredElements() {
        List<AbstractElement> realElements = new ArrayList<>();
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.currentScreen == null) {
                LOGGER.debug("No current screen available");
                return realElements;
            }
            
            // Try to access FancyMenu's element list through reflection
            Object screen = client.currentScreen;
            
            // Method 1: Try to find elements through screen fields
            realElements.addAll(searchScreenForElements(screen));
            
            // Method 2: Try to find elements through FancyMenu's layout system
            if (realElements.isEmpty()) {
                realElements.addAll(searchLayoutForElements());
            }
            
            // Filter to only elements that might be anchored to our template
            List<AbstractElement> anchoredElements = new ArrayList<>();
            for (AbstractElement element : realElements) {
                if (isElementAnchoredToTemplate(element)) {
                    anchoredElements.add(element);
                    LOGGER.info("Found anchored element: {} of type {}", 
                        element.getClass().getSimpleName(), element.getClass().getName());
                }
            }
            
            return anchoredElements;
            
        } catch (Exception e) {
            LOGGER.warn("Failed to find real anchored elements: {}", e.getMessage());
            return realElements;
        }
    }
    
    /**
     * Search the current screen for FancyMenu elements
     */
    private List<AbstractElement> searchScreenForElements(Object screen) {
        List<AbstractElement> elements = new ArrayList<>();
        
        try {
            // Look for common FancyMenu field names that might contain elements
            String[] fieldNames = {
                "elements", "customElements", "fancyMenuElements", "layoutElements",
                "editorElements", "menuElements", "children", "widgets"
            };
            
            Class<?> screenClass = screen.getClass();
            for (String fieldName : fieldNames) {
                try {
                    Field field = findFieldInClassHierarchy(screenClass, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object fieldValue = field.get(screen);
                        
                        if (fieldValue instanceof List) {
                            List<?> list = (List<?>) fieldValue;
                            for (Object item : list) {
                                if (item instanceof AbstractElement) {
                                    elements.add((AbstractElement) item);
                                }
                            }
                            LOGGER.debug("Found {} elements in field: {}", list.size(), fieldName);
                        }
                    }
                } catch (Exception e) {
                    // Continue to next field
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to search screen for elements: {}", e.getMessage());
        }
        
        return elements;
    }
    
    /**
     * Search through FancyMenu's layout system for elements
     */
    private List<AbstractElement> searchLayoutForElements() {
        List<AbstractElement> elements = new ArrayList<>();
        
        try {
            // Try to access FancyMenu's layout manager or registry
            String[] classNames = {
                "de.keksuccino.fancymenu.customization.layout.LayoutManager",
                "de.keksuccino.fancymenu.customization.element.ElementRegistry",
                "de.keksuccino.fancymenu.customization.layout.Layout"
            };
            
            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    
                    // Look for static methods that might return elements
                    String[] methodNames = {"getElements", "getAllElements", "getCurrentElements", "getLayoutElements"};
                    
                    for (String methodName : methodNames) {
                        try {
                            Method method = clazz.getMethod(methodName);
                            Object result = method.invoke(null);
                            
                            if (result instanceof List) {
                                List<?> list = (List<?>) result;
                                for (Object item : list) {
                                    if (item instanceof AbstractElement) {
                                        elements.add((AbstractElement) item);
                                    }
                                }
                                LOGGER.debug("Found {} elements via {}.{}", list.size(), className, methodName);
                            }
                        } catch (Exception e) {
                            // Continue to next method
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Continue to next class
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to search layout for elements: {}", e.getMessage());
        }
        
        return elements;
    }
    
    /**
     * Find a field in the class hierarchy
     */
    private Field findFieldInClassHierarchy(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Check if an element is anchored to our template
     */
    private boolean isElementAnchoredToTemplate(AbstractElement element) {
        if (templateElement == null || element == templateElement) {
            return false;
        }
        
        try {
            // Method 1: Check if element position is relative to template
            int templateX = templateElement.getAbsoluteX();
            int templateY = templateElement.getAbsoluteY();
            int templateWidth = templateElement.getAbsoluteWidth();
            int templateHeight = templateElement.getAbsoluteHeight();
            
            int elementX = element.getAbsoluteX();
            int elementY = element.getAbsoluteY();
            
            // Check if element is within or near the template bounds
            boolean withinBounds = elementX >= templateX - 50 && elementX <= templateX + templateWidth + 50 &&
                                 elementY >= templateY - 50 && elementY <= templateY + templateHeight + 50;
            
            if (withinBounds) {
                LOGGER.debug("Element {} is within template bounds", element.getClass().getSimpleName());
                return true;
            }
            
            // Method 2: Try to access anchor information through reflection
            if (hasAnchorToTemplate(element)) {
                LOGGER.debug("Element {} has anchor to template", element.getClass().getSimpleName());
                return true;
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to check if element is anchored: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if element has anchor information pointing to template
     */
    private boolean hasAnchorToTemplate(AbstractElement element) {
        try {
            // Look for anchor-related fields
            String[] anchorFields = {"anchor", "anchorElement", "anchorPoint", "parentElement", "linkedElement"};
            
            Class<?> elementClass = element.getClass();
            for (String fieldName : anchorFields) {
                Field field = findFieldInClassHierarchy(elementClass, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object anchorValue = field.get(element);
                    
                    // Check if anchor points to our template
                    if (anchorValue == templateElement) {
                        return true;
                    }
                    
                    // Check if anchor value contains reference to our template
                    if (anchorValue != null && anchorValue.toString().contains(templateElement.toString())) {
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            // Ignore reflection errors
        }
        
        return false;
    }
    
    /**
     * Extract text from any element that might contain text
     */
    private String getTextFromElement(AbstractElement element) {
        try {
            // Method 1: Try common text field names
            String[] textFields = {"text", "content", "displayText", "label", "message"};
            
            Class<?> elementClass = element.getClass();
            for (String fieldName : textFields) {
                Field field = findFieldInClassHierarchy(elementClass, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(element);
                    if (value instanceof String) {
                        String text = (String) value;
                        LOGGER.debug("Found text in field {}: {}", fieldName, text);
                        return text;
                    }
                }
            }
            
            // Method 2: Try common text getter methods
            String[] textMethods = {"getText", "getContent", "getDisplayText", "getLabel", "getMessage"};
            
            for (String methodName : textMethods) {
                try {
                    Method method = elementClass.getMethod(methodName);
                    Object result = method.invoke(element);
                    if (result instanceof String) {
                        String text = (String) result;
                        LOGGER.debug("Found text via method {}: {}", methodName, text);
                        return text;
                    }
                } catch (Exception e) {
                    // Continue to next method
                }
            }
            
            // Method 3: Check if it's a specific FancyMenu text element type
            String className = element.getClass().getSimpleName();
            if (className.contains("Text")) {
                LOGGER.debug("Found text element type: {}", className);
                return "{world_name}"; // Default placeholder for testing
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to extract text from element: {}", e.getMessage());
        }
        
        return "Text Element";
    }
    
    /**
     * Update layout dimensions based on template size
     */
    private void updateLayoutFromTemplate() {
        if (templateElement != null) {
            cardWidth = templateElement.getTemplateWidth();
            cardHeight = templateElement.getTemplateHeight();
        }
        
        // Calculate total dimensions based on card count and layout
        if (autoLayout && cachedWorlds != null) {
            adjustLayoutForWorldCount(cachedWorlds.size());
        } else {
            this.baseWidth = (cardWidth * cardsPerRow) + (cardSpacing * (cardsPerRow - 1));
            this.baseHeight = cardHeight;
        }
    }
    
    /**
     * Main render method - searches for templates and renders world cards
     */
    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // Update world data periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate > UPDATE_INTERVAL) {
            updateWorldCache();
            findTemplateInLayout(); // Only search for templates periodically
            lastUpdate = currentTime;
        }
        
        if (cachedWorlds == null || cachedWorlds.isEmpty()) {
            renderNoWorldsMessage(drawContext, mouseX, mouseY);
            return;
        }
        
        // Render world cards using template if available, otherwise fallback
        if (templateElement != null && !templateChildren.isEmpty()) {
            renderWorldCardsWithTemplate(drawContext, mouseX, mouseY);
        } else {
            renderFallbackWorldCards(drawContext, mouseX, mouseY);
        }
    }
    
    /**
     * Render world cards using the template system
     */
    private void renderWorldCardsWithTemplate(DrawContext drawContext, int mouseX, int mouseY) {
        int startX = getAbsoluteX();
        int startY = getAbsoluteY();
        
        // Only log this occasionally, not every frame
        if (System.currentTimeMillis() - lastUpdate < 1000) {
            LOGGER.info("🎨 RENDERING {} world cards with template. Template children: {}", cachedWorlds.size(), templateChildren.size());
        }
        
        if (templateChildren.isEmpty()) {
            LOGGER.warn("⚠️ No template children found - cannot render dynamic content!");
            renderFallbackWorldCards(drawContext, mouseX, mouseY);
            return;
        }
        
        for (int i = 0; i < cachedWorlds.size(); i++) {
            WorldInfo world = cachedWorlds.get(i);
            
            // Calculate card position
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            
            int cardX = startX + (col * (cardWidth + cardSpacing));
            int cardY = startY + (row * (cardHeight + cardSpacing));
            
            // Only log this occasionally, not every frame
            if (System.currentTimeMillis() - lastUpdate < 1000) {
                LOGGER.info("🌍 Rendering card {} for world: {} at position {},{}", i, world.getWorldName(), cardX, cardY);
            }
            
            // Render template background if visible in editor
            if (templateElement.isShowInEditor()) {
                renderTemplateBackground(drawContext, cardX, cardY);
            }
            
            // Render each anchored element with placeholder processing
            for (int j = 0; j < templateChildren.size(); j++) {
                AbstractElement child = templateChildren.get(j);
                try {
                    // Only log this occasionally, not every frame
                    if (System.currentTimeMillis() - lastUpdate < 1000) {
                        LOGGER.info("🔧 Rendering template child {} ({}) for world: {}", j, child.getClass().getSimpleName(), world.getWorldName());
                    }
                    renderTemplateChild(drawContext, child, world, cardX, cardY);
                } catch (Exception e) {
                    LOGGER.error("💥 Failed to render template child {} for world: {}", j, world.getWorldName(), e);
                }
            }
        }
    }
    
    /**
     * Render template background
     */
    private void renderTemplateBackground(DrawContext drawContext, int cardX, int cardY) {
        // Render template container background
        int bgColor = templateElement.getBackgroundColor();
        if ((bgColor & 0xFF000000) != 0) { // Only if alpha > 0
            drawContext.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, bgColor);
        }
        
        // Render border
        int borderColor = templateElement.getBorderColor();
        if ((borderColor & 0xFF000000) != 0) { // Only if alpha > 0
            drawContext.drawBorder(cardX, cardY, cardWidth, cardHeight, borderColor);
        }
    }
    
    /**
     * Render a template child element with placeholder processing
     */
    private void renderTemplateChild(DrawContext drawContext, AbstractElement child, WorldInfo world, int cardX, int cardY) {
        try {
            // Calculate child position relative to template
            int templateX = templateElement.getAbsoluteX();
            int templateY = templateElement.getAbsoluteY();
            
            int relativeX = child.getAbsoluteX() - templateX;
            int relativeY = child.getAbsoluteY() - templateY;
            
            int childX = cardX + relativeX;
            int childY = cardY + relativeY;
            
            // Determine element type and render accordingly
            String childClassName = child.getClass().getSimpleName();
            String fullClassName = child.getClass().getName();
            
            // Only log this occasionally, not every frame
            if (System.currentTimeMillis() - lastUpdate < 1000) {
                LOGGER.info("🎭 Rendering child element: {} of type {}", childClassName, fullClassName);
            }
            
            // Check actual class type, not just name
            if (child instanceof WorldCardImageElement) {
                // Handle our custom world card image elements
                if (System.currentTimeMillis() - lastUpdate < 1000) {
                    LOGGER.info("✅ DETECTED WorldCardImageElement - rendering as image for world: {}", world.getWorldName());
                }
                renderWorldCardImageElement(drawContext, child, world, childX, childY);
            } else if (childClassName.contains("Image") || childClassName.contains("Picture")) {
                // Handle standard image elements
                LOGGER.debug("Detected standard image element - processing placeholders");
                renderImageElementWithPlaceholders(drawContext, child, world, childX, childY);
            } else if (childClassName.contains("Text") || childClassName.contains("Label")) {
                // Handle text elements
                LOGGER.debug("Detected text element - processing placeholders");
                renderTextElementWithPlaceholders(drawContext, child, world, childX, childY);
            } else if (childClassName.contains("Button") || childClassName.contains("Widget")) {
                // Handle button elements
                LOGGER.debug("Detected button/widget element - processing placeholders");
                renderButtonElementWithPlaceholders(drawContext, child, world, childX, childY);
            } else {
                // Try to render as the actual element type
                LOGGER.debug("Unknown element type: {} - trying direct rendering", fullClassName);
                renderElementDirectly(drawContext, child, world, childX, childY);
            }
            
        } catch (Exception e) {
            LOGGER.error("💥 Failed to render template child", e);
        }
    }
    
    /**
     * Render WorldCardImageElement with world context
     */
    private void renderWorldCardImageElement(DrawContext drawContext, AbstractElement imageElement, WorldInfo world, int x, int y) {
        try {
            // Only log this occasionally, not every frame
            if (System.currentTimeMillis() - lastUpdate < 1000) {
                LOGGER.info("🎯 RENDERING WorldCardImageElement for world: {} at position {},{}", world.getWorldName(), x, y);
            }
            
            // Set the current world on the image element
            if (imageElement instanceof WorldCardImageElement) {
                WorldCardImageElement worldImageElement = (WorldCardImageElement) imageElement;
                
                // Set the world context
                if (System.currentTimeMillis() - lastUpdate < 1000) {
                    LOGGER.info("🌍 Setting world context on image element: {}", world.getWorldName());
                }
                worldImageElement.setCurrentWorld(world);
                
                // Temporarily move element to card position
                int originalX = imageElement.getAbsoluteX();
                int originalY = imageElement.getAbsoluteY();
                
                if (System.currentTimeMillis() - lastUpdate < 1000) {
                    LOGGER.info("📍 Moving element from {},{} to {},{}", originalX, originalY, x, y);
                }
                setElementPosition(imageElement, x, y);
                
                // Call the element's render method directly
                if (System.currentTimeMillis() - lastUpdate < 1000) {
                    LOGGER.info("🎨 Calling render method on WorldCardImageElement");
                }
                worldImageElement.render(drawContext, 0, 0, 0.0f);
                
                // Restore original position
                setElementPosition(imageElement, originalX, originalY);
                
                if (System.currentTimeMillis() - lastUpdate < 1000) {
                    LOGGER.info("✅ Successfully rendered WorldCardImageElement for world: {}", world.getWorldName());
                }
            } else {
                LOGGER.error("❌ Element is not a WorldCardImageElement: {}", imageElement.getClass().getName());
            }
            
        } catch (Exception e) {
            LOGGER.error("💥 Failed to render WorldCardImageElement for world: {}", world.getWorldName(), e);
            // Fallback: render a placeholder
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                drawContext.drawText(client.textRenderer, Text.literal("[ERROR]"), x, y, 0xFFFF0000, false);
            }
        }
    }
    
    /**
     * Render image elements with world card placeholder processing
     */
    private void renderImageElementWithPlaceholders(DrawContext drawContext, AbstractElement imageElement, WorldInfo world, int x, int y) {
        try {
            // Try to render the image element directly at the correct position
            renderElementDirectly(drawContext, imageElement, world, x, y);
            
            LOGGER.debug("Rendered image element at {},{}", x, y);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render image element", e);
            // Fallback: render a placeholder
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                drawContext.drawText(client.textRenderer, Text.literal("[Image]"), x, y, 0xFF888888, false);
            }
        }
    }
    
    /**
     * Render button elements with placeholder processing
     */
    private void renderButtonElementWithPlaceholders(DrawContext drawContext, AbstractElement buttonElement, WorldInfo world, int x, int y) {
        try {
            // Process any text placeholders in button labels
            String buttonText = getTextFromElement(buttonElement);
            if (buttonText != null) {
                String processedText = PlaceholderProcessor.processPlaceholders(buttonText, world);
                
                // Try to update the button text
                setTextOnElement(buttonElement, processedText);
            }
            
            // Try to render the button element directly
            renderElementDirectly(drawContext, buttonElement, world, x, y);
            
            LOGGER.debug("Rendered button element at {},{}", x, y);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render button element", e);
            // Fallback: render as text
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                drawContext.drawText(client.textRenderer, Text.literal("[Button]"), x, y, textColor, false);
            }
        }
    }
    
    /**
     * Try to render an element directly by calling its render method
     */
    private void renderElementDirectly(DrawContext drawContext, AbstractElement element, WorldInfo world, int newX, int newY) {
        try {
            // Save original position
            int originalX = element.getAbsoluteX();
            int originalY = element.getAbsoluteY();
            
            // Temporarily move element to new position
            setElementPosition(element, newX, newY);
            
            // Try to call the element's render method
            Method renderMethod = findRenderMethod(element.getClass());
            if (renderMethod != null) {
                renderMethod.setAccessible(true);
                renderMethod.invoke(element, drawContext, 0, 0, 0.0f); // mouseX, mouseY, delta
                LOGGER.debug("Called render method directly on {}", element.getClass().getSimpleName());
            } else {
                LOGGER.debug("No render method found for {}", element.getClass().getSimpleName());
            }
            
            // Restore original position
            setElementPosition(element, originalX, originalY);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render element directly: {}", e.getMessage());
        }
    }
    
    /**
     * Set element position using reflection
     */
    private void setElementPosition(AbstractElement element, int x, int y) {
        try {
            // Try common position field names
            String[] xFields = {"x", "posX", "absoluteX", "baseX"};
            String[] yFields = {"y", "posY", "absoluteY", "baseY"};
            
            Class<?> elementClass = element.getClass();
            
            // Set X position
            for (String fieldName : xFields) {
                try {
                    Field field = findFieldInClassHierarchy(elementClass, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(element, x);
                        break;
                    }
                } catch (Exception e) {
                    // Continue to next field
                }
            }
            
            // Set Y position
            for (String fieldName : yFields) {
                try {
                    Field field = findFieldInClassHierarchy(elementClass, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(element, y);
                        break;
                    }
                } catch (Exception e) {
                    // Continue to next field
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Failed to set element position", e);
        }
    }
    
    /**
     * Set text on an element using reflection
     */
    private void setTextOnElement(AbstractElement element, String text) {
        try {
            String[] fieldNames = {"text", "content", "displayText", "label", "message"};
            
            Class<?> elementClass = element.getClass();
            for (String fieldName : fieldNames) {
                try {
                    Field field = findFieldInClassHierarchy(elementClass, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(element, text);
                        LOGGER.debug("Set text using field: {}", fieldName);
                        return;
                    }
                } catch (Exception e) {
                    // Continue to next field
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to set text on element", e);
        }
    }
    
    /**
     * Find render method in element class hierarchy
     */
    private Method findRenderMethod(Class<?> clazz) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                // Try different render method signatures
                try {
                    return currentClass.getDeclaredMethod("render", DrawContext.class, int.class, int.class, float.class);
                } catch (NoSuchMethodException e1) {
                    try {
                        return currentClass.getDeclaredMethod("render", Object.class, int.class, int.class, float.class);
                    } catch (NoSuchMethodException e2) {
                        // Continue to parent class
                    }
                }
            } catch (Exception e) {
                // Continue to parent class
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }
    
    /**
     * Enhanced text rendering with real element text extraction
     */
    private void renderTextElementWithPlaceholders(DrawContext drawContext, AbstractElement textElement, WorldInfo world, int x, int y) {
        try {
            // Skip rendering text for WorldCardImageElement - it handles its own rendering
            if (textElement instanceof WorldCardImageElement) {
                LOGGER.debug("Skipping text rendering for WorldCardImageElement - handled separately");
                return;
            }
            
            // Get the original text from the element
            String originalText = getTextFromElement(textElement);
            
            // Process placeholders using enhanced processor
            String processedText = PlaceholderProcessor.processPlaceholders(originalText, world);
            
            // Render the processed text
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                drawContext.drawText(client.textRenderer, Text.literal(processedText), x, y, textColor, false);
            }
            
            LOGGER.debug("Rendered text: {} -> {}", originalText, processedText);
            
        } catch (Exception e) {
            LOGGER.debug("Failed to render text with placeholders", e);
            // Fallback rendering
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.textRenderer != null) {
                drawContext.drawText(client.textRenderer, Text.literal("Text Element"), x, y, textColor, false);
            }
        }
    }
    
    /**
     * Render fallback world cards when no template is available
     */
    private void renderFallbackWorldCards(DrawContext drawContext, int mouseX, int mouseY) {
        int startX = getAbsoluteX();
        int startY = getAbsoluteY();
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) return;
        
        for (int i = 0; i < cachedWorlds.size(); i++) {
            WorldInfo world = cachedWorlds.get(i);
            
            // Calculate card position
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            
            int cardX = startX + (col * (cardWidth + cardSpacing));
            int cardY = startY + (row * (cardHeight + cardSpacing));
            
            // Render fallback card
            drawContext.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, backgroundColor);
            drawContext.drawBorder(cardX, cardY, cardWidth, cardHeight, 0xFF666666);
            
            // Render world name
            String worldName = world.getWorldName();
            if (worldName.length() > 20) {
                worldName = worldName.substring(0, 17) + "...";
            }
            drawContext.drawText(client.textRenderer, Text.literal(worldName), cardX + 5, cardY + 5, textColor, false);
            
            // Render additional info
            drawContext.drawText(client.textRenderer, Text.literal("Mode: " + world.getGameMode()), cardX + 5, cardY + 20, 0xFFCCCCCC, false);
            drawContext.drawText(client.textRenderer, Text.literal("Last: " + world.getLastPlayed()), cardX + 5, cardY + 35, 0xFFCCCCCC, false);
        }
    }
    
    /**
     * Handle mouse click for interactions
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && cachedWorlds != null) { // Left click
            int startX = getAbsoluteX();
            int startY = getAbsoluteY();
            
            for (int i = 0; i < cachedWorlds.size(); i++) {
                WorldInfo world = cachedWorlds.get(i);
                
                // Calculate card position
                int row = i / cardsPerRow;
                int col = i % cardsPerRow;
                
                int cardX = startX + (col * (cardWidth + cardSpacing));
                int cardY = startY + (row * (cardHeight + cardSpacing));
                
                // Check if click is within this card
                if (mouseX >= cardX && mouseX <= cardX + cardWidth && 
                    mouseY >= cardY && mouseY <= cardY + cardHeight) {
                    
                    handleWorldCardClick(world);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Handle world card click
     */
    private void handleWorldCardClick(WorldInfo world) {
        try {
            LOGGER.info("World card clicked: {}", world.getWorldName());
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("§aClicked world: " + world.getWorldName()), false);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle world card click", e);
        }
    }
    
    /**
     * Update the cached world data
     */
    private void updateWorldCache() {
        try {
            if (worldDataManager != null && worldDataManager.isInitialized()) {
                cachedWorlds = worldDataManager.getWorlds();
                
                // Auto-adjust layout based on world count
                if (autoLayout && cachedWorlds != null) {
                    adjustLayoutForWorldCount(cachedWorlds.size());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to update world cache", e);
        }
    }
    
    /**
     * Automatically adjust layout based on number of worlds
     */
    private void adjustLayoutForWorldCount(int worldCount) {
        if (worldCount <= 3) {
            cardsPerRow = worldCount;
            this.baseWidth = (cardWidth * worldCount) + (cardSpacing * (worldCount - 1));
            this.baseHeight = cardHeight;
        } else if (worldCount <= 6) {
            cardsPerRow = 3;
            this.baseWidth = (cardWidth * 3) + (cardSpacing * 2);
            this.baseHeight = (cardHeight * 2) + cardSpacing;
        } else {
            cardsPerRow = 3;
            int rows = (int) Math.ceil((double) worldCount / 3);
            this.baseWidth = (cardWidth * 3) + (cardSpacing * 2);
            this.baseHeight = (cardHeight * rows) + (cardSpacing * (rows - 1));
        }
    }
    
    /**
     * Render message when no worlds are found
     */
    private void renderNoWorldsMessage(DrawContext drawContext, int mouseX, int mouseY) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null) return;
            
            int centerX = getAbsoluteX() + (getAbsoluteWidth() / 2);
            int centerY = getAbsoluteY() + (getAbsoluteHeight() / 2);
            
            if (templateElement == null) {
                drawContext.drawText(client.textRenderer, Text.literal("No World Card Template found"), centerX - 80, centerY - 30, 0xFFFF6666, false);
                drawContext.drawText(client.textRenderer, Text.literal("Add a World Card Template element"), centerX - 90, centerY - 15, 0xFFCCCCCC, false);
                drawContext.drawText(client.textRenderer, Text.literal("Then anchor text/buttons to it"), centerX - 80, centerY, 0xFFCCCCCC, false);
                drawContext.drawText(client.textRenderer, Text.literal("Use placeholders like {world_name}"), centerX - 85, centerY + 15, 0xFF99CCFF, false);
            } else {
                drawContext.drawText(client.textRenderer, Text.literal("No worlds found"), centerX - 50, centerY - 10, 0xFFFFFFFF, false);
                drawContext.drawText(client.textRenderer, Text.literal("Create a world to see it here"), centerX - 80, centerY + 5, 0xFFCCCCCC, false);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to render no worlds message", e);
        }
    }
    
    // ====================================
    // STATIC REGISTRY FOR TESTING
    // ====================================
    
    /**
     * Static registry to track template instances (Phase 1 approach)
     */
    public static class WorldCardTemplateRegistry {
        private static WorldCardTemplateElement currentTemplate = null;
        
        public static void registerTemplate(WorldCardTemplateElement template) {
            currentTemplate = template;
            LOGGER.info("Template registered: {}", template.getTemplateName());
        }
        
        public static void unregisterTemplate(WorldCardTemplateElement template) {
            if (currentTemplate == template) {
                currentTemplate = null;
                LOGGER.info("Template unregistered");
            }
        }
        
        public static WorldCardTemplateElement getFirstTemplate() {
            return currentTemplate;
        }
    }
    
    // ====================================
    // COMPATIBILITY METHODS
    // ====================================
    
    // Configuration methods
    public void setCardsPerRow(int cardsPerRow) {
        this.cardsPerRow = Math.max(1, Math.min(6, cardsPerRow));
        this.autoLayout = false;
        updateLayoutFromTemplate();
    }
    
    public void setCardSpacing(int spacing) {
        this.cardSpacing = Math.max(0, Math.min(50, spacing));
        updateLayoutFromTemplate();
    }
    
    public void setAutoLayout(boolean auto) {
        this.autoLayout = auto;
        if (auto && cachedWorlds != null) {
            adjustLayoutForWorldCount(cachedWorlds.size());
        }
    }
    
    // Missing methods that other files depend on
    public void setCardSize(int width, int height) {
        this.cardWidth = Math.max(100, Math.min(400, width));
        this.cardHeight = Math.max(50, Math.min(200, height));
        updateLayoutFromTemplate();
    }
    
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }
    
    public void setTextColor(int color) {
        this.textColor = color;
    }
    
    public void setFontSize(int size) {
        // Store font size (placeholder implementation)
    }
    
    public void setHoverColor(int color) {
        // Store hover color (placeholder implementation)
    }
    
    public void setBorderColor(int color) {
        // Store border color (placeholder implementation)
    }
    
    public void setActiveColor(int color) {
        // Store active color (placeholder implementation)
    }
    
    // Legacy information display toggles
    public void setShowLastPlayed(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowWorldSize(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowGameMode(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowDifficulty(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowVersion(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowSeed(boolean show) {
        // Placeholder implementation
    }
    
    // Legacy button configuration
    public void setShowButtons(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowPlayButton(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowEditButton(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowDeleteButton(boolean show) {
        // Placeholder implementation
    }
    
    public void setShowWorldIcons(boolean show) {
        // Placeholder implementation
    }
    
    public void setBackgroundImage(String imagePath) {
        // Placeholder implementation
    }
    
    public void setUseBackgroundImage(boolean use) {
        // Placeholder implementation
    }
    
    // Getters - SINGLE INSTANCE OF EACH METHOD
    public int getCardWidth() { 
        return cardWidth; 
    }
    
    public int getCardHeight() { 
        return cardHeight; 
    }
    
    public int getBackgroundColor() { 
        return backgroundColor; 
    }
    
    public int getTextColor() { 
        return textColor; 
    }
    
    public int getFontSize() { 
        return 12; // Default font size
    }
    
    public boolean isShowLastPlayed() { 
        return true; // Default to true
    }
    
    public boolean isShowWorldSize() { 
        return true; // Default to true
    }
    
    public boolean isShowGameMode() { 
        return true; // Default to true
    }
    
    public boolean isShowDifficulty() { 
        return true; // Default to true
    }
    
    public boolean isShowVersion() { 
        return false; // Default to false
    }
    
    public boolean isShowSeed() { 
        return false; // Default to false
    }
    
    public boolean isShowButtons() { 
        return true; // Default to true
    }
    
    public boolean isShowPlayButton() { 
        return true; // Default to true
    }
    
    public boolean isShowEditButton() { 
        return true; // Default to true
    }
    
    public boolean isShowDeleteButton() { 
        return false; // Default to false for safety
    }
    
    public boolean isShowWorldIcons() { 
        return true; // Default to true
    }
    
    // Other getters
    public int getCardsPerRow() { return cardsPerRow; }
    public int getCardSpacing() { return cardSpacing; }
    public boolean isAutoLayout() { return autoLayout; }
    public int getWorldCount() { return cachedWorlds != null ? cachedWorlds.size() : 0; }
    public boolean hasTemplate() { return templateElement != null; }
    
    /**
     * Override getDisplayName to return Text (FancyMenu v3 expects this)
     */
    @Override
    public Text getDisplayName() {
        String templateInfo = templateElement != null ? 
            " [" + templateElement.getTemplateName() + "]" : " [No Template]";
        return Text.literal("World Cards" + templateInfo + " (" + getWorldCount() + " worlds)");
    }
}