package com.glacio.trap.listeners;

import com.glacio.trap.TrapSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;

public class ShopListener implements Listener {
    private final TrapSystem plugin;

    public ShopListener(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Tuzak")) return; // Sadece bizim menülerse çalış
        
        event.setCancelled(true); // Eşyaları kimse yerinden oynatmasın
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // ANA MENÜ KONTROLÜ
        if (title.equals("§6§lTuzak Dükkanı")) {
            if (slot == 20) {
                Inventory buyInv = plugin.getManagerRegistry().getBuyGUI().createBuyMenu("§6§lTuzak Satın Al");
                player.openInventory(buyInv);
            }
            if (slot == 22) {
                Inventory sellInv = plugin.getManagerRegistry().getSellGUI().createSellMenu("§6§lTuzak Sat");
                plugin.getManagerRegistry().getSellGUI().addSellableTraps(sellInv, player);
                plugin.getManagerRegistry().getSellGUI().addBackItem(sellInv);
                plugin.getManagerRegistry().getSellGUI().addCloseItem(sellInv);
                player.openInventory(sellInv);
            }
            if (slot == 24) plugin.getManagerRegistry().getMainMenuHandler().openMyTrapsMenu(player);
        }

        // SATMA MENÜSÜ KONTROLÜ
        else if (title.equals("§6§lTuzak Sat")) {
            if (slot == 45) {
                plugin.getManagerRegistry().getMainMenuGUI().openMainMenu(player);
                return;
            }
            // Eğer bir tuzağa tıklandıysa (Örn: slot 10-43 arası)
            if (slot >= 10 && slot <= 43 && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                // Eşya isminden trapId alıp Merkezi İşlemci'ye gönder
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                String cleanName = org.bukkit.ChatColor.stripColor(itemName);
                if (cleanName.startsWith("Tuzak #")) {
                    try {
                        int trapId = Integer.parseInt(cleanName.split("#")[1].split(" ")[0]);
                        plugin.getManagerRegistry().getActionHandler().handleSell(player, trapId);
                    } catch (Exception e) {
                        player.sendMessage("§cTuzak ID'si okunamadı!");
                    }
                }
            }
        }

        // SATIN ALMA MENÜSÜ KONTROLÜ
        else if (title.equals("§6§lTuzak Satın Al")) {
            if (slot == 45) {
                plugin.getManagerRegistry().getMainMenuGUI().openMainMenu(player);
                return;
            }
            // Tuzak tıklaması
            if (slot >= 10 && slot <= 43 && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                String cleanName = org.bukkit.ChatColor.stripColor(itemName);
                if (cleanName.startsWith("Tuzak #")) {
                    try {
                        int trapId = Integer.parseInt(cleanName.split("#")[1].split(" ")[0]);
                        plugin.getManagerRegistry().getActionHandler().handleBuy(player, String.valueOf(trapId));
                    } catch (Exception e) {
                        player.sendMessage("§cTuzak ID'si okunamadı!");
                    }
                }
            }
        }

        // TUZAKLARIM MENÜSÜ KONTROLÜ
        else if (title.equals("§6§lTuzaklarım")) {
            if (slot == 45) {
                plugin.getManagerRegistry().getMainMenuGUI().openMainMenu(player);
                return;
            }
            // Tuzak tıklaması - yükseltme menüsüne git
            if (slot >= 10 && slot <= 43 && event.getCurrentItem() != null && 
                event.getCurrentItem().getType() != Material.AIR) {
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                String cleanName = org.bukkit.ChatColor.stripColor(itemName);
                if (cleanName.startsWith("Tuzak #")) {
                    try {
                        int trapId = Integer.parseInt(cleanName.split("#")[1].split(" ")[0]);
                        var trap = plugin.getTrapManager().getTrapById(trapId);
                        if (trap != null) {
                            plugin.getManagerRegistry().getUpgradeGUI().openUpgradeMenu(player, trapId);
                        }
                    } catch (Exception e) {
                        player.sendMessage("§cTuzak ID'si okunamadı!");
                    }
                }
            }
        }

        // YÜKSELTME MENÜSÜ KONTROLÜ
        else if (title.contains("Yükseltme:")) { // contains daha güvenilir
            event.setCancelled(true);
            
            if (slot == 45) {
                plugin.getManagerRegistry().getMainMenuHandler().openMyTrapsMenu(player);
                return;
            }
            
            if (slot == 31) { // Direkt slot kontrolü daha güvenilir
                try {
                    String cleanTitle = org.bukkit.ChatColor.stripColor(title);
                    // "Yükseltme: #123" -> sadece rakamları al
                    String idString = cleanTitle.replaceAll("[^0-9]", ""); 
                    int trapId = Integer.parseInt(idString);
                    
                    plugin.getManagerRegistry().getActionHandler().handleUpgrade(player, trapId);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.2f);
                } catch (Exception e) {
                    player.sendMessage("§cTuzak ID'si başlıktan okunamadı!");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Eşyadan trapId çıkaran yardımcı metot (Kullanılmıyor ama gelecekte lazım olabilir)
     */
    @SuppressWarnings("unused")
    private int extractTrapId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        String name = item.getItemMeta().getDisplayName();
        if (name.startsWith("Tuzak #")) {
            return Integer.parseInt(name.replace("Tuzak #", ""));
        }
        return -1;
    }
}
