package com.glacio.trap.gui;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.utils.GuiUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class TrapMainMenuGUI {
    private final TrapSystem plugin;

    public TrapMainMenuGUI(TrapSystem plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        String title = "§6§lTuzak Dükkanı";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 1. Kenarlıkları çiz (Merkezi metod)
        drawBorder(inv);

        // 2. Ana Butonlar
        inv.setItem(20, GuiUtils.createItem(Material.EMERALD, "§a§lSatın Al", 
            List.of("§7Yeni tuzak satın al", "§7Kasaban için tuzak kur", "", "§eTıkla satın al!"), false));
        
        inv.setItem(22, GuiUtils.createItem(Material.GOLD_INGOT, "§6§lSat", 
            List.of("§7Mevcut tuzaklarını sat", "§7Para kazan", "", "§eTıkla sat!"), false));
        
        inv.setItem(24, GuiUtils.createItem(Material.DIAMOND_PICKAXE, "§b§lTuzaklarım", 
            List.of("§7Tuzaklarını yönet", "§7Seviye yükselt", "", "§eTıkla yönet!"), false));

        // 3. İstatistikler (Placeholder'ları plugin verisine bağla)
        addStats(inv);

        // 4. Kapat butonu
        inv.setItem(53, GuiUtils.createItem(Material.BARRIER, "§cKapat", List.of("§7Menüyü kapat"), false));

        player.openInventory(inv);
    }

    private void addStats(Inventory inv) {
        // Burada plugin.getTrapManager()... diyerek gerçek veriyi çekmelisin
        inv.setItem(11, GuiUtils.createItem(Material.BOOK, "§6§lİstatistikler", 
            List.of("§7Toplam Tuzak: §e" + 25, "§7Satın Alınan: §a" + 15), false));
        
        inv.setItem(15, GuiUtils.createItem(Material.PAPER, "§6§lFiyatlar", 
            List.of("§7Temel Fiyat: §e10.000"), false));
    }

    private void drawBorder(Inventory inv) {
        // GuiUtils'deki metodu kullanarak sadece slot numaralarını belirle
        var borderItem = GuiUtils.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, borderItem);
            }
        }
    }
    
    /**
     * Satın alınan tuzak sayısını döndürür (placeholder)
     */
    private int getPurchasedCount() {
        return 15; // Gerçek veri gelecek
    }
    
    /**
     * Satılan tuzak sayısını döndürür (placeholder)
     */
    private int getSoldCount() {
        return 8; // Gerçek veri gelecek
    }
}
