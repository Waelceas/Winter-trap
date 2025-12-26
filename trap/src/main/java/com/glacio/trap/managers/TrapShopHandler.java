package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class TrapShopHandler {
    private final TrapSystem plugin;
    private final TrapShopGUI gui;
    private final TrapShopLogic logic;
    private final Map<Player, TrapShopManager.ShopView> openMenus;
    
    public TrapShopHandler(TrapSystem plugin, Map<Player, TrapShopManager.ShopView> openMenus) {
        this.plugin = plugin;
        this.gui = new TrapShopGUI();
        this.logic = new TrapShopLogic(plugin);
        this.openMenus = openMenus;
    }
    
    public void handleClick(Player player, int slot, org.bukkit.event.inventory.ClickType clickType) {
        String title = player.getOpenInventory().getTitle();
        TrapShopManager.ShopView view = openMenus.get(player);
        
        // Eğer view yoksa veya başlık geçerli değilse işlemi iptal et
        if (view == null || !isValidTitleForView(title, view.getType())) {
            player.sendMessage("§cBir hata oluştu! Lütfen menüyü tekrar açın.");
            player.closeInventory();
            return;
        }
        
        // Slot -1 ise (legacy çağrılar için) işlemi atla
        if (slot == -1) return;
        
        // Tıklanan itemi al
        ItemStack item = player.getOpenInventory().getItem(slot);
        if (item == null || !item.hasItemMeta()) return;
        
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        // Debug mesajı
        player.sendMessage("§aTıklandı: " + itemName);
        
        // Menü butonlarını işle
        switch (itemName) {
            case "Satın Al":
                openBuyMenu(player);
                return;
            case "Tuzaklarım":
                openMyTrapsMenu(player);
                return;
            case "Geri Dön":
                openMainMenu(player);
                return;
            case "Kapat":
                player.closeInventory();
                return;
        }
        
        // Tuzak itemleri için özel kontrol
        if (itemName.startsWith("Tuzak #")) {
            int id = Integer.parseInt(itemName.replace("Tuzak #", ""));
            handleTrapItemClick(player, title, id);
        }
        
        // Sayfa navigasyonu
        if (TrapPagination.isValidNavigationSlot(slot)) {
            handlePageNavigation(player, slot, view);
        }
    }
    
    private void handleTrapItemClick(Player player, String title, int trapId) {
        if (title.equals("§6§lTuzak Satın Al")) {
            handlePurchaseAction(player, trapId);
        } else if (title.equals("§6§lTuzaklarım")) {
            Trap trap = plugin.getTrapManager().getTrapById(trapId);
            if (trap != null) {
                openUpgradeMenu(player, trap);
            }
        }
    }
    
    private void handlePurchaseAction(Player player, int trapId) {
        if (logic.processPurchase(player, trapId)) {
            player.closeInventory();
        }
    }
    
    private void handlePageNavigation(Player player, int slot, TrapShopManager.ShopView view) {
        TrapPagination.NavigationAction action = TrapPagination.getNavigationAction(slot);
        
        switch (action) {
            case PREVIOUS:
                if (view.getPage() > 0) {
                    view.setPage(view.getPage() - 1);
                    refreshCurrentMenu(player, view);
                }
                break;
            case NEXT:
                view.setPage(view.getPage() + 1);
                refreshCurrentMenu(player, view);
                break;
            case INFO:
            case NONE:
                break;
        }
    }
    
    private void refreshCurrentMenu(Player player, TrapShopManager.ShopView view) {
        switch (view.getType()) {
            case "buy":
                openBuyMenu(player);
                break;
            case "my_traps":
                openMyTrapsMenu(player);
                break;
            default:
                break;
        }
    }
    
    // Menü açma metotları
    private void openMainMenu(Player player) {
        TrapShopManager.ShopView view = new TrapShopManager.ShopView("main", 0);
        openMenus.put(player, view);
        player.openInventory(gui.createMainMenu("§6§lTuzak Dükkanı"));
    }
    
    private void openBuyMenu(Player player) {
        TrapShopManager.ShopView view = new TrapShopManager.ShopView("buy", 0);
        openMenus.put(player, view);
        
        var inventory = gui.createBuyMenu("§6§lTuzak Satın Al");
        
        // Satılık tuzakları ekle
        var forSale = plugin.getTrapManager().getAllTraps().stream()
                .filter(t -> t.getOwnerTownName() == null)
                .toList();

        int slot = 10;
        for (Trap trap : forSale) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
            if (slot > 43) break;

            double price = logic.getTrapPrice(trap.getId());
            gui.addTrapItem(inventory, slot, trap.getId(), "Yok", 1, 0, price, true);
            slot++;
        }
        
        player.openInventory(inventory);
    }
    
    private void openMyTrapsMenu(Player player) {
        TrapShopManager.ShopView view = new TrapShopManager.ShopView("my_traps", 0);
        openMenus.put(player, view);
        
        var inventory = gui.createMyTrapsMenu("§6§lTuzaklarım");
        
        String townName = logic.getPlayerTown(player);
        if (townName == null) {
            player.sendMessage("§cKasaban bulunamadı!");
            return;
        }
        
        var myTraps = plugin.getTrapManager().getAllTraps().stream()
                .filter(t -> townName.equals(t.getOwnerTownName()))
                .toList();

        int slot = 10;
        for (Trap trap : myTraps) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
            if (slot > 43) break;

            TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
            int level = stats != null ? stats.getLevel() : 1;
            int kills = stats != null ? stats.getTotalKills() : 0;
            
            gui.addTrapItem(inventory, slot, trap.getId(), townName, level, kills, 0, false);
            slot++;
        }
        
        player.openInventory(inventory);
    }
    
    private void openUpgradeMenu(Player player, Trap trap) {
        TrapShopManager.ShopView view = new TrapShopManager.ShopView("upgrade", 0);
        view.setData("trap_id", trap.getId());
        openMenus.put(player, view);

        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
        int currentLevel = stats.getLevel();
        int nextLevel = currentLevel + 1;
        double cost = logic.getUpgradeCost(currentLevel);
        
        var inventory = gui.createUpgradeMenu("§0Tuzak Geliştir: #" + trap.getId(), trap.getId(), currentLevel, nextLevel, cost);
        player.openInventory(inventory);
    }
    
    // Yardımcı metotlar
    private boolean isValidTitleForView(String title, String viewType) {
        switch (viewType) {
            case "main": return title.equals("§6§lTuzak Dükkanı");
            case "buy": return title.equals("§6§lTuzak Satın Al");
            case "my_traps": return title.equals("§6§lTuzaklarım");
            case "upgrade": return title.contains("Geliştir");
            default: return false;
        }
    }
}
