package com.glacio.trap.gui;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.handlers.TrapShopSellHandler;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.List;

public class TrapSellGUI {
    private final TrapSystem plugin;
    private final TrapShopSellHandler sellHandler;

    // Constructor: Hem plugin hem handler almalı
    public TrapSellGUI(TrapSystem plugin, TrapShopSellHandler sellHandler) {
        this.plugin = plugin;
        this.sellHandler = sellHandler;
    }

    // Maven'ın aradığı eski isimle metodu geri koyalım ama içeriği yeni olsun
    public Inventory createSellMenu(String title) {
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        addDecorativeBorder(inventory);
        addBackItem(inventory);
        addCloseItem(inventory);
        return inventory;
    }

    // Bu metodu PUBLIC yapalım ki Handler erişebilsin (Maven hatası buradaydı)
    public void addSellableTraps(Inventory inventory, Player player) {
        List<Trap> sellableTraps = sellHandler.getSellableTraps(player);
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        
        for (int i = 0; i < sellableTraps.size() && i < slots.length; i++) {
            inventory.setItem(slots[i], sellHandler.createSellItem(slots[i], sellableTraps.get(i)));
        }
        
        // Eğer satılabilir tuzak yoksa mesaj göster
        if (sellableTraps.isEmpty()) {
            inventory.setItem(22, GuiUtils.createItem(Material.BARRIER, "§c§lSatılabilir Tuzak Yok", 
                List.of("§7Kasabanıza ait tuzak bulunmuyor!", "§7Yeni tuzak oluşturmak için /trap komutunu kullanın."), false));
        }
        
        // Bilgi paneli ekle
        inventory.setItem(4, GuiUtils.createItem(Material.BOOK, "§6§lTuzak Satış", 
            List.of("§7Tuzaklarınızı satarak havuza eklersiniz.", "§7Diğer oyuncular bu tuzakları satın alabilir.", "§7Fiyat: Seviye ve kill sayısına göre belirlenir.", "", "§eTıkla ve havuza ekle!"), false));
    }

    public void addBackItem(Inventory inventory) {
        inventory.setItem(45, GuiUtils.createItem(Material.ARROW, "§cGeri Dön", List.of("§7Ana menüye dön"), false));
    }

    public void addCloseItem(Inventory inventory) {
        inventory.setItem(53, GuiUtils.createItem(Material.BARRIER, "§cKapat", List.of("§7Menüyü kapat"), false));
    }

    private void addDecorativeBorder(Inventory inventory) {
        var pane = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 45 || i % 9 == 0 || (i+1) % 9 == 0) inventory.setItem(i, pane);
        }
    }
}
