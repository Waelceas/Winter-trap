package com.glacio.trap.commands;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TrapAdminCommand implements CommandExecutor {
    private final TrapSystem plugin;

    public TrapAdminCommand(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.getMsg("only-players"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) {
            player.sendMessage(ColorUtils.getMsg("no-permission"));
            return true;
        }
        
        if (args.length == 0 || args[0].equalsIgnoreCase("yardım") || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfigs();
                player.sendMessage(ChatColor.GREEN + "Trap plugin yapılandırması yeniden yüklendi!");
                player.sendMessage(ChatColor.YELLOW + "Not: Yeni oluşturulan şehirler için Towny verileri güncelleniyor...");
                break;
            case "shop":
                return handleShopCommand(player, args);
            case "ekle":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /trapadmin ekle <trap-id>");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    Location[] locs = plugin.getSelections().get(player.getUniqueId());
                    if (locs == null || locs[0] == null || locs[1] == null) {
                        player.sendMessage(ColorUtils.getMsg("not-in-trap-zone"));
                        return true;
                    }
                    plugin.getTrapManager().addTrap(id, locs[0], locs[1]);
                    player.sendMessage(ColorUtils.getMsg("trap-added").replace("%id%", String.valueOf(id)));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.getMsg("invalid-id"));
                }
                break;

            case "sil":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /trapadmin sil <trap-id>");
                    return true;
                }
                try {
                    plugin.getTrapManager().removeTrap(Integer.parseInt(args[1]));
                    player.sendMessage(ColorUtils.getMsg("trap-removed"));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.getMsg("invalid-id"));
                }
                break;

            case "editor":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /trapadmin editor <chunk|wand>");
                    return true;
                }
                
                switch (args[1].toLowerCase()) {
                    case "chunk":
                        // Get current chunk boundaries
                        org.bukkit.Chunk chunk = player.getLocation().getChunk();
                        int minX = chunk.getX() * 16;
                        int minZ = chunk.getZ() * 16;
                        int maxX = minX + 15;
                        int maxZ = minZ + 15;
                        int y = player.getLocation().getBlockY();
                        
                        Location pos1 = new Location(player.getWorld(), minX, y, minZ);
                        Location pos2 = new Location(player.getWorld(), maxX, y, maxZ);
                        
                        // Store selection
                        plugin.getSelections().put(player.getUniqueId(), new Location[]{pos1, pos2});
                        player.sendMessage(ChatColor.GREEN + "Bulunduğunuz chunk seçildi: (" + minX + "," + minZ + ") - (" + maxX + "," + maxZ + ")");
                        break;
                        
                    case "wand":
                        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
                        ItemMeta meta = wand.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(ColorUtils.rainbow("Trap Wand"));
                            wand.setItemMeta(meta);
                        }
                        player.getInventory().addItem(wand);
                        player.sendMessage(ColorUtils.getMsg("wand-given"));
                        break;
                        
                    default:
                        player.sendMessage(ChatColor.RED + "Kullanım: /trapadmin editor <chunk|wand>");
                        break;
                }
                break;

            default:
                sendAdminHelp(player);
                break;
        }
        return true;
    }

    private boolean handleShopCommand(Player player, String[] args) {
        if (args.length < 2) {
            sendShopHelp(player);
            return true;
        }
        
        switch (args[1].toLowerCase()) {
            case "menu":
                plugin.getShopManager().openShopMenu(player);
                return true;
            case "remove":
                return handleRemoveTrapCommand(player, args);
            case "price":
                return handlePriceCommand(player, args);
            case "list":
                return handleShopListCommand(player);
            default:
                sendShopHelp(player);
                return true;
        }
    }

    private boolean handleRemoveTrapCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Kullanım: /trapadmin shop remove <trap-id>");
            return true;
        }
        
        try {
            int trapId = Integer.parseInt(args[2]);
            
            if (!plugin.getTrapManager().doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            // Remove trap from shop and from manager
            plugin.getTrapLevelManager().cancelTrapSale(trapId);
            plugin.getTrapManager().removeTrap(trapId);
            
            player.sendMessage(ChatColor.GREEN + "Trap #" + trapId + " başarıyla kaldırıldı!");
            return true;
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Geçersiz trap ID!");
            return true;
        }
    }

    private boolean handlePriceCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Kullanım: /trapadmin shop price <trap-id> <yeni-fiyat>");
            return true;
        }
        
        try {
            int trapId = Integer.parseInt(args[2]);
            double newPrice = Double.parseDouble(args[3]);
            
            if (!plugin.getTrapManager().doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            if (newPrice <= 0) {
                player.sendMessage(ChatColor.RED + "Fiyat 0'dan büyük olmalı!");
                return true;
            }
            
            // Update price in TrapLevelManager
            plugin.getTrapLevelManager().updateTrapPrice(trapId, newPrice);
            
            player.sendMessage(ChatColor.GREEN + "Trap #" + trapId + " fiyatı başarıyla " + 
                String.format("%.2f", newPrice) + " TL olarak güncellendi!");
            return true;
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Geçersiz sayı formatı!");
            return true;
        }
    }

    private boolean handleShopListCommand(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== Satılık Tuzaklar ===");
        var trapsForSale = plugin.getTrapLevelManager().getTrapsForSale();
        
        if (trapsForSale.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Satılık tuzak bulunmuyor.");
            return true;
        }
        
        for (var entry : trapsForSale) {
            int trapId = entry.getKey();
            var stats = entry.getValue();
            Trap trap = plugin.getTrapManager().getTrapById(trapId);
            
            if (trap != null) {
                String owner = trap.getOwnerTownName() != null ? trap.getOwnerTownName() : "Bilinmiyor";
                player.sendMessage(ChatColor.GREEN + "Trap #" + trapId + 
                    ChatColor.GRAY + " - Sahip: " + ChatColor.YELLOW + owner +
                    ChatColor.GRAY + " - Fiyat: " + ChatColor.GREEN + String.format("%.2f", stats.getSalePrice()) + " TL");
            }
        }
        
        return true;
    }

    private void sendShopHelp(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "=== Trap Admin Shop Menüsü ===");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin shop menu" + ChatColor.GRAY + " - Admin shop menüsünü aç");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin shop remove <id>" + ChatColor.GRAY + " - Tuzak kaldır");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin shop price <id> <fiyat>" + ChatColor.GRAY + " - Fiyat değiştir");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin shop list" + ChatColor.GRAY + " - Satılık tuzakları listele");
    }

    private void sendAdminHelp(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "=== Trap Admin Menüsü ===");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin reload" + ChatColor.GRAY + " - Plugin yapılandırmasını yeniden yükler");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin shop" + ChatColor.GRAY + " - Shop yönetimi menüsü");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin ekle <id>" + ChatColor.GRAY + " - Yeni trap ekler");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin sil <id>" + ChatColor.GRAY + " - Trap siler");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin editor <chunk|wand>" + ChatColor.GRAY + " - Chunk seç veya wand al");
        player.sendMessage(ChatColor.YELLOW + "/trapadmin yardım" + ChatColor.GRAY + " - Bu menüyü gösterir");
    }
}
