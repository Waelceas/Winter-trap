package com.glacio.trap.events;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.handlers.TrapShopSellHandler;
import com.glacio.trap.models.Trap;
import org.bukkit.entity.Player;

/**
 * TrapSellEvent - Tuzak satma işlemleri için event yöneticisi
 */
public class TrapSellEvent {
    private final TrapSystem plugin;
    private final TrapShopSellHandler sellHandler;
    
    public TrapSellEvent(TrapSystem plugin) {
        this.plugin = plugin;
        this.sellHandler = new TrapShopSellHandler(plugin);
    }
    
    /**
     * Tuzak satma işlemini yönetir
     */
    public boolean handleSellTrap(Player player, int trapId) {
        // Debug mesajı
        player.sendMessage("§e[EVENT] Trap sell event triggered for trap #" + trapId);
        
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) {
            player.sendMessage("§c[EVENT] Trap not found!");
            return false;
        }
        
        player.sendMessage("§e[EVENT] Trap found: " + trap.getOwnerTownName());
        
        // Tuzak sahibi kontrolü
        String playerTown = getPlayerTown(player);
        if (playerTown == null) {
            player.sendMessage("§c[EVENT] No town found!");
            return false;
        }
        
        if (!playerTown.equals(trap.getOwnerTownName())) {
            player.sendMessage("§c[EVENT] You don't own this trap!");
            return false;
        }
        
        // Satma işlemini gerçekleştir
        boolean success = sellHandler.handleSellTrap(player, trapId);
        player.sendMessage("§e[EVENT] Sell result: " + success);
        
        return success;
    }
    
    /**
     * Oyuncunun kasaba bilgisini alır
     */
    private String getPlayerTown(Player player) {
        // Geçici olarak null döndür - gerçek Towny entegrasyonu sonra yapılacak
        return "TestTown"; // Test için
    }
    
    /**
     * Satılabilir tuzakları listeler
     */
    public void listSellableTraps(Player player) {
        var sellableTraps = sellHandler.getSellableTraps(player);
        player.sendMessage("§6[EVENT] Sellable traps: " + sellableTraps.size());
        
        for (Trap trap : sellableTraps) {
            // Geçici olarak sabit fiyat - gerçek hesaplama sonra yapılacak
            double sellPrice = 5000.0; // Test için
            player.sendMessage("§7- Trap #" + trap.getId() + " - Price: §e" + String.format("%.2f", sellPrice));
        }
    }
}
