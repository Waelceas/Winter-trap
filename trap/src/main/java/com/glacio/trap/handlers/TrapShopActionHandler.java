package com.glacio.trap.handlers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class TrapShopActionHandler {
    private final TrapSystem plugin;

    public TrapShopActionHandler(TrapSystem plugin) {
        this.plugin = plugin;
    }

    // SATMA İŞLEMİ
    public void handleSell(Player player, int trapId) {
        // ManagerRegistry üzerinden SellHandler'ı çağırıyoruz
        boolean success = plugin.getManagerRegistry().getSellHandler().handleSellTrap(player, trapId);
        if (success) {
            player.closeInventory();
            // Başarılı ses efekti
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage("§a§lBAŞARILI! §7Tuzak satıldı.");
            // Satma menüsünü yeniden aç
            plugin.getManagerRegistry().getMainMenuHandler().openSellMenu(player);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§c§lHATA! §7Tuzak satılamadı.");
        }
    }

    // SATIN ALMA İŞLEMİ
    public void handleBuy(Player player, String trapType) {
        double price = 10000.0; // Bunu config'den çekebilirsin
        if (plugin.getEconomy().getBalance(player) >= price) {
            plugin.getEconomy().withdrawPlayer(player, price);
            // Geçici olarak sadece mesaj gönder
            player.sendMessage("§a§lBAŞARILI! §7Tuzak satın alındı. (Trap ID: " + trapType + ")");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§c§lHATA! §7Yetersiz bakiye.");
        }
    }

    // YÜKSELTME İŞLEMİ
    public void handleUpgrade(Player player, int trapId) {
        boolean success = plugin.getManagerRegistry().getUpgradeHandler().handleUpgradeTrap(player, trapId);
        if (success) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.sendMessage("§a§lBAŞARILI! §7Tuzak yükseltildi.");
            // Yükseltme menüsünü yeniden aç
            plugin.getManagerRegistry().getUpgradeGUI().openUpgradeMenu(player, trapId);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage("§c§lHATA! §7Yükseltme başarısız.");
        }
    }
}
