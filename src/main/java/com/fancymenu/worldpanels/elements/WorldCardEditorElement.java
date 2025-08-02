package com.fancymenu.worldpanels.elements;

// REMOVED: import com.fancymenu.worldpanels.template.WorldCardTemplateDesigner;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor interface for Dynamic World Card Elements - FancyMenu v3 Compatible.
 * Properly integrates with FancyMenu's ContextMenu system by overriding the init() method.
 */
@Environment(EnvType.CLIENT)
public class WorldCardEditorElement extends AbstractEditorElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCardEditorElement.class);
    
    private final WorldCardElement worldCardElement;
    private final WorldCardElementBuilder builder;
    
    public WorldCardEditorElement(WorldCardElement element, WorldCardElementBuilder builder, LayoutEditorScreen screen) {
        super(element, screen);
        this.worldCardElement = element;
        this.builder = builder;
    }
    
    /**
     * Override the init() method to add our custom context menu entries.
     * This is where FancyMenu builds the right-click context menu.
     */
    @Override
    public void init() {
        // Call the parent init() first to get all the standard menu items
        super.init();
        
        // Add a separator before our custom options
        this.rightClickMenu.addSeparatorEntry("world_cards_separator");
        
        // REMOVED TEMPLATE EDITOR - Now using anchor-based system
        
        // Create our custom context menu sections
        createLayoutConfigurationMenu();
        createAppearanceConfigurationMenu();
        createInformationDisplayMenu();
        createButtonConfigurationMenu();
        
        // Add refresh action
        this.rightClickMenu.addClickableEntry("refresh_world_data", 
            Text.literal("Refresh World Data"), 
            (menu, entry) -> {
                refreshWorldData();
                menu.closeMenu();
            })
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Refresh world data from disk")))
            .setIcon(ContextMenu.IconFactory.getIcon("refresh"));
    }
    
    /**
     * Create the layout configuration submenu
     */
    private void createLayoutConfigurationMenu() {
        ContextMenu layoutMenu = new ContextMenu();
        
        // Auto layout toggle
        this.addToggleContextMenuEntryTo(layoutMenu, "auto_layout", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isAutoLayout(),
            (element, value) -> {
                element.worldCardElement.setAutoLayout(value);
                LOGGER.info("Auto layout: {}", value);
            },
            "Auto Layout");
        
        layoutMenu.addSeparatorEntry("layout_separator");
        
        // Cards per row options
        for (int i = 1; i <= 6; i++) {
            final int cardsPerRow = i;
            String entryId = "cards_per_row_" + i;
            
            layoutMenu.addClickableEntry(entryId, 
                Text.literal(cardsPerRow + " Cards Per Row"),
                (menu, entry) -> {
                    this.editor.history.saveSnapshot();
                    worldCardElement.setCardsPerRow(cardsPerRow);
                    worldCardElement.setAutoLayout(false);
                    LOGGER.info("Cards per row: {}", cardsPerRow);
                    menu.closeMenu();
                });
        }
        
        // Card size configuration
        layoutMenu.addSeparatorEntry("size_separator");
        
        this.addGenericIntegerInputContextMenuEntryTo(layoutMenu, "card_width",
            element -> element instanceof WorldCardEditorElement,
            consumes -> ((WorldCardEditorElement)consumes).worldCardElement.getCardWidth(),
            (element, value) -> {
                ((WorldCardEditorElement)element).worldCardElement.setCardSize(value, ((WorldCardEditorElement)element).worldCardElement.getCardHeight());
                LOGGER.info("Card width: {}", value);
            },
            Text.literal("Card Width"),
            true, 200, null, null);
            
        this.addGenericIntegerInputContextMenuEntryTo(layoutMenu, "card_height",
            element -> element instanceof WorldCardEditorElement,
            consumes -> ((WorldCardEditorElement)consumes).worldCardElement.getCardHeight(),
            (element, value) -> {
                ((WorldCardEditorElement)element).worldCardElement.setCardSize(((WorldCardEditorElement)element).worldCardElement.getCardWidth(), value);
                LOGGER.info("Card height: {}", value);
            },
            Text.literal("Card Height"),
            true, 100, null, null);
            
        this.addGenericIntegerInputContextMenuEntryTo(layoutMenu, "card_spacing",
            element -> element instanceof WorldCardEditorElement,
            consumes -> ((WorldCardEditorElement)consumes).worldCardElement.getCardSpacing(),
            (element, value) -> {
                ((WorldCardEditorElement)element).worldCardElement.setCardSpacing(value);
                LOGGER.info("Card spacing: {}", value);
            },
            Text.literal("Card Spacing"),
            true, 10, null, null);
        
        // Add the layout submenu to the main context menu
        this.rightClickMenu.addSubMenuEntry("card_layout", 
            Text.literal("Card Layout"), 
            layoutMenu)
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Configure card layout and spacing")))
            .setIcon(ContextMenu.IconFactory.getIcon("move"));
    }
    
    /**
     * Create the appearance configuration submenu
     */
    private void createAppearanceConfigurationMenu() {
        ContextMenu appearanceMenu = new ContextMenu();
        
        // Theme selection
        String[] themes = {"Default", "Dark", "Light", "Blue", "Green"};
        for (String theme : themes) {
            String entryId = "theme_" + theme.toLowerCase();
            
            appearanceMenu.addClickableEntry(entryId,
                Text.literal(theme + " Theme"),
                (menu, entry) -> {
                    this.editor.history.saveSnapshot();
                    applyTheme(theme);
                    menu.closeMenu();
                });
        }
        
        appearanceMenu.addSeparatorEntry("color_separator");
        
        // Color customization using FancyMenu's string input for hex colors
        this.addGenericStringInputContextMenuEntryTo(appearanceMenu, "background_color",
            element -> element instanceof WorldCardEditorElement,
            consumes -> String.format("#%06X", ((WorldCardEditorElement)consumes).worldCardElement.getBackgroundColor() & 0xFFFFFF),
            (element, hexColor) -> {
                try {
                    int color = Integer.parseInt(hexColor.replace("#", ""), 16) | 0x88000000;
                    ((WorldCardEditorElement)element).worldCardElement.setBackgroundColor(color);
                    LOGGER.info("Background color: {}", hexColor);
                } catch (Exception e) {
                    LOGGER.warn("Invalid color format: {}", hexColor);
                }
            },
            null, false, false,
            Text.literal("Background Color"),
            true, "#222222", 
            consumes -> consumes.matches("#[0-9A-Fa-f]{6}"),
            null);
            
        this.addGenericStringInputContextMenuEntryTo(appearanceMenu, "text_color",
            element -> element instanceof WorldCardEditorElement,
            consumes -> String.format("#%06X", ((WorldCardEditorElement)consumes).worldCardElement.getTextColor() & 0xFFFFFF),
            (element, hexColor) -> {
                try {
                    int color = Integer.parseInt(hexColor.replace("#", ""), 16) | 0xFF000000;
                    ((WorldCardEditorElement)element).worldCardElement.setTextColor(color);
                    LOGGER.info("Text color: {}", hexColor);
                } catch (Exception e) {
                    LOGGER.warn("Invalid color format: {}", hexColor);
                }
            },
            null, false, false,
            Text.literal("Text Color"),
            true, "#FFFFFF",
            consumes -> consumes.matches("#[0-9A-Fa-f]{6}"),
            null);
        
        // Add the appearance submenu to the main context menu
        this.rightClickMenu.addSubMenuEntry("card_appearance",
            Text.literal("Card Appearance"),
            appearanceMenu)
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Configure card colors and themes")))
            .setIcon(ContextMenu.IconFactory.getIcon("color"));
    }
    
    /**
     * Create the information display submenu
     */
    private void createInformationDisplayMenu() {
        ContextMenu infoMenu = new ContextMenu();
        
        // Information display toggles
        this.addToggleContextMenuEntryTo(infoMenu, "show_last_played", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowLastPlayed(),
            (element, value) -> {
                element.worldCardElement.setShowLastPlayed(value);
                LOGGER.info("Show last played: {}", value);
            },
            "Show Last Played");
            
        this.addToggleContextMenuEntryTo(infoMenu, "show_world_size", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowWorldSize(),
            (element, value) -> {
                element.worldCardElement.setShowWorldSize(value);
                LOGGER.info("Show world size: {}", value);
            },
            "Show World Size");
            
        this.addToggleContextMenuEntryTo(infoMenu, "show_game_mode", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowGameMode(),
            (element, value) -> {
                element.worldCardElement.setShowGameMode(value);
                LOGGER.info("Show game mode: {}", value);
            },
            "Show Game Mode");
            
        this.addToggleContextMenuEntryTo(infoMenu, "show_difficulty", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowDifficulty(),
            (element, value) -> {
                element.worldCardElement.setShowDifficulty(value);
                LOGGER.info("Show difficulty: {}", value);
            },
            "Show Difficulty");
            
        this.addToggleContextMenuEntryTo(infoMenu, "show_version", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowVersion(),
            (element, value) -> {
                element.worldCardElement.setShowVersion(value);
                LOGGER.info("Show version: {}", value);
            },
            "Show Version");
        
        // Add the information submenu to the main context menu
        this.rightClickMenu.addSubMenuEntry("information_display",
            Text.literal("Information Display"),
            infoMenu)
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Configure what world information to display")))
            .setIcon(ContextMenu.IconFactory.getIcon("info"));
    }
    
    /**
     * Create the button configuration submenu
     */
    private void createButtonConfigurationMenu() {
        ContextMenu buttonMenu = new ContextMenu();
        
        // Enable/disable buttons
        this.addToggleContextMenuEntryTo(buttonMenu, "show_buttons", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowButtons(),
            (element, value) -> {
                element.worldCardElement.setShowButtons(value);
                LOGGER.info("Show buttons: {}", value);
            },
            "Show Buttons");
            
        buttonMenu.addSeparatorEntry("button_types_separator");
        
        // Individual button toggles
        this.addToggleContextMenuEntryTo(buttonMenu, "show_play_button", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowPlayButton(),
            (element, value) -> {
                element.worldCardElement.setShowPlayButton(value);
                LOGGER.info("Show play button: {}", value);
            },
            "Show Play Button");
            
        this.addToggleContextMenuEntryTo(buttonMenu, "show_edit_button", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowEditButton(),
            (element, value) -> {
                element.worldCardElement.setShowEditButton(value);
                LOGGER.info("Show edit button: {}", value);
            },
            "Show Edit Button");
            
        this.addToggleContextMenuEntryTo(buttonMenu, "show_delete_button", WorldCardEditorElement.class,
            consumes -> consumes.worldCardElement.isShowDeleteButton(),
            (element, value) -> {
                element.worldCardElement.setShowDeleteButton(value);
                LOGGER.info("Show delete button: {}", value);
            },
            "Show Delete Button");
        
        // Add the button submenu to the main context menu
        this.rightClickMenu.addSubMenuEntry("button_configuration",
            Text.literal("Button Configuration"),
            buttonMenu)
            .setTooltipSupplier((menu, entry) -> de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip.of(
                de.keksuccino.fancymenu.util.LocalizationUtils.splitLocalizedLines("Configure world card buttons")))
            .setIcon(ContextMenu.IconFactory.getIcon("button"));
    }
    
    /**
     * Apply a theme by name
     */
    private void applyTheme(String themeName) {
        this.editor.history.saveSnapshot();
        
        switch (themeName) {
            case "Dark":
                worldCardElement.setBackgroundColor(0x88333333);
                worldCardElement.setHoverColor(0x88555555);
                worldCardElement.setBorderColor(0xFF888888);
                worldCardElement.setTextColor(0xFFFFFFFF);
                break;
            case "Light":
                worldCardElement.setBackgroundColor(0x88EEEEEE);
                worldCardElement.setHoverColor(0x88DDDDDD);
                worldCardElement.setBorderColor(0xFF666666);
                worldCardElement.setTextColor(0xFF000000);
                break;
            case "Blue":
                worldCardElement.setBackgroundColor(0x88334455);
                worldCardElement.setHoverColor(0x88445566);
                worldCardElement.setBorderColor(0xFF6699CC);
                worldCardElement.setTextColor(0xFFFFFFFF);
                break;
            case "Green":
                worldCardElement.setBackgroundColor(0x88334433);
                worldCardElement.setHoverColor(0x88445544);
                worldCardElement.setBorderColor(0xFF66CC66);
                worldCardElement.setTextColor(0xFFFFFFFF);
                break;
            default: // Default
                worldCardElement.setBackgroundColor(0x88222222);
                worldCardElement.setHoverColor(0x88444444);
                worldCardElement.setBorderColor(0xFF666666);
                worldCardElement.setTextColor(0xFFFFFFFF);
                break;
        }
        LOGGER.info("Applied theme: {}", themeName);
    }
    
    /**
     * Refresh world data
     */
    private void refreshWorldData() {
        try {
            this.editor.history.saveSnapshot();
            com.fancymenu.worldpanels.managers.WorldDataManager.getInstance().refreshWorlds();
            
            // Also trigger export
            if (com.fancymenu.worldpanels.exporters.WorldDataExporter.getInstance().isInitialized()) {
                com.fancymenu.worldpanels.exporters.WorldDataExporter.getInstance().forceExport();
            }
            
            LOGGER.info("World data refreshed - found {} worlds", worldCardElement.getWorldCount());
        } catch (Exception e) {
            LOGGER.warn("Failed to refresh world data", e);
        }
    }
    
    /**
     * Get tooltip text for the editor element
     */
    public List<Text> getTooltipText() {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("§6Dynamic World Cards"));
        tooltip.add(Text.literal("§7Worlds: §f" + worldCardElement.getWorldCount()));
        tooltip.add(Text.literal("§7Layout: §f" + (worldCardElement.isAutoLayout() ? "Auto" : worldCardElement.getCardsPerRow() + " per row")));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("§eRight-click for configuration options"));
        return tooltip;
    }
    
    /**
     * Get display name for the editor element
     */
    public Text getDisplayName() {
        return Text.literal("World Cards (" + worldCardElement.getWorldCount() + " worlds)");
    }
}