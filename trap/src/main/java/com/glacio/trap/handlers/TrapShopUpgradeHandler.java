package com.glacio.trap.handlers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TrapShopUpgradeHandler - Sadece tuzak yükseltme işlemleri
 */
public class TrapShopUpgradeHandler {
    private final TrapSystem plugin;
    
    public TrapShopUpgradeHandler(TrapSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Tuzak yükseltme işlemini yönetir
     */
    public boolean handleUpgradeTrap(Player player, int trapId) {
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) {
            player.sendMessage(ColorUtils.getMsg("trap-not-found"));
            return false;
        }
        
        String playerTown = getPlayerTown(player);
        if (playerTown == null) {
            player.sendMessage(ColorUtils.getMsg("no-town"));
            return false;
        }
        
        if (!playerTown.equals(trap.getOwnerTownName())) {
            player.sendMessage(ColorUtils.getMsg("not-trap-owner"));
            return false;
        }
        
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        if (stats == null) {
            player.sendMessage(ColorUtils.getMsg("trap-stats-not-found"));
            return false;
        }
        
        int currentLevel = stats.getLevel();
        int maxLevel = plugin.getConfig().getInt("pricing.max-trap-level", 10);
        
        if (currentLevel >= maxLevel) {
            player.sendMessage(ColorUtils.getMsg("trap-max-level"));
            return false;
        }
        
        double upgradeCost = calculateUpgradeCost(currentLevel);
        if (!hasEnoughMoney(player, upgradeCost)) {
            player.sendMessage(ColorUtils.getMsg("insufficient-money"));
            return false;
        }
        
        // Para çek
        plugin.getEconomy().withdrawPlayer(player, upgradeCost);
        
        // Seviyeyi yükselt
        stats.setLevel(currentLevel + 1);
        plugin.getTrapLevelManager().saveStats();
        
        // Başarı mesajı ve ses
        player.sendMessage(ColorUtils.getMsg("trap-upgraded")
                .replace("%level%", String.valueOf(currentLevel + 1))
                .replace("%cost%", String.format("%.2f", upgradeCost)));
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * Yükseltilebilir tuzakları listeler
     */
    public List<Trap> getUpgradableTraps(Player player) {
        String playerTown = getPlayerTown(player);
        if (playerTown == null) {
            return List.of();
        }
        
        int maxLevel = plugin.getConfig().getInt("pricing.max-trap-level", 10);
        
        return plugin.getTrapManager().getAllTraps().stream()
                .filter(trap -> playerTown.equals(trap.getOwnerTownName()))
                .filter(trap -> {
                    TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
                    return stats != null && stats.getLevel() < maxLevel;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Yükseltme maliyetini hesaplar
     */
    public double calculateUpgradeCost(int currentLevel) {
        double baseCost = plugin.getConfig().getDouble("pricing.upgrade.base-cost", 5000.0);
        double multiplier = plugin.getConfig().getDouble("pricing.upgrade.multiplier", 1.5);
        
        return baseCost * Math.pow(multiplier, currentLevel);
    }
    
    /**
     * Oyuncunun yeterli parası var mı?
     */
    private boolean hasEnoughMoney(Player player, double amount) {
        return plugin.getEconomy().getBalance(player) >= amount;
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
     * Yükseltme menüsü item'i oluşturur
     */
    public ItemStack createUpgradeItem(int slot, Trap trap) {
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
        if (stats == null) return null;
        
        int currentLevel = stats.getLevel();
        int nextLevel = currentLevel + 1;
        double upgradeCost = calculateUpgradeCost(currentLevel);
        int maxLevel = plugin.getConfig().getInt("pricing.max-trap-level", 10);
        
        String displayName = ChatColor.YELLOW + "Tuzak #" + trap.getId() + " " + ChatColor.AQUA + "(Yükselt)";
        
        List<String> lore = List.of(
            ChatColor.GRAY + "Mevcut Seviye: " + ChatColor.GREEN + currentLevel,
            ChatColor.GRAY + "Sonraki Seviye: " + ChatColor.AQUA + nextLevel,
            ChatColor.GRAY + "Yükseltme Maliyeti: " + ChatColor.GOLD + String.format("%.2f", upgradeCost),
            ChatColor.GRAY + "Maksimum Seviye: " + ChatColor.RED + maxLevel,
            "",
            ChatColor.YELLOW + "Tıkla yükselt!"
        );
        
        return com.glacio.trap.utils.GuiUtils.createItem(
            org.bukkit.Material.ANVIL,
            displayName,
            lore,
            false
        );
    }
    
    /**
     * Yükseltilebilirlik durumunu kontrol eder
     */
    public boolean canUpgradeTrap(Player player, int trapId) {
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) return false;
        
        // Oyuncunun kasabası var mı ve tuzak ona ait mi?
        String playerTown = getPlayerTown(player);
        if (playerTown == null || !playerTown.equals(trap.getOwnerTownName())) {
            return false;
        }
        
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        if (stats == null) return false;
        
        // Maksimum seviyeye ulaştı mı?
        int currentLevel = stats.getLevel();
        int maxLevel = plugin.getConfig().getInt("pricing.max-trap-level", 10);
        if (currentLevel >= maxLevel) return false;
        
        // Yeterli parası var mı?
        double upgradeCost = calculateUpgradeCost(currentLevel);
        return hasEnoughMoney(player, upgradeCost);
    }
}
