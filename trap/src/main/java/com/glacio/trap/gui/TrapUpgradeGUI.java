package com.glacio.trap.gui;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.handlers.TrapShopUpgradeHandler;
import com.glacio.trap.utils.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.List;

public class TrapUpgradeGUI {
    private final TrapSystem plugin;
    private final TrapShopUpgradeHandler upgradeHandler;

    // Handler'ı dışarıdan alıyoruz (Main sınıfında bir kez oluşturulmuş olanı)
    public TrapUpgradeGUI(TrapSystem plugin, TrapShopUpgradeHandler upgradeHandler) {
        this.plugin = plugin;
        this.upgradeHandler = upgradeHandler;
    }

    public void openUpgradeMenu(Player player, int trapId) {
        // Verileri kendimiz çekiyoruz
        var trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) {
            player.sendMessage("§cTuzak bulunamadı!");
            return;
        }

        // İstatistikleri çek
        var stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        int currentLevel = (stats != null) ? stats.getLevel() : 1;
        int nextLevel = currentLevel + 1;
        
        // Maliyeti hesapla
        double cost = upgradeHandler.calculateUpgradeCost(currentLevel);

        // Menüyü oluştur
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lYükseltme: #" + trapId);

        addLayout(inv);
        addUpgradeContent(inv, trapId, currentLevel, nextLevel, cost);

        player.openInventory(inv);
    }

    private void addUpgradeContent(Inventory inv, int id, int cur, int next, double cost) {
        // Ana Bilgi (Merkez)
        inv.setItem(22, GuiUtils.createItem(Material.DIAMOND_PICKAXE, "§6§lTuzak #" + id, 
            List.of("§7Mevcut: §a" + cur, "§7Hedef: §b" + next), true));

        // Yükseltme Butonu
        inv.setItem(31, GuiUtils.createItem(Material.ANVIL, "§e§lYÜKSELT", 
            List.of("§7Maliyet: §6" + cost + " TL", "", "§aTıkla ve Geliştir!"), false));

        // Kartlar
        inv.setItem(11, GuiUtils.createItem(Material.EXPERIENCE_BOTTLE, "§bSeviye", List.of("§7Max: §c10"), false));
        inv.setItem(15, GuiUtils.createItem(Material.GOLD_INGOT, "§eEkonomi", List.of("§7Fiyat: §6" + cost), false));
        
        // Bonus Hesabı (Kendi içindeki mantığı kullanabilirsin)
        double bonus = cur * 10.5;
        inv.setItem(24, GuiUtils.createItem(Material.ENCHANTED_BOOK, "§dBonuslar", 
            List.of("§7Bonus: §a%" + bonus), false));
    }

    private void addLayout(Inventory inv) {
        // Kenarlıklar (GuiUtils kullanarak)
        org.bukkit.inventory.ItemStack pane = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,45,46,47,48,49,50,51,52,53,9,18,27,36,17,26,35,44}) {
            inv.setItem(i, pane);
        }
        // Geri Dön
        inv.setItem(45, GuiUtils.createItem(Material.ARROW, "§cGeri Dön", List.of(), false));
    }
}
