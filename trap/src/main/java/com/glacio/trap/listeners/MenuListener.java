package com.glacio.trap.listeners;

import com.glacio.trap.TrapSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {
    private final TrapSystem plugin;

    public MenuListener(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is a trap menu
        if (event.getView().getTitle().equals(plugin.getMenuManager().getMenuTitle())) {
            event.setCancelled(true);
            
            // Handle click
            plugin.getMenuManager().handleClick(player, event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // Check if this was a trap menu
        if (event.getView().getTitle().equals(plugin.getMenuManager().getMenuTitle())) {
            plugin.getMenuManager().closeMenu(player);
        }
    }
}
