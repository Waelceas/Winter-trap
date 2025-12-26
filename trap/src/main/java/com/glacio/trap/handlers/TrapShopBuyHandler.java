package com.glacio.trap.handlers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.managers.TrapMarket;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TrapShopBuyHandler - Sadece tuzak satın alma işlemleri
 */
public class TrapShopBuyHandler {
    private final TrapSystem plugin;
    
    public TrapShopBuyHandler(TrapSystem plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Tuzak satın alma işlemini yönetir - havuzdan alır
     */
    public boolean handleBuyTrap(Player player, int trapId) {
        player.sendMessage("§e[DEBUG] handleBuyTrap called for trap #" + trapId);
        
        // Önce havuzda mı kontrol et
        TrapMarket market = plugin.getManagerRegistry().getTrapMarket();
        if (!market.isInMarket(trapId)) {
            player.sendMessage("§cBu tuzak havuzda bulunmuyor!");
            return false;
        }
        
        TrapMarket.MarketEntry entry = market.getMarketEntry(trapId);
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) {
            player.sendMessage("§cTuzak bulunamadı!");
            return false;
        }
        
        String playerTown = getPlayerTown(player);
        player.sendMessage("§e[DEBUG] Player town: " + playerTown);
        
        if (playerTown == null) {
            player.sendMessage("§cKasabanız bulunmuyor!");
            return false;
        }
        
        // Kendi tuzakını satın almaya çalışıyor mu?
        if (playerTown.equals(entry.getSellerTown())) {
            player.sendMessage("§cKendi tuzakınızı satın alamazsınız!");
            return false;
        }
        
        double buyPrice = entry.getPrice();
        player.sendMessage("§e[DEBUG] Buy price: " + buyPrice);
        
        if (!hasEnoughMoney(player, buyPrice)) {
            player.sendMessage("§cYetersiz bakiye! Gerekli: §6" + buyPrice + " TL");
            return false;
        }
        
        // Para çek
        plugin.getEconomy().withdrawPlayer(player, buyPrice);
        player.sendMessage("§e[DEBUG] Money withdrawn");
        
        // Satıcı kasabaya para gönder (isteğe bağlı)
        // plugin.getEconomy().depositPlayer(sellerPlayer, buyPrice * 0.9); // %90 komisyon
        
        // Tuzakı havuzdan satın al
        boolean success = market.buyFromMarket(trapId, playerTown);
        if (!success) {
            // Hata durumunda parayı geri ver
            plugin.getEconomy().depositPlayer(player, buyPrice);
            player.sendMessage("§cTuzak satın alınamadı, para iade edildi!");
            return false;
        }
        
        player.sendMessage("§a§lBAŞARILI! §7Tuzak #" + trapId + " satın alındı!");
        player.sendMessage("§eFiyat: §6" + buyPrice + " TL");
        player.sendMessage("§7Tuzak artık kasabanıza ait!");
        
        return true;
    }
    
    /**
     * Satın alınabilir tuzakları listeler - havuzdakiler
     */
    public List<Trap> getBuyableTraps() {
        TrapMarket market = plugin.getManagerRegistry().getTrapMarket();
        return market.getMarketTraps().stream()
                .map(entry -> plugin.getTrapManager().getTrapById(entry.getTrapId()))
                .filter(trap -> trap != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Satın alma fiyatını hesaplar
     */
    private double calculateBuyPrice(int trapId) {
        // Konfigürasyondan fiyat al
        double basePrice = plugin.getConfig().getDouble("pricing.base-trap-price", 10000.0);
        
        // Konfigürasyondan özel fiyat varsa onu kullan
        String configPath = "trap-prices." + trapId;
        if (plugin.getConfig().contains(configPath)) {
            basePrice = plugin.getConfig().getDouble(configPath);
        }
        
        return basePrice;
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
     * Satın alma menüsü item'i oluşturur - havuz fiyatları
     */
    public ItemStack createBuyItem(int slot, Trap trap) {
        TrapMarket market = plugin.getManagerRegistry().getTrapMarket();
        TrapMarket.MarketEntry entry = market.getMarketEntry(trap.getId());
        
        double buyPrice = (entry != null) ? entry.getPrice() : 0.0;
        String sellerTown = (entry != null) ? entry.getSellerTown() : "Bilinmiyor";
        
        String displayName = ChatColor.YELLOW + "Tuzak #" + trap.getId() + " " + ChatColor.GREEN + "(Satın Al)";
        
        List<String> lore = List.of(
            ChatColor.GRAY + "Satıcı: " + ChatColor.AQUA + sellerTown,
            ChatColor.GRAY + "Fiyat: " + ChatColor.GOLD + String.format("%.2f", buyPrice),
            ChatColor.GRAY + "Konum: " + ChatColor.WHITE + formatLocation(trap.getCenter()),
            "",
            ChatColor.YELLOW + "Tıkla satın al!"
        );
        
        return com.glacio.trap.utils.GuiUtils.createItem(
            org.bukkit.Material.EMERALD,
            displayName,
            lore,
            false
        );
    }
    
    /**
     * Konum bilgisini formatlar
     */
    private String formatLocation(org.bukkit.Location loc) {
        return String.format("X:%d Z:%d", (int) loc.getX(), (int) loc.getZ());
    }
    
    /**
     * Satın alabilirlik durumunu kontrol eder - havuz kontrolü
     */
    public boolean canBuyTrap(Player player, int trapId) {
        TrapMarket market = plugin.getManagerRegistry().getTrapMarket();
        
        // Tuzak havuzda mı?
        if (!market.isInMarket(trapId)) return false;
        
        TrapMarket.MarketEntry entry = market.getMarketEntry(trapId);
        if (entry == null) return false;
        
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) return false;
        
        // Oyuncunun kasabası var mı?
        String playerTown = getPlayerTown(player);
        if (playerTown == null) return false;
        
        // Kendi tuzakunu mu almaya çalışıyor?
        if (playerTown.equals(entry.getSellerTown())) return false;
        
        // Yeterli parası var mı?
        return hasEnoughMoney(player, entry.getPrice());
    }
}
