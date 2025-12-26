package com.glacio.trap.gui;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.handlers.TrapShopBuyHandler;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * TrapBuyGUI - Sadece satın alma menüsü
 */
public class TrapBuyGUI {
    private final TrapSystem plugin;
    private final TrapShopBuyHandler buyHandler;
    
    public TrapBuyGUI(TrapSystem plugin) {
        this.plugin = plugin;
        this.buyHandler = new TrapShopBuyHandler(plugin);
    }
    
    /**
     * Satın alma menüsünü oluşturur
     */
    public Inventory createBuyMenu(String title) {
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        // Dekoratif kenar
        addDecorativeBorder(inventory);
        
        // Geri dön ve kapat butonları
        addBackItem(inventory);
        addCloseItem(inventory);
        
        return inventory;
    }
    
    /**
     * Satın alınabilir tuzakları menüye ekler - havuzdakiler
     */
    public void addBuyableTraps(Inventory inventory) {
        // Gerçek satın alınabilir tuzakları al (havuzdakiler)
        var buyableTraps = buyHandler.getBuyableTraps();
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        
        for (int i = 0; i < buyableTraps.size() && i < slots.length; i++) {
            inventory.setItem(slots[i], buyHandler.createBuyItem(slots[i], buyableTraps.get(i)));
        }
        
        // Eğer satın alınabilir tuzak yoksa mesaj göster
        if (buyableTraps.isEmpty()) {
            inventory.setItem(22, GuiUtils.createItem(Material.BARRIER, "§c§lHavuzda Tuzak Yok", 
                List.of("§7Şu anda havuzda tuzak bulunmuyor!", "§7Oyuncular tuzak sattığında burada görünecek.", "", "§eTuzak satmak için 'Tuzak Sat' menüsünü kullanın!"), false));
        }
        
        // Bilgi paneli ekle
        inventory.setItem(4, GuiUtils.createItem(Material.BOOK, "§6§lTuzak Havuzu", 
            List.of("§7Bu menüdeki tuzaklar", "§7diğer oyuncular tarafından satılmıştır.", "§7Fiyatlar seviyeye göre belirlenir.", "", "§eTıkla ve satın al!"), false));
    }
    
    /**
     * Dekoratif kenar ekler
     */
    private void addDecorativeBorder(Inventory inventory) {
        ItemStack border = GuiUtils.createBorderItem();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inventory.setItem(i, border);
            inventory.setItem(i + 8, border);
        }
    }
    
    /**
     * Geri dön butonu ekler
     */
    private void addBackItem(Inventory inventory) {
        inventory.setItem(45, GuiUtils.createBackButton("Ana menüye dön"));
    }
    
    /**
     * Kapat butonu ekler
     */
    private void addCloseItem(Inventory inventory) {
        inventory.setItem(53, GuiUtils.createCloseButton());
    }
}
