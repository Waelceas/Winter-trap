package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TrapShopLogic {
    private final TrapSystem plugin;
    
    public TrapShopLogic(TrapSystem plugin) {
        this.plugin = plugin;
    }
    
    public boolean processPurchase(Player player, int trapId) {
        double price = plugin.getConfig().getDouble("trap-prices.level-" + 1, 5000.0);
        
        if (!hasEnoughMoney(player, price)) {
            player.sendMessage("§cYetersiz para! Gerekli: §a" + price + " TL");
            return false;
        }
        
        String townName = getPlayerTown(player);
        if (townName == null) {
            player.sendMessage("§cKasaban bulunamadı!");
            return false;
        }
        
        if (!withdrawMoney(player, price)) {
            return false;
        }
        
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap != null) {
            trap.setOwnerTownName(townName);
            plugin.getTrapManager().saveTraps();
            
            player.sendMessage("§aTuzak satın alındı! ID: #" + trapId);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            return true;
        }
        
        return false;
    }
    
    public boolean processUpgrade(Player player, int trapId) {
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        if (stats == null) {
            player.sendMessage("§cTuzak bilgileri bulunamadı!");
            return false;
        }
        
        double cost = (stats.getLevel() + 1) * 10000.0;

        if (!hasEnoughMoney(player, cost)) {
            player.sendMessage("§cBakiyeniz yetersiz!");
            return false;
        }

        if (!withdrawMoney(player, cost)) {
            return false;
        }
        
        stats.setLevel(stats.getLevel() + 1);
        plugin.getTrapLevelManager().saveStats();

        player.sendMessage("§a§lTEBRİKLER! §fTuzağınız " + stats.getLevel() + ". seviyeye ulaştı.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        return true;
    }
    
    public boolean processSell(Player player, int trapId, double price) {
        if (plugin.getEconomy() == null) {
            player.sendMessage(ColorUtils.colorize("&cEkonomi sistemi mevcut değil!"));
            return false;
        }
        
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) {
            player.sendMessage("§cTuzak bulunamadı!");
            return false;
        }
        
        String townName = getPlayerTown(player);
        if (townName == null || !townName.equals(trap.getOwnerTownName())) {
            player.sendMessage("§cBu tuzak sana ait değil!");
            return false;
        }
        
        // Tuzak için satış fiyatı belirle
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        if (stats != null) {
            stats.setForSale(price, townName);
            plugin.getTrapLevelManager().saveStats();
        }
        
        // Para ödeme
        plugin.getEconomy().depositPlayer(player, price);
        
        player.sendMessage("§aTuzak satışa çıkarıldı! Fiyat: §e" + price + " TL");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        return true;
    }
    
    public boolean hasEnoughMoney(Player player, double amount) {
        if (plugin.getEconomy() == null) {
            player.sendMessage(ColorUtils.colorize("&cEkonomi sistemi mevcut değil!"));
            return false;
        }
        return plugin.getEconomy().getBalance(player) >= amount;
    }
    
    public boolean withdrawMoney(Player player, double amount) {
        if (plugin.getEconomy() == null) {
            player.sendMessage(ColorUtils.colorize("&cEkonomi sistemi mevcut değil!"));
            return false;
        }
        
        if (plugin.getEconomy().getBalance(player) < amount) {
            return false;
        }
        
        plugin.getEconomy().withdrawPlayer(player, amount);
        return true;
    }
    
    public String getPlayerTown(Player player) {
        com.palmergames.bukkit.towny.object.Resident res = com.palmergames.bukkit.towny.TownyAPI.getInstance().getResident(player);
        return (res != null && res.hasTown()) ? res.getTownOrNull().getName() : null;
    }
    
    public boolean canPlayerAccessTrap(Player player, Trap trap) {
        String townName = getPlayerTown(player);
        return townName != null && townName.equals(trap.getOwnerTownName());
    }
    
    public double getTrapPrice(int trapId) {
        return plugin.getConfig().getDouble("trap-prices.level-" + 1, 5000.0);
    }
    
    public double getUpgradeCost(int currentLevel) {
        return (currentLevel + 1) * 10000.0;
    }
}
