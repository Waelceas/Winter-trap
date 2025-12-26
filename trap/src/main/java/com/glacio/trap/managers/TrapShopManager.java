package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.handlers.TrapShopCoordinator;
import com.glacio.trap.models.TrapStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * TrapShopManager v4 - Ultra Modular Architecture
 * Sadece koordinasyon yapar, her şeyi özel handlerlara devreder
 */
public class TrapShopManager {
    private final TrapSystem plugin;
    private FileConfiguration shopConfig;
    private final Map<Player, ShopView> openMenus = new HashMap<>();
    
    // Ana koordinatör
    private final TrapShopCoordinator coordinator;
    
    // GUI Titles
    private final String MAIN_MENU_TITLE = "§6§lTuzak Dükkanı";
    private final String BUY_MENU_TITLE = "§6§lTuzak Satın Al";
    private final String MY_TRAPS_TITLE = "§6§lTuzaklarım";
    
    /**
     * Shop view state - Menü durumunu tutar
     */
    public static class ShopView {
        private final String type;
        private int page;
        private final Map<String, Object> data;
        
        public ShopView(String type, int page) {
            this.type = type;
            this.page = page;
            this.data = new HashMap<>();
        }
        
        public String getType() { return type; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public Map<String, Object> getData() { return data; }
        public Object getData(String key) { return data.get(key); }
        public void setData(String key, Object value) { data.put(key, value); }
    }

    public TrapShopManager(TrapSystem plugin) {
        this.plugin = plugin;
        this.coordinator = new TrapShopCoordinator(plugin, openMenus);
        loadShopConfig();
        plugin.getLogger().info("TrapShopManager v4 (Ultra Modular) initialized!");
    }
    
    private void loadShopConfig() {
        File configFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!configFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
        shopConfig = plugin.getConfig();
    }
    
    /**
     * Ana menüyü açar
     */
    public void openShopMenu(Player player) {
        coordinator.openMainMenu(player);
    }
    
    /**
     * Tıklama olaylarını coordinator'a devreder
     */
    public void handleClick(Player player, int slot, org.bukkit.event.inventory.ClickType clickType) {
        coordinator.handleClick(player, slot, clickType);
    }
    
    /**
     * Menü durumunu kapatır
     */
    public void closeShopMenu(Player player) {
        openMenus.remove(player);
    }
    
    /**
     * Shop view durumunu döndürür
     */
    public ShopView getShopView(Player player) {
        return openMenus.get(player);
    }
    
    /**
     * Menü başlığını döndürür
     */
    public String getMenuTitle() {
        return MAIN_MENU_TITLE;
    }
    
    /**
     * Konfigürasyonu yeniler
     */
    public void reloadShopConfig() {
        loadShopConfig();
    }
    
    // Legacy metotlar - uyumluluk için
    public void handleTrapPurchase(Player player, int trapId, TrapStats stats) {
        // Coordinator'a devreder
        coordinator.handleClick(player, -1, org.bukkit.event.inventory.ClickType.LEFT);
    }
    
    public void handleTrapUpgrade(Player player, int trapId, TrapStats stats) {
        // Coordinator'a devreder
        coordinator.handleClick(player, -1, org.bukkit.event.inventory.ClickType.LEFT);
    }
    
    public void onInventoryClose(Player player) {
        closeShopMenu(player);
    }
}
