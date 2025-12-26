package com.glacio.trap.handlers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.gui.TrapSellGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * TrapShopCoordinator - Tüm handlerları birleştiren ana koordinatör
 */
public class TrapShopCoordinator {
    private final TrapSystem plugin;
    private final Map<Player, com.glacio.trap.managers.TrapShopManager.ShopView> openMenus;
    
    // Handlerlar
    private final TrapShopMainMenuHandler mainMenuHandler;
    private final TrapShopBuyHandler buyHandler;
    private final TrapShopSellHandler sellHandler;
    private final TrapShopUpgradeHandler upgradeHandler;
    
    // GUI'ler
    private final TrapSellGUI sellGUI;
    
    public TrapShopCoordinator(TrapSystem plugin, Map<Player, com.glacio.trap.managers.TrapShopManager.ShopView> openMenus) {
        this.plugin = plugin;
        this.openMenus = openMenus;
        
        // Handlerları başlat
        this.mainMenuHandler = new TrapShopMainMenuHandler(plugin, openMenus);
        this.buyHandler = new TrapShopBuyHandler(plugin);
        this.sellHandler = new TrapShopSellHandler(plugin);
        this.upgradeHandler = new TrapShopUpgradeHandler(plugin);
        
        // GUI'leri başlat (ManagerRegistry üzerinden)
        this.sellGUI = new TrapSellGUI(plugin, sellHandler);
    }
    
    /**
     * Tüm tıklama olaylarını yönetir
     */
    public void handleClick(Player player, int slot, org.bukkit.event.inventory.ClickType clickType) {
        String title = player.getOpenInventory().getTitle();
        com.glacio.trap.managers.TrapShopManager.ShopView view = openMenus.get(player);
        
        // View kontrolü
        if (view == null || !isValidTitleForView(title, view.getType())) {
            return; // Sessizce geç
        }
        
        // Slot -1 ise (legacy çağrılar için) işlemi atla
        if (slot == -1) return;
        
        // Sadece geçerli slotları işle (10-44 arası item slotları)
        if (slot < 10 || slot > 44) {
            // Kenar ve buton slotları için özel kontrol
            if (slot == 45 || slot == 53) {
                handlePageNavigation(player, slot, view);
                return;
            }
            return; // Diğer kenar slotlarını görmezden gel
        }
        
        // Tıklanan itemi al
        ItemStack item = player.getOpenInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        // Menü tipine göre yönlendir
        switch (view.getType()) {
            case "main":
                handleMainMenuClick(player, itemName);
                break;
            case "buy":
                handleBuyMenuClick(player, itemName, slot);
                break;
            case "sell":
                handleSellMenuClick(player, itemName, slot);
                break;
            case "my_traps":
                handleMyTrapsMenuClick(player, itemName, slot);
                break;
            case "upgrade":
                handleUpgradeMenuClick(player, itemName, slot);
                break;
        }
    }
    
    /**
     * Ana menü tıklamalarını yönetir
     */
    private void handleMainMenuClick(Player player, String itemName) {
        if (mainMenuHandler.handleMainMenuClick(player, itemName)) {
            return; // İşlem başarılı
        }
        
        // Geri dön butonu
        if (itemName.equals("Geri Dön")) {
            mainMenuHandler.openMainMenu(player);
        }
    }
    
    /**
     * Satın alma menüsü tıklamalarını yönetir
     */
    private void handleBuyMenuClick(Player player, String itemName, int slot) {
        player.sendMessage("§e[DEBUG] Buy menu click: " + itemName + " at slot " + slot);
        
        // Geri dön butonu
        if (itemName.equals("Geri Dön")) {
            mainMenuHandler.openMainMenu(player);
            return;
        }
        
        // Tuzak tıklaması
        if (itemName.startsWith("Tuzak #")) {
            int trapId = Integer.parseInt(itemName.replace("Tuzak #", ""));
            player.sendMessage("§e[DEBUG] Attempting to buy trap #" + trapId);
            boolean success = buyHandler.handleBuyTrap(player, trapId);
            player.sendMessage("§e[DEBUG] Buy result: " + success);
        }
    }
    
    /**
     * Satma menüsü tıklamalarını yönetir
     */
    private void handleSellMenuClick(Player player, String itemName, int slot) {
        // Geri dön butonu
        if (itemName.equals("Geri Dön")) {
            mainMenuHandler.openMainMenu(player);
            return;
        }
        
        // Tuzak tıklaması
        if (itemName.startsWith("Tuzak #")) {
            int trapId = Integer.parseInt(itemName.replace("Tuzak #", ""));
            // Merkezi İşlemci'yi kullan
            plugin.getManagerRegistry().getActionHandler().handleSell(player, trapId);
        }
    }
    
    /**
     * Tuzaklarım menüsü tıklamalarını yönetir
     */
    private void handleMyTrapsMenuClick(Player player, String itemName, int slot) {
        // Geri dön butonu
        if (itemName.equals("Geri Dön")) {
            mainMenuHandler.openMainMenu(player);
            return;
        }
        
        // Tuzak tıklaması - yükseltme menüsüne git
        if (itemName.startsWith("Tuzak #")) {
            int trapId = Integer.parseInt(itemName.replace("Tuzak #", ""));
            mainMenuHandler.openUpgradeMenu(player, trapId);
        }
    }
    
    /**
     * Yükseltme menüsü tıklamalarını yönetir
     */
    private void handleUpgradeMenuClick(Player player, String itemName, int slot) {
        // Geri dön butonu
        if (itemName.equals("Geri Dön")) {
            mainMenuHandler.openMyTrapsMenu(player);
            return;
        }
        
        // Yükseltme butonu
        if (itemName.equals("YÜKSELT")) {
            com.glacio.trap.managers.TrapShopManager.ShopView view = openMenus.get(player);
            Integer trapId = (Integer) view.getData("trap_id");
            if (trapId != null) {
                // Merkezi İşlemci'yi kullan
                plugin.getManagerRegistry().getActionHandler().handleUpgrade(player, trapId);
            }
        }
    }
    
    /**
     * Sayfa navigasyonunu yönetir
     */
    private void handlePageNavigation(Player player, int slot, com.glacio.trap.managers.TrapShopManager.ShopView view) {
        // Basit navigasyon - geçici olarak devre dışı
        if (slot == 45) { // Geri butonu
            mainMenuHandler.openMainMenu(player);
        }
    }
    
    /**
     * Mevcut menüyü yeniler
     */
    private void refreshCurrentMenu(Player player, com.glacio.trap.managers.TrapShopManager.ShopView view) {
        switch (view.getType()) {
            case "buy":
                mainMenuHandler.openBuyMenu(player);
                break;
            case "sell":
                mainMenuHandler.openSellMenu(player);
                break;
            case "my_traps":
                mainMenuHandler.openMyTrapsMenu(player);
                break;
            default:
                break;
        }
    }
    
    /**
     * Ana menüyü açar
     */
    public void openMainMenu(Player player) {
        mainMenuHandler.openMainMenu(player);
    }
    
    /**
     * Menü başlığını doğrular
     */
    private boolean isValidTitleForView(String title, String viewType) {
        switch (viewType) {
            case "main": return title.equals("§6§lTuzak Dükkanı");
            case "buy": return title.equals("§6§lTuzak Satın Al");
            case "sell": return title.equals("§6§lTuzak Sat");
            case "my_traps": return title.equals("§6§lTuzaklarım");
            case "upgrade": return title.contains("Geliştir");
            default: return false;
        }
    }
}
