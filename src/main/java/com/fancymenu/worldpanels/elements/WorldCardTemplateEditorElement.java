package com.fancymenu.worldpanels.elements;

import com.fancymenu.worldpanels.template.WorldCardTemplateElementBuilder;
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
 * Editor for WorldCardTemplateElement - provides configuration options for the template container.
 */
@Environment(EnvType.CLIENT)
public class WorldCardTemplateEditorElement extends AbstractEditorElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardTemplateEditorElement.class);
    
    private final WorldCardTemplateElement templateElement;
    private final WorldCardTemplateElementBuilder builder;
    
    public WorldCardTemplateEditorElement(WorldCardTemplateElement element, WorldCardTemplateElementBuilder builder, LayoutEditorScreen screen) {
        super(element, screen);
        this.templateElement = element;
        this.builder = builder;
    }
    
    @Override
    public void init() {
        // Call parent init first
        super.init();
        
        // Add separator before our options
        this.rightClickMenu.addSeparatorEntry("template_separator");
        
        // Template configuration options
        createTemplateConfigurationMenu();
        
        // Template preview and help
        createTemplateHelpMenu();
    }
    
    /**
     * Create template configuration submenu
     */
    private void createTemplateConfigurationMenu() {
        ContextMenu templateMenu = new ContextMenu();
        
        // Template name configuration
        this.addGenericStringInputContextMenuEntryTo(templateMenu, "template_name",
            element -> element instanceof WorldCardTemplateEditorElement,
            consumes -> ((WorldCardTemplateEditorElement)consumes).templateElement.getTemplateName(),
            (element, name) -> {
                ((WorldCardTemplateEditorElement)element).templateElement.setTemplateName(name);
                LOGGER.info("Template name changed to: {}", name);
            },
            null, false, false,
            Text.literal("Template Name"),
            true, "World Card Template", 
            name -> name != null && !name.trim().isEmpty(),
            null);
        
        templateMenu.addSeparatorEntry("appearance_separator");
        
        // Background color
        this.addGenericStringInputContextMenuEntryTo(templateMenu, "background_color",
            element -> element instanceof WorldCardTemplateEditorElement,
            consumes -> String.format("#%06X", ((WorldCardTemplateEditorElement)consumes).templateElement.getBackgroundColor() & 0xFFFFFF),
            (element, hexColor) -> {
                try {
                    int color = Integer.parseInt(hexColor.replace("#", ""), 16) | 0x40000000; // Semi-transparent
                    ((WorldCardTemplateEditorElement)element).templateElement.setBackgroundColor(color);
                    LOGGER.info("Background color changed to: {}", hexColor);
                } catch (Exception e) {
                    LOGGER.warn("Invalid color format: {}", hexColor);
                }
            },
            null, false, false,
            Text.literal("Background Color"),
            true, "#00AAFF",
            color -> color.matches("#[0-9A-Fa-f]{6}"),
            null);
        
        // Border color
        this.addGenericStringInputContextMenuEntryTo(templateMenu, "border_color",
            element -> element instanceof WorldCardTemplateEditorElement,
            consumes -> String.format("#%06X", ((WorldCardTemplateEditorElement)consumes).templateElement.getBorderColor() & 0xFFFFFF),
            (element, hexColor) -> {
                try {
                    int color = Integer.parseInt(hexColor.replace("#", ""), 16) | 0xFF000000;
                    ((WorldCardTemplateEditorElement)element).templateElement.setBorderColor(color);
                    LOGGER.info("Border color changed to: {}", hexColor);
                } catch (Exception e) {
                    LOGGER.warn("Invalid color format: {}", hexColor);
                }
            },
            null, false, false,
            Text.literal("Border Color"),
            true, "#00AAFF",
            color -> color.matches("#[0-9A-Fa-f]{6}"),
            null);
        
        // Show in editor toggle
        this.addToggleContextMenuEntryTo(templateMenu, "show_in_editor", WorldCardTemplateEditorElement.class,
            consumes -> consumes.templateElement.isShowInEditor(),
            (element, value) -> {
                element.templateElement.setShowInEditor(value);
                LOGGER.info("Show in editor: {}", value);
            },
            "Show Template Container in Editor");
        
        templateMenu.addSeparatorEntry("dynamic_separator");
        
        // Dynamic images toggle
        this.addToggleContextMenuEntryTo(templateMenu, "enable_dynamic_images", WorldCardTemplateEditorElement.class,
            consumes -> consumes.templateElement.isDynamicImagesEnabled(),
            (element, value) -> {
                element.templateElement.setDynamicImagesEnabled(value);
                LOGGER.info("Dynamic images enabled: {}", value);
                if (value) {
                    showDynamicImageInstructions();
                }
            },
            "Enable Dynamic World Images");
        
        // Dynamic image source selector
        this.addGenericStringInputContextMenuEntryTo(templateMenu, "image_placeholder",
            element -> element instanceof WorldCardTemplateEditorElement,
            consumes -> ((WorldCardTemplateEditorElement)consumes).templateElement.getImagePlaceholder(),
            (element, placeholder) -> {
                ((WorldCardTemplateEditorElement)element).templateElement.setImagePlaceholder(placeholder);
                LOGGER.info("Image placeholder changed to: {}", placeholder);
            },
            null, false, false,
            Text.literal("Image Placeholder"),
            true, "{world_screenshot}",
            placeholder -> placeholder != null && placeholder.contains("{") && placeholder.contains("}"),
            null);
        
        // Add template configuration submenu
        this.rightClickMenu.addSubMenuEntry("template_config",
            Text.literal("Template Configuration"),
            templateMenu)
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Configure template container appearance and behavior")))
            .setIcon(ContextMenu.IconFactory.getIcon("edit"));
    }
    
    /**
     * Create template help and information submenu
     */
    private void createTemplateHelpMenu() {
        ContextMenu helpMenu = new ContextMenu();
        
        // Show available placeholders
        helpMenu.addClickableEntry("show_placeholders",
            Text.literal("Available Placeholders"),
            (menu, entry) -> {
                showPlaceholderHelp();
                menu.closeMenu();
            })
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("View all available world data placeholders")))
            .setIcon(ContextMenu.IconFactory.getIcon("info"));
        
        // Template usage instructions
        helpMenu.addClickableEntry("usage_instructions",
            Text.literal("Usage Instructions"),
            (menu, entry) -> {
                showUsageInstructions();
                menu.closeMenu();
            })
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Learn how to use the world card template system")))
            .setIcon(ContextMenu.IconFactory.getIcon("help"));
        
        // Add help submenu
        this.rightClickMenu.addSubMenuEntry("template_help",
            Text.literal("Template Help"),
            helpMenu)
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Get help with using world card templates")))
            .setIcon(ContextMenu.IconFactory.getIcon("help"));
    }
    
    /**
     * Show dynamic image setup instructions
     */
    private void showDynamicImageInstructions() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§6Dynamic World Images Setup:"), false);
            client.player.sendMessage(Text.literal("§71. Add an Image element to your layout"), false);
            client.player.sendMessage(Text.literal("§72. Anchor the image element to this template"), false);
            client.player.sendMessage(Text.literal("§73. Set image source to 'World Card' and choose type:"), false);
            client.player.sendMessage(Text.literal("   §e• World Screenshot §7- Auto-generated world preview"), false);
            client.player.sendMessage(Text.literal("   §e• World Icon §7- Custom world icon or screenshot"), false);
            client.player.sendMessage(Text.literal("   §e• Smart Selection §7- Best available image"), false);
            client.player.sendMessage(Text.literal("   §e• Game Mode Icon §7- Icon based on game mode"), false);
            client.player.sendMessage(Text.literal("   §e• Status Icon §7- Icon based on world status"), false);
            client.player.sendMessage(Text.literal("§74. Each world card will show its own image!"), false);
            client.player.sendMessage(Text.literal(""), false);
            client.player.sendMessage(Text.literal("§aAlternatively, use these placeholders in image source:"), false);
            client.player.sendMessage(Text.literal("§e{world_screenshot} {world_icon} {world_image}"), false);
        }
    }
    
    /**
     * Show comprehensive placeholder help to the user
     */
    private void showPlaceholderHelp() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§6=== WORLD CARD PLACEHOLDERS ==="), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Basic World Information
            client.player.sendMessage(Text.literal("§e🌍 BASIC WORLD INFO:"), false);
            client.player.sendMessage(Text.literal("§f{world_name} §7- World display name"), false);
            client.player.sendMessage(Text.literal("§f{folder_name} §7- World folder name"), false);
            client.player.sendMessage(Text.literal("§f{version} §7- Minecraft version"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Game Settings
            client.player.sendMessage(Text.literal("§e⚔ GAME SETTINGS:"), false);
            client.player.sendMessage(Text.literal("§f{game_mode} §7- Survival, Creative, etc."), false);
            client.player.sendMessage(Text.literal("§f{game_mode_display} §7- Formatted game mode"), false);
            client.player.sendMessage(Text.literal("§f{difficulty} §7- Easy, Normal, Hard"), false);
            client.player.sendMessage(Text.literal("§f{difficulty_display} §7- Formatted difficulty"), false);
            client.player.sendMessage(Text.literal("§f{hardcore} §7- Yes/No"), false);
            client.player.sendMessage(Text.literal("§f{cheats} §7- Enabled/Disabled"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Time & Dates
            client.player.sendMessage(Text.literal("§e🕐 TIME & DATES:"), false);
            client.player.sendMessage(Text.literal("§f{last_played} §7- Relative time (2h ago)"), false);
            client.player.sendMessage(Text.literal("§f{last_played_date} §7- Date only"), false);
            client.player.sendMessage(Text.literal("§f{last_played_time} §7- Time only"), false);
            client.player.sendMessage(Text.literal("§f{creation_time} §7- When world was created"), false);
            client.player.sendMessage(Text.literal("§f{world_age} §7- How old the world is"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // World Status & Size
            client.player.sendMessage(Text.literal("§e📊 STATUS & SIZE:"), false);
            client.player.sendMessage(Text.literal("§f{in_use} §7- Currently playing (Yes/No)"), false);
            client.player.sendMessage(Text.literal("§f{world_size_formatted} §7- World size (125.3 MB)"), false);
            client.player.sendMessage(Text.literal("§f{world_size_mb} §7- Size in MB"), false);
            client.player.sendMessage(Text.literal("§f{play_status} §7- Recently Played, etc."), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Weather & World Time
            client.player.sendMessage(Text.literal("§e🌤 WEATHER & TIME:"), false);
            client.player.sendMessage(Text.literal("§f{weather} §7- Clear, Rain, Thunderstorm"), false);
            client.player.sendMessage(Text.literal("§f{time_of_day} §7- In-game time (14:30)"), false);
            client.player.sendMessage(Text.literal("§f{raining} §7- Yes/No"), false);
            client.player.sendMessage(Text.literal("§f{thundering} §7- Yes/No"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Visual Symbols
            client.player.sendMessage(Text.literal("§e🎨 VISUAL SYMBOLS:"), false);
            client.player.sendMessage(Text.literal("§f{status_dot} §7- 🟢/🔴 (playing/not playing)"), false);
            client.player.sendMessage(Text.literal("§f{mode_symbol} §7- ⚔/🎨/🗺 (game mode icons)"), false);
            client.player.sendMessage(Text.literal("§f{difficulty_symbol} §7- 🕊/😊/😐/😰 (difficulty icons)"), false);
            client.player.sendMessage(Text.literal("§f{weather_symbol} §7- ☀/🌧/⛈ (weather icons)"), false);
            client.player.sendMessage(Text.literal("§f{hardcore_symbol} §7- 💀/❤ (hardcore indicator)"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Images
            client.player.sendMessage(Text.literal("§e🖼️ DYNAMIC IMAGES:"), false);
            client.player.sendMessage(Text.literal("§f{world_screenshot} §7- World's auto-generated screenshot"), false);
            client.player.sendMessage(Text.literal("§f{world_icon} §7- Custom world icon or screenshot"), false);
            client.player.sendMessage(Text.literal("§f{world_image} §7- Smart image selection"), false);
            client.player.sendMessage(Text.literal("§f{game_mode_icon} §7- Icon based on game mode"), false);
            client.player.sendMessage(Text.literal("§f{status_icon} §7- Icon based on world status"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            // Advanced
            client.player.sendMessage(Text.literal("§e🔧 ADVANCED:"), false);
            client.player.sendMessage(Text.literal("§f{seed} §7- World seed number"), false);
            client.player.sendMessage(Text.literal("§f{seed_short} §7- Shortened seed"), false);
            client.player.sendMessage(Text.literal("§f{world_type} §7- Survival, Hardcore, etc."), false);
            client.player.sendMessage(Text.literal("§f{days_since_played} §7- Days since last played"), false);
            client.player.sendMessage(Text.literal(""), false);
            
            client.player.sendMessage(Text.literal("§a✨ Use these in any text element anchored to this template!"), false);
            client.player.sendMessage(Text.literal("§7Example: §e{world_name} §7- §e{game_mode} §7(§e{last_played}§7)"), false);
        }
    }
    
    /**
     * Show usage instructions to the user
     */
    private void showUsageInstructions() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§6World Card Template Usage:"), false);
            client.player.sendMessage(Text.literal("§71. Add text, button, or image elements to your layout"), false);
            client.player.sendMessage(Text.literal("§72. Anchor those elements to this template container"), false);
            client.player.sendMessage(Text.literal("§73. Use placeholders like §e{world_name}§7 in text elements"), false);
            client.player.sendMessage(Text.literal("§74. For images, use 'World Card' source type or placeholders"), false);
            client.player.sendMessage(Text.literal("§75. Add a World Cards element to display your worlds"), false);
            client.player.sendMessage(Text.literal("§76. The template will automatically apply to all world cards!"), false);
        }
    }
    
    /**
     * Get tooltip text for the editor element
     */
    public List<Text> getTooltipText() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("§6World Card Template Container"));
        tooltip.add(Text.literal("§7Name: §f" + templateElement.getTemplateName()));
        tooltip.add(Text.literal("§7Size: §f" + templateElement.getTemplateWidth() + "x" + templateElement.getTemplateHeight()));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§eAnchor elements to this template"));
        tooltip.add(Text.literal("§eUse placeholders like {world_name} in text"));
        return tooltip;
    }
    
    /**
     * Get display name for the editor element
     */
    public Text getDisplayName() {
        return Text.literal("Template: " + templateElement.getTemplateName());
    }
}