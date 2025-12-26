package com.glacio.trap.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class TrapShopGUI {
    
    public static ItemStack createGuiItem(Material material, String name, List<String> lore, boolean enchanted) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            
            if (enchanted) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public static void addDecorativeBorder(Inventory inventory) {
        ItemStack glass = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§f", null, false);
        
        // Top row
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass);
        }
        // Bottom row  
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }
        // Sides
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, glass);
            inventory.setItem(i + 8, glass);
        }
    }
    
    public static void addBackItem(Inventory inventory, int slot) {
        List<String> lore = Arrays.asList("§7Ana menüye dön");
        ItemStack back = createGuiItem(Material.ARROW, "§e§lGeri", lore, false);
        inventory.setItem(slot, back);
    }
    
    public static void addCloseItem(Inventory inventory, int slot) {
        List<String> lore = Arrays.asList("§7Menüyü kapat");
        ItemStack close = createGuiItem(Material.BARRIER, "§c§lKapat", lore, false);
        inventory.setItem(slot, close);
    }
    
    public static Inventory createMainMenu(String title) {
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        addDecorativeBorder(inventory);
        
        // Ana menü butonları
        inventory.setItem(20, createGuiItem(Material.DIAMOND, "§a§lSatın Al", 
            Arrays.asList("§7Yeni tuzak satın al", "§7Kasaban için güçlü savunma!"), true));
            
        inventory.setItem(24, createGuiItem(Material.GOLD_INGOT, "§e§lSat", 
            Arrays.asList("§7Tuzaklarını sat", "§7Para kazan!"), true));
            
        inventory.setItem(30, createGuiItem(Material.CHEST, "§b§lTuzaklarım", 
            Arrays.asList("§7Kendi tuzaklarını gör", "§7Yükseltmeler yap!"), true));
            
        inventory.setItem(49, createGuiItem(Material.BARRIER, "§c§lKapat", 
            Arrays.asList("§7Menüyü kapat"), false));
        
        return inventory;
    }
    
    public static Inventory createBuyMenu(String title) {
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        addDecorativeBorder(inventory);
        addBackItem(inventory, 48);
        addCloseItem(inventory, 49);
        return inventory;
    }
    
    public static Inventory createMyTrapsMenu(String title) {
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        addDecorativeBorder(inventory);
        addBackItem(inventory, 49);
        return inventory;
    }
    
    public static Inventory createUpgradeMenu(String title, int trapId, int currentLevel, int nextLevel, double cost) {
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        List<String> lore = Arrays.asList(
            "§7Şu anki Seviye: §f" + currentLevel,
            "§7Sonraki Seviye: §a" + nextLevel,
            "",
            "§7Yükseltme Ücreti: §6" + cost + " TL",
            "",
            "§eYükseltmek için tıkla!"
        );

        inventory.setItem(13, createGuiItem(Material.NETHER_STAR, "§6§lSEVİYE ATLAT", lore, true));
        inventory.setItem(18, createGuiItem(Material.ARROW, "§7Geri Dön", null, false));
        
        return inventory;
    }
    
    public static void addTrapItem(Inventory inventory, int slot, int trapId, String owner, int level, int kills, double price, boolean isForSale) {
        Material material = isForSale ? Material.BEACON : Material.DISPENSER;
        String name = "§e§lTuzak #" + trapId;
        
        List<String> lore;
        if (isForSale) {
            lore = Arrays.asList(
                "§7ID: §f#" + trapId,
                "§7Fiyat: §a" + price + " TL",
                "",
                "§eSatın almak için tıkla!"
            );
        } else {
            lore = Arrays.asList(
                "§7ID: §f#" + trapId,
                "§7Seviye: §b" + level,
                "§7Toplam Öldürme: §f" + kills,
                "",
                "§eYükseltmek için tıkla!"
            );
        }
        
        ItemStack item = createGuiItem(material, name, lore, true);
        inventory.setItem(slot, item);
    }
    
    /**
     * Geri dön butonu ekler
     */
    public static void addBackItem(org.bukkit.inventory.Inventory inventory) {
        List<String> lore = List.of("§7Ana menüye dön");
        inventory.setItem(45, createGuiItem(org.bukkit.Material.ARROW, "§cGeri Dön", lore, false));
    }
    
    /**
     * Kapat butonu ekler
     */
    public static void addCloseItem(org.bukkit.inventory.Inventory inventory) {
        List<String> lore = List.of("§7Menüyü kapat");
        inventory.setItem(53, createGuiItem(org.bukkit.Material.BARRIER, "§cKapat", lore, false));
    }
}
