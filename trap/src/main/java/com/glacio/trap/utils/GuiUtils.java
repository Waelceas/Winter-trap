package com.glacio.trap.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GuiUtils - GUI item oluşturma için merkezi yardımcı sınıf
 */
public class GuiUtils {
    
    /**
     * Ana GUI item oluşturma metodu - tüm projede bu kullanılacak
     */
    public static ItemStack createItem(Material material, String name, List<String> lore, boolean enchanted) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            
            if (enchanted) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Basit GUI item (enchanted olmadan)
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, lore, false);
    }
    
    /**
     * Dekoratif border item
     */
    public static ItemStack createBorderItem() {
        return createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }
    
    /**
     * Geri dön butonu
     */
    public static ItemStack createBackButton(String description) {
        return createItem(Material.ARROW, "§cGeri Dön", List.of("§7" + description));
    }
    
    /**
     * Kapat butonu
     */
    public static ItemStack createCloseButton() {
        return createItem(Material.BARRIER, "§cKapat", List.of("§7Menüyü kapatır."));
    }
}
