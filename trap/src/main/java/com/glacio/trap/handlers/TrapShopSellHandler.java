package com.glacio.trap.handlers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.managers.TrapMarket;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TrapShopSellHandler - Sadece tuzak satma işlemleri
 */
public class TrapShopSellHandler {
    private final TrapSystem plugin;
    
    public TrapShopSellHandler(TrapSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Tuzak satma işlemini yönetir - tuzakları merkezi havuza ekler
     */
    public boolean handleSellTrap(Player player, int trapId) {
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
        
        // Market fiyatını hesapla
        double marketPrice = TrapMarket.MarketEntry.calculateMarketPrice(plugin, trapId);
        if (marketPrice <= 0) {
            player.sendMessage(ColorUtils.getMsg("cannot-sell"));
            return false;
        }
        
        // Tuzak sahibini temizle (havuza eklemek için)
        trap.setOwnerTownName(null);
        trap.getMembers().clear();
        trap.setMarketPrice(marketPrice);
        plugin.getTrapManager().saveTraps();
        
        // Tuzakı havuza ekle
        TrapMarket market = plugin.getManagerRegistry().getTrapMarket();
        boolean addedToMarket = market.addToMarket(trapId, marketPrice, playerTown);
        
        if (!addedToMarket) {
            // Hata durumunda tuzak sahibini geri yükle
            trap.setOwnerTownName(playerTown);
            plugin.getTrapManager().saveTraps();
            player.sendMessage("§c§lHATA! §7Tuzak havuza eklenemedi!");
            return false;
        }
        
        player.sendMessage(ColorUtils.getMsg("trap-sold")
                .replace("%price%", String.format("%.2f", marketPrice))
                .replace("%trap%", "#" + trapId));
        player.sendMessage("§a§lBAŞARILI! §7Tuzakınız havuza eklendi, diğer oyuncular satın alabilir!");
        
        return true;
    }
    
    /**
     * Satılabilir tuzakları listeler
     */
    public List<Trap> getSellableTraps(Player player) {
        String playerTown = getPlayerTown(player);
        if (playerTown == null) {
            return List.of();
        }
        
        return plugin.getTrapManager().getAllTraps().stream()
                .filter(trap -> playerTown.equals(trap.getOwnerTownName()))
                .collect(Collectors.toList());
    }
    
    /**
     * Satış fiyatını hesaplar
     */
    private double calculateSellPrice(int trapId) {
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
        if (stats == null) return 0;
        
        int level = stats.getLevel();
        int kills = stats.getTotalKills();
        
        // Config'den fiyatları al
        double basePrice = plugin.getConfig().getDouble("pricing.sell.base-price", 5000.0);
        double levelBonus = plugin.getConfig().getDouble("pricing.sell.level-bonus", 2000.0);
        double killBonus = plugin.getConfig().getDouble("pricing.sell.kill-bonus", 100.0);
        
        // Temel fiyat + seviye bonus + kill bonus
        return basePrice + (level * levelBonus) + (kills * killBonus);
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
     * Satış menüsü item'i oluşturur
     */
    public ItemStack createSellItem(int slot, Trap trap) {
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
        int level = stats != null ? stats.getLevel() : 1;
        int kills = stats != null ? stats.getTotalKills() : 0;
        double sellPrice = calculateSellPrice(trap.getId());
        
        String displayName = ChatColor.YELLOW + "Tuzak #" + trap.getId() + " " + ChatColor.GRAY + "(Satılık)";
        
        List<String> lore = List.of(
            ChatColor.GRAY + "Seviye: " + ChatColor.GREEN + level,
            ChatColor.GRAY + "Öldürme: " + ChatColor.RED + kills,
            ChatColor.GRAY + "Satış Fiyatı: " + ChatColor.GOLD + String.format("%.2f", sellPrice),
            "",
            ChatColor.YELLOW + "Tıkla sat!"
        );
        
        return com.glacio.trap.utils.GuiUtils.createItem(
            org.bukkit.Material.DIAMOND_PICKAXE,
            displayName,
            lore,
            false
        );
    }
}
