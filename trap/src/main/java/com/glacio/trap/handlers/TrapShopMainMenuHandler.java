package com.glacio.trap.handlers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.gui.TrapMainMenuGUI;
import com.glacio.trap.gui.TrapBuyGUI;
import com.glacio.trap.gui.TrapSellGUI;
import com.glacio.trap.gui.TrapUpgradeGUI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

/**
 * TrapShopMainMenuHandler - Sadece ana menü navigasyonu
 */
public class TrapShopMainMenuHandler {
    private final TrapSystem plugin;
    private final Map<Player, com.glacio.trap.managers.TrapShopManager.ShopView> openMenus;
    
    // GUI sınıfları
    private final TrapMainMenuGUI mainMenuGUI;
    private final TrapBuyGUI buyGUI;
    private final TrapSellGUI sellGUI;
    private final TrapUpgradeGUI upgradeGUI;
    
    public TrapShopMainMenuHandler(TrapSystem plugin, Map<Player, com.glacio.trap.managers.TrapShopManager.ShopView> openMenus) {
        this.plugin = plugin;
        this.openMenus = openMenus;
        
        // GUI sınıflarını başlat
        this.mainMenuGUI = new TrapMainMenuGUI(plugin);
        this.buyGUI = new TrapBuyGUI(plugin);
        this.sellGUI = new TrapSellGUI(plugin, new TrapShopSellHandler(plugin));
        this.upgradeGUI = new TrapUpgradeGUI(plugin, new TrapShopUpgradeHandler(plugin));
    }
    
    /**
     * Ana menü tıklamalarını yönetir
     */
    public boolean handleMainMenuClick(Player player, String itemName) {
        switch (itemName) {
            case "Satın Al":
                playClickSound(player);
                openBuyMenu(player);
                return true;
                
            case "Sat":
                playClickSound(player);
                openSellMenu(player);
                return true;
                
            case "Tuzaklarım":
                playClickSound(player);
                openMyTrapsMenu(player);
                return true;
                
            case "Kapat":
                player.closeInventory();
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Ana menüyü açar
     */
    public void openMainMenu(Player player) {
        com.glacio.trap.managers.TrapShopManager.ShopView view = new com.glacio.trap.managers.TrapShopManager.ShopView("main", 0);
        openMenus.put(player, view);
        
        // Yeni TrapMainMenuGUI yapısını kullan
        mainMenuGUI.openMainMenu(player);
    }
    
    /**
     * Satın alma menüsünü açar
     */
    public void openBuyMenu(Player player) {
        com.glacio.trap.managers.TrapShopManager.ShopView view = new com.glacio.trap.managers.TrapShopManager.ShopView("buy", 0);
        openMenus.put(player, view);
        
        Inventory inventory = buyGUI.createBuyMenu("§6§lTuzak Satın Al");
        
        // Satın alınabilir tuzakları ekle
        buyGUI.addBuyableTraps(inventory);
        
        player.openInventory(inventory);
    }
    
    /**
     * Satma menüsünü açar
     */
    public void openSellMenu(Player player) {
        com.glacio.trap.managers.TrapShopManager.ShopView view = new com.glacio.trap.managers.TrapShopManager.ShopView("sell", 0);
        openMenus.put(player, view);
        
        // Eski metodu kullan ama modern içerikle
        Inventory inventory = sellGUI.createSellMenu("§6§lTuzak Sat");
        
        // Satılabilir tuzakları ekle
        sellGUI.addSellableTraps(inventory, player);
        
        // Geri dön ve kapat butonları
        sellGUI.addBackItem(inventory);
        sellGUI.addCloseItem(inventory);
        
        player.openInventory(inventory);
    }
    
    /**
     * Tuzaklarım menüsünü açar
     */
    public void openMyTrapsMenu(Player player) {
        com.glacio.trap.managers.TrapShopManager.ShopView view = new com.glacio.trap.managers.TrapShopManager.ShopView("my_traps", 0);
        openMenus.put(player, view);
        
        Inventory inventory = org.bukkit.Bukkit.createInventory(null, 54, "§6§lTuzaklarım");
        
        // Dekoratif kenar
        addDecorativeBorder(inventory);
        
        // Geri dön ve kapat butonları
        addBackItem(inventory);
        addCloseItem(inventory);
        
        String townName = getPlayerTown(player);
        if (townName == null) {
            player.sendMessage("§cKasaban bulunamadı!");
            player.closeInventory();
            return;
        }
        
        var myTraps = plugin.getTrapManager().getAllTraps().stream()
                .filter(t -> townName.equals(t.getOwnerTownName()))
                .toList();

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        int slotIndex = 0;
        
        for (var trap : myTraps) {
            if (slotIndex >= slots.length) break;

            var stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
            int level = stats != null ? stats.getLevel() : 1;
            int kills = stats != null ? stats.getTotalKills() : 0;
            
            // Tuzak item'i oluştur
            String displayName = "§6§lTuzak #" + trap.getId();
            java.util.List<String> lore = java.util.List.of(
                "§7Seviye: §a" + level,
                "§7Öldürme: §c" + kills,
                "§7Kasaba: §e" + townName,
                "",
                "§eTıkla yükselt!"
            );
            
            inventory.setItem(slots[slotIndex], com.glacio.trap.utils.GuiUtils.createItem(
                org.bukkit.Material.DIAMOND_PICKAXE, displayName, lore, false));
            slotIndex++;
        }
        
        // Eğer tuzak yoksa mesaj göster
        if (myTraps.isEmpty()) {
            inventory.setItem(22, com.glacio.trap.utils.GuiUtils.createItem(
                org.bukkit.Material.BARRIER, "§c§lTuzak Yok", 
                java.util.List.of("§7Henüz tuzak satın almadın!", "§7Tuzak Dükkanı'ndan satın alabilirsin."), false));
        }
        
        player.openInventory(inventory);
    }
    
    /**
     * Yükseltme menüsünü açar
     */
    public void openUpgradeMenu(Player player, int trapId) {
        com.glacio.trap.managers.TrapShopManager.ShopView view = new com.glacio.trap.managers.TrapShopManager.ShopView("upgrade", 0);
        view.setData("trap_id", trapId);
        openMenus.put(player, view);

        var trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) {
            player.sendMessage("§cTuzak bulunamadı!");
            return;
        }
        
        var stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        if (stats == null) return;
        
        int currentLevel = stats.getLevel();
        int nextLevel = currentLevel + 1;
        
        TrapShopUpgradeHandler upgradeHandler = new TrapShopUpgradeHandler(plugin);
        double cost = upgradeHandler.calculateUpgradeCost(currentLevel);
        
        // Yeni TrapUpgradeGUI yapısını kullan
        upgradeGUI.openUpgradeMenu(player, trapId);
    }
    
    /**
     * Menü başlığını kontrol eder
     */
    public boolean isMainMenu(String title) {
        return title.equals("§6§lTuzak Dükkanı");
    }
    
    /**
     * Oyuncunun kasabasını alır
     */
    private String getPlayerTown(Player player) {
        com.palmergames.bukkit.towny.TownyAPI townyAPI = com.palmergames.bukkit.towny.TownyAPI.getInstance();
        com.palmergames.bukkit.towny.object.Resident resident = townyAPI.getResident(player);
        if (resident != null && resident.hasTown()) {
            com.palmergames.bukkit.towny.object.Town town = resident.getTownOrNull();
            return town != null ? town.getName() : null;
        }
        return null;
    }
    
    /**
     * Tıklama sesini çalar
     */
    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Dekoratif kenar ekler
     */
    private void addDecorativeBorder(Inventory inventory) {
        org.bukkit.inventory.ItemStack border = com.glacio.trap.utils.GuiUtils.createItem(
            org.bukkit.Material.GRAY_STAINED_GLASS_PANE, " ", java.util.List.of(), false);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Geri dön butonu ekler
     */
    private void addBackItem(Inventory inventory) {
        inventory.setItem(45, com.glacio.trap.utils.GuiUtils.createItem(
            org.bukkit.Material.ARROW, "§cGeri Dön", java.util.List.of("§7Ana menüye dön"), false));
    }
    
    /**
     * Kapat butonu ekler
     */
    private void addCloseItem(Inventory inventory) {
        inventory.setItem(53, com.glacio.trap.utils.GuiUtils.createItem(
            org.bukkit.Material.BARRIER, "§cKapat", java.util.List.of("§7Menüyü kapat"), false));
    }
}
