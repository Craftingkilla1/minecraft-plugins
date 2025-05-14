package com.minecraft.core.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility class for inventory-related operations
 */
public class InventoryUtil {
    
    private InventoryUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Create an item stack with custom name and lore
     * @param material The material
     * @param name The display name
     * @param lore The lore lines
     * @return The created item stack
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            
            if (lore.length > 0) {
                List<String> loreList = Arrays.stream(lore)
                    .filter(line -> line != null && !line.isEmpty())
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
                
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create an item stack with custom name and lore
     * @param material The material
     * @param name The display name
     * @param lore The lore lines
     * @return The created item stack
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, lore.toArray(new String[0]));
    }
    
    /**
     * Create a player head item
     * @param player The player
     * @param name The display name
     * @param lore The lore lines
     * @return The created player head
     */
    public static ItemStack createPlayerHead(Player player, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            
            if (lore.length > 0) {
                List<String> loreList = Arrays.stream(lore)
                    .filter(line -> line != null && !line.isEmpty())
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
                
                meta.setLore(loreList);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a glowing item (with enchantment glint)
     * @param item The item to make glow
     * @return The glowing item
     */
    public static ItemStack makeGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a divider item for menus
     * @param color The glass pane color
     * @return The divider item
     */
    public static ItemStack createDivider(Material color) {
        return createItem(color, " ", "");
    }
    
    /**
     * Fill empty slots in an inventory with a specified item
     * @param inventory The inventory to fill
     * @param item The item to fill with
     */
    public static void fillEmptySlots(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
    }
    
    /**
     * Create a border around the edges of an inventory
     * @param inventory The inventory
     * @param size The inventory size
     * @param item The item to use for the border
     */
    public static void createBorder(Inventory inventory, int size, ItemStack item) {
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, item);
            inventory.setItem(size - 9 + i, item);
        }
        
        // Left and right columns
        for (int i = 9; i < size - 9; i += 9) {
            inventory.setItem(i, item);
            inventory.setItem(i + 8, item);
        }
    }
    
    /**
     * Update an item's display name
     * @param item The item to update
     * @param name The new display name
     * @return The updated item
     */
    public static ItemStack updateItemName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Update an item's lore
     * @param item The item to update
     * @param lore The new lore
     * @return The updated item
     */
    public static ItemStack updateItemLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            List<String> coloredLore = lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
            
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create a confirmation menu with yes and no options
     * @param title The menu title
     * @param yesAction What to do when Yes is clicked
     * @param noAction What to do when No is clicked
     * @return The confirmation menu
     */
    public static Inventory createConfirmationMenu(String title, Consumer<Player> yesAction, Consumer<Player> noAction) {
        Inventory inventory = org.bukkit.Bukkit.createInventory(null, 27, title);
        
        // Add yes button
        ItemStack yesButton = createItem(Material.LIME_WOOL, "&a&lYes", "&7Click to confirm");
        inventory.setItem(11, yesButton);
        
        // Add no button
        ItemStack noButton = createItem(Material.RED_WOOL, "&c&lNo", "&7Click to cancel");
        inventory.setItem(15, noButton);
        
        // Add dividers
        ItemStack divider = createDivider(Material.BLACK_STAINED_GLASS_PANE);
        fillEmptySlots(inventory, divider);
        
        return inventory;
    }
    
    /**
     * Check if an inventory is empty
     * @param inventory The inventory to check
     * @return true if the inventory is empty
     */
    public static boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }
}