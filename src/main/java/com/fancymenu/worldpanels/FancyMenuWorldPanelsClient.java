package com.fancymenu.worldpanels;

import com.fancymenu.worldpanels.elements.WorldCardElementBuilder;
import com.fancymenu.worldpanels.exporters.WorldDataExporter;
import com.fancymenu.worldpanels.managers.WorldDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main client mod initializer for FancyMenu World Panels.
 * Provides dynamic world card elements and enhanced JSON exports.
 */
@Environment(EnvType.CLIENT)
public class FancyMenuWorldPanelsClient implements ClientModInitializer {
    public static final String MOD_ID = "fancymenu-world-panels";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static boolean initialized = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing FancyMenu World Panels with Dynamic World Cards");
        
        try {
            // Initialize core managers first
            WorldDataManager.initialize();
            WorldDataExporter.initialize();
            
            // Try to register world card element with FancyMenu
            registerWorldCardElement();
            
            // Register client lifecycle events
            ClientLifecycleEvents.CLIENT_STARTED.register(this::onClientStarted);
            ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStopping);
            
            // Register tick events for periodic updates
            ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
            
            initialized = true;
            LOGGER.info("FancyMenu World Panels initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize FancyMenu World Panels", e);
        }
    }
    
    /**
     * Try to register the world card element with FancyMenu v3
     */
    private void registerWorldCardElement() {
        try {
            // Check if FancyMenu is available
            Class.forName("de.keksuccino.fancymenu.customization.element.ElementRegistry");
            
            WorldCardElementBuilder builder = new WorldCardElementBuilder();
            
            // Try the static register method (common in FancyMenu v3)
            String[] methodNames = {"register", "registerElement", "registerBuilder", "addElement", "addBuilder"};
            Class<?> registryClass = Class.forName("de.keksuccino.fancymenu.customization.element.ElementRegistry");
            
            boolean worldCardRegistered = false;
            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = registryClass.getMethod(methodName, builder.getClass().getSuperclass());
                    method.invoke(null, builder);
                    LOGGER.info("✅ Registered Dynamic World Cards element using {}", methodName);
                    worldCardRegistered = true;
                    break;
                } catch (Exception e) {
                    LOGGER.debug("{} method failed: {}", methodName, e.getMessage());
                }
            }
            
            if (!worldCardRegistered) {
                // Try with different parameter types
                try {
                    java.lang.reflect.Method method = registryClass.getMethod("register", String.class, builder.getClass());
                    method.invoke(null, "worldcard", builder);
                    LOGGER.info("✅ Registered Dynamic World Cards element using register(String, Builder)");
                    worldCardRegistered = true;
                } catch (Exception e) {
                    LOGGER.debug("register(String, Builder) method failed: {}", e.getMessage());
                }
            }
            
            // ==========================================
            // Register WorldCardTemplateElement
            // ==========================================
            if (worldCardRegistered) {
                try {
                    com.fancymenu.worldpanels.template.WorldCardTemplateElementBuilder templateBuilder = 
                        new com.fancymenu.worldpanels.template.WorldCardTemplateElementBuilder();
                    
                    // Try the same registration methods for the template element
                    boolean templateRegistered = false;
                    for (String methodName : methodNames) {
                        try {
                            java.lang.reflect.Method method = registryClass.getMethod(methodName, templateBuilder.getClass().getSuperclass());
                            method.invoke(null, templateBuilder);
                            LOGGER.info("✅ Registered World Card Template element using {}", methodName);
                            templateRegistered = true;
                            break;
                        } catch (Exception e) {
                            LOGGER.debug("Template {} method failed: {}", methodName, e.getMessage());
                        }
                    }
                    
                    if (!templateRegistered) {
                        // Try with different parameter types
                        try {
                            java.lang.reflect.Method method = registryClass.getMethod("register", String.class, templateBuilder.getClass());
                            method.invoke(null, "world_card_template", templateBuilder);
                            LOGGER.info("✅ Registered World Card Template element using register(String, Builder)");
                            templateRegistered = true;
                        } catch (Exception e) {
                            LOGGER.debug("Template register(String, Builder) method failed: {}", e.getMessage());
                        }
                    }
                    
                    if (!templateRegistered) {
                        LOGGER.warn("❌ Could not register World Card Template element");
                    }
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to register World Card Template element: {}", e.getMessage());
                }
            }
            
            // ==========================================
            // Register WorldCardImageElement (Custom Image Element)
            // ==========================================
            if (worldCardRegistered) {
                try {
                    com.fancymenu.worldpanels.elements.WorldCardImageElementBuilder imageBuilder = 
                        new com.fancymenu.worldpanels.elements.WorldCardImageElementBuilder();
                    
                    // Try the same registration methods for the image element
                    boolean imageRegistered = false;
                    for (String methodName : methodNames) {
                        try {
                            java.lang.reflect.Method method = registryClass.getMethod(methodName, imageBuilder.getClass().getSuperclass());
                            method.invoke(null, imageBuilder);
                            LOGGER.info("✅ Registered World Card Image element using {}", methodName);
                            imageRegistered = true;
                            break;
                        } catch (Exception e) {
                            LOGGER.debug("Image {} method failed: {}", methodName, e.getMessage());
                        }
                    }
                    
                    if (!imageRegistered) {
                        // Try with different parameter types
                        try {
                            java.lang.reflect.Method method = registryClass.getMethod("register", String.class, imageBuilder.getClass());
                            method.invoke(null, "world_card_image", imageBuilder);
                            LOGGER.info("✅ Registered World Card Image element using register(String, Builder)");
                            imageRegistered = true;
                        } catch (Exception e) {
                            LOGGER.debug("Image register(String, Builder) method failed: {}", e.getMessage());
                        }
                    }
                    
                    if (!imageRegistered) {
                        LOGGER.warn("❌ Could not register World Card Image element");
                    }
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to register World Card Image element: {}", e.getMessage());
                }
            }
            
            // ==========================================
            // World Card Images Available Through Custom Element  
            // ==========================================
            if (worldCardRegistered) {
                LOGGER.info("🖼️ World Card images available through custom element:");
                LOGGER.info("   • Look for 'World Card Image' element in FancyMenu editor");
                LOGGER.info("   • Right-click element to choose image type:");
                LOGGER.info("     - World Screenshot (auto-generated preview)");
                LOGGER.info("     - World Icon (custom icon or screenshot fallback)");
                LOGGER.info("     - Smart Selection (best available image)");
                LOGGER.info("     - Game Mode Icon (based on game mode)");
                LOGGER.info("     - Status Icon (based on world status)");
            }
            
            if (!worldCardRegistered) {
                // If we get here, none of the registration methods worked
                LOGGER.warn("❌ Could not find a suitable registration method for FancyMenu elements");
                LOGGER.warn("Available ElementRegistry methods:");
                
                // Debug: List all available methods
                try {
                    java.lang.reflect.Method[] methods = registryClass.getMethods();
                    for (java.lang.reflect.Method method : methods) {
                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
                            java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                            LOGGER.warn("  - {} ({})", method.getName(), java.util.Arrays.toString(method.getParameterTypes()));
                        }
                    }
                } catch (Exception debugE) {
                    LOGGER.warn("Could not list ElementRegistry methods");
                }
                
                LOGGER.warn("Dynamic World Cards won't appear in FancyMenu editor, but JSON export will still work");
            }
            
        } catch (ClassNotFoundException e) {
            LOGGER.info("FancyMenu not detected - World Cards will work for JSON export only");
        } catch (Exception e) {
            LOGGER.warn("Failed to register World Cards element with FancyMenu: {}", e.getMessage());
            LOGGER.info("World Cards will still work for JSON export");
        }
    }
    
    private void onClientStarted(MinecraftClient client) {
        LOGGER.info("Client started - updating world data");
        updateWorldData();
        logSuccessMessage();
    }
    
    private void onClientStopping(MinecraftClient client) {
        LOGGER.info("Client stopping - performing final world data export");
        updateWorldData();
        
        // Shutdown managers
        try {
            if (WorldDataExporter.getInstance().isInitialized()) {
                WorldDataExporter.getInstance().shutdown();
            }
            if (WorldDataManager.getInstance().isInitialized()) {
                WorldDataManager.getInstance().shutdown();
            }
        } catch (Exception e) {
            LOGGER.warn("Error during shutdown", e);
        }
    }
    
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 100; // Update every 5 seconds (20 ticks/sec)
    
    private void onClientTick(MinecraftClient client) {
        if (!initialized) return;
        
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            updateWorldData();
            tickCounter = 0;
        }
    }
    
    /**
     * Update world data and export JSON files
     */
    private void updateWorldData() {
        try {
            // Force refresh of world data
            if (WorldDataManager.getInstance().isInitialized()) {
                WorldDataManager.getInstance().refreshWorlds();
            }
            
            // Force export
            if (WorldDataExporter.getInstance().isInitialized()) {
                WorldDataExporter.getInstance().forceExport();
            }
            
            LOGGER.debug("World data updated and exported successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to update world data", e);
        }
    }
    
    /**
     * Log success message with instructions
     */
    private void logSuccessMessage() {
        try {
            int worldCount = WorldDataManager.getInstance().getWorlds().size();
            LOGGER.info("🎉 FancyMenu World Panels is working!");
            LOGGER.info("📊 Found {} worlds - JSON files exported", worldCount);
            LOGGER.info("🎮 Look for 'Dynamic World Cards' element in FancyMenu editor");
            LOGGER.info("🎯 Look for 'World Card Template' element for templates");
            LOGGER.info("🖼️ Look for 'World Card Image' element for dynamic images");
            LOGGER.info("📁 JSON files available at: config/fancymenu/assets/worlddata.json");
        } catch (Exception e) {
            LOGGER.debug("Failed to log success message", e);
        }
    }
    
    /**
     * Get the world data manager instance
     */
    public static WorldDataManager getWorldDataManager() {
        try {
            return WorldDataManager.getInstance();
        } catch (Exception e) {
            LOGGER.warn("WorldDataManager not available", e);
            return null;
        }
    }
    
    /**
     * Get the world data exporter instance
     */
    public static WorldDataExporter getWorldDataExporter() {
        try {
            return WorldDataExporter.getInstance();
        } catch (Exception e) {
            LOGGER.warn("WorldDataExporter not available", e);
            return null;
        }
    }
    
    /**
     * Check if the mod is properly initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Force an immediate world data update
     */
    public static void forceUpdate() {
        if (initialized) {
            try {
                if (WorldDataManager.getInstance().isInitialized()) {
                    WorldDataManager.getInstance().refreshWorlds();
                }
                if (WorldDataExporter.getInstance().isInitialized()) {
                    WorldDataExporter.getInstance().forceExport();
                }
                LOGGER.info("Forced world data update completed");
            } catch (Exception e) {
                LOGGER.error("Failed to force update world data", e);
            }
        }
    }
}