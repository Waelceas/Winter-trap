package com.glacio.trap.commands;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.ColorUtils;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class TrapCommand implements CommandExecutor {
    private final TrapSystem plugin;

    public TrapCommand(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.getMsg("only-players"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("yardım") || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "ekle":
                if (!player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) {
                    player.sendMessage(ColorUtils.getMsg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sendHelp(player);
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
                if (!player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) {
                    player.sendMessage(ColorUtils.getMsg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sendHelp(player);
                    return true;
                }
                try {
                    plugin.getTrapManager().removeTrap(Integer.parseInt(args[1]));
                    player.sendMessage(ColorUtils.getMsg("trap-removed"));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.getMsg("invalid-id"));
                }
                break;

            case "al":
                // Check if player is in a town and is mayor
                Resident resident = TownyAPI.getInstance().getResident(player);
                if (resident == null || !resident.hasTown()) {
                    player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                    return true;
                }
                Town town = resident.getTownOrNull();
                if (town == null) return true;
                if (!resident.isMayor()) {
                    player.sendMessage(ColorUtils.getMsg("only-mayor"));
                    return true;
                }
                
                // Check if town already owns a trap
                for (Trap t : plugin.getTrapManager().getAllTraps()) {
                    if (town.getName().equals(t.getOwnerTownName())) {
                        player.sendMessage(ColorUtils.getMsg("already-has-trap"));
                        return true;
                    }
                }
                
                Trap targetTrap = null;
                
                // Check if an ID was provided
                if (args.length > 1) {
                    try {
                        int trapId = Integer.parseInt(args[1]);
                        targetTrap = plugin.getTrapManager().getTrapById(trapId);
                        if (targetTrap == null) {
                            player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtils.getMsg("invalid-id"));
                        return true;
                    }
                } else {
                    // Fall back to getting trap at player's location
                    targetTrap = plugin.getTrapManager().getTrapAt(player.getLocation());
                    if (targetTrap == null) {
                        player.sendMessage(ColorUtils.getMsg("not-in-trap-zone"));
                        return true;
                    }
                }
                
                // Check if the trap is already owned
                if (targetTrap.getOwnerTownName() != null) {
                    player.sendMessage(ColorUtils.getMsg("already-sold"));
                    return true;
                }
                
                // Process payment
                double price = plugin.getConfig().getDouble("trap-price", 5000.0);
                if (town.getAccount().getHoldingBalance() < price) {
                    player.sendMessage(ColorUtils.getMsg("insufficient-funds").replace("%price%", String.valueOf(price)));
                    return true;
                }
                
                // Complete purchase - ownership goes to town, not individual
                town.getAccount().withdraw(price, "Trap Satın Alımı (ID: " + targetTrap.getId() + ")");
                targetTrap.setOwnerTownName(town.getName());
                targetTrap.setPurchaseDate(System.currentTimeMillis());
                // Add mayor as admin
                targetTrap.getMembers().put(player.getUniqueId(), Trap.TrapRole.ADMIN);
                plugin.getTrapManager().save();
                player.sendMessage(ColorUtils.getMsg("trap-purchased")
                    .replace("%id%", String.valueOf(targetTrap.getId())));
                break;

            case "sat":
                // Check if player is in a town and is mayor
                Resident sellResident = TownyAPI.getInstance().getResident(player);
                if (sellResident == null || !sellResident.hasTown()) {
                    player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                    return true;
                }
                Town sellTown = sellResident.getTownOrNull();
                if (sellTown == null) return true;
                if (!sellResident.isMayor()) {
                    player.sendMessage(ColorUtils.getMsg("only-mayor"));
                    return true;
                }
                
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /trap sat <trap-id>");
                    return true;
                }
                
                try {
                    int trapId = Integer.parseInt(args[1]);
                    Trap sellTargetTrap = plugin.getTrapManager().getTrapById(trapId);
                    if (sellTargetTrap == null) {
                        player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                        return true;
                    }
                    
                    // Check if town owns this trap
                    if (!sellTown.getName().equals(sellTargetTrap.getOwnerTownName())) {
                        player.sendMessage(ChatColor.RED + "Bu trap size ait değil!");
                        return true;
                    }
                    
                    // Calculate sell price (half of purchase price)
                    double purchasePrice = plugin.getConfig().getDouble("trap-price", 5000.0);
                    double sellPrice = purchasePrice / 2.0;
                    
                    // Remove ownership and give money
                    sellTargetTrap.setOwnerTownName(null);
                    sellTargetTrap.getMembers().clear();
                    sellTown.getAccount().deposit(sellPrice, "Trap Satışı (ID: " + trapId + ")");
                    plugin.getTrapManager().save();
                    
                    player.sendMessage(ChatColor.GREEN + "Trap başarıyla satıldı! " + ChatColor.GOLD + sellPrice + " para" + ChatColor.GREEN + " kasaya eklendi.");
                    
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.getMsg("invalid-id"));
                }
                break;

            case "yetki":
                Resident yetkiResident = TownyAPI.getInstance().getResident(player);
                if (yetkiResident == null || !yetkiResident.hasTown()) {
                    player.sendMessage(ChatColor.RED + "Bir şehirde olmalısınız!");
                    return true;
                }
                Town yetkiTown = yetkiResident.getTownOrNull();
                if (yetkiTown == null) return true;
                if (!yetkiResident.isMayor()) {
                    player.sendMessage(ChatColor.RED + "Sadece şehir belediye başkanı yetki verebilir!");
                    return true;
                }
                
                // Find town's trap
                Trap townTrap = null;
                for (Trap t : plugin.getTrapManager().getAllTraps()) {
                    if (yetkiTown.getName().equals(t.getOwnerTownName())) {
                        townTrap = t;
                        break;
                    }
                }
                
                if (townTrap == null) {
                    player.sendMessage(ChatColor.RED + "Şehrinize ait bir trap bulunamadı!");
                    return true;
                }
                
                if (args.length == 1) {
                    // Show ver|al options
                    player.sendMessage(ChatColor.YELLOW + "Yetki işlemi seçin:");
                    player.sendMessage(ChatColor.GREEN + "/trap yetki ver" + ChatColor.GRAY + " - Oyuncuya yetki ver");
                    player.sendMessage(ChatColor.RED + "/trap yetki al" + ChatColor.GRAY + " - Oyuncunun yetkisini al");
                    return true;
                }
                
                if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("ver")) {
                        // Show online players from same town
                        player.sendMessage(ChatColor.YELLOW + "Yetki verilecek oyuncuyu seçin:");
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            Resident onlineResident = TownyAPI.getInstance().getResident(onlinePlayer);
                            if (onlineResident != null && onlineResident.hasTown()) {
                                Town onlineTown = onlineResident.getTownOrNull();
                                if (onlineTown != null && onlineTown.getName().equals(yetkiTown.getName())) {
                                    if (!townTrap.getMembers().containsKey(onlinePlayer.getUniqueId())) {
                                        player.sendMessage(ChatColor.AQUA + "/trap yetki ver " + onlinePlayer.getName() + 
                                            ChatColor.GRAY + " - " + onlinePlayer.getName() + "'na yetki ver");
                                    }
                                }
                            }
                        }
                    } else if (args[1].equalsIgnoreCase("al")) {
                        // Show players who currently have permission
                        player.sendMessage(ChatColor.YELLOW + "Yetkisi alınacak oyuncuyu seçin:");
                        for (Map.Entry<UUID, Trap.TrapRole> entry : townTrap.getMembers().entrySet()) {
                            Player memberPlayer = Bukkit.getPlayer(entry.getKey());
                            if (memberPlayer != null && memberPlayer.isOnline()) {
                                player.sendMessage(ChatColor.AQUA + "/trap yetki al " + memberPlayer.getName() + 
                                    ChatColor.GRAY + " - " + memberPlayer.getName() + "'nın yetkisini al");
                            }
                        }
                        if (townTrap.getMembers().isEmpty()) {
                            player.sendMessage(ChatColor.GRAY + "Henüz kimseye yetki verilmemiş.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Geçersiz seçenek! 'ver' veya 'al' kullanın.");
                    }
                    return true;
                }
                
                if (args.length == 3) {
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    if (targetPlayer == null) {
                        player.sendMessage(ChatColor.RED + "Oyuncu bulunamadı!");
                        return true;
                    }
                    
                    if (args[1].equalsIgnoreCase("ver")) {
                        if (townTrap.getMembers().containsKey(targetPlayer.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + targetPlayer.getName() + " zaten yetkiye sahip!");
                            return true;
                        }
                        townTrap.getMembers().put(targetPlayer.getUniqueId(), Trap.TrapRole.MEMBER);
                        plugin.getTrapManager().save();
                        player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " adlı oyuncuya trap yetkisi verildi!");
                        targetPlayer.sendMessage(ChatColor.GREEN + player.getName() + " size trap yetkisi verdi!");
                    } else if (args[1].equalsIgnoreCase("al")) {
                        if (!townTrap.getMembers().containsKey(targetPlayer.getUniqueId())) {
                            player.sendMessage(ChatColor.RED + targetPlayer.getName() + "'nın zaten yetkisi yok!");
                            return true;
                        }
                        townTrap.getMembers().remove(targetPlayer.getUniqueId());
                        plugin.getTrapManager().save();
                        player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " adlı oyuncunun trap yetkisi alındı!");
                        targetPlayer.sendMessage(ChatColor.RED + player.getName() + " size olan trap yetkinizi aldı!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Geçersiz seçenek! 'ver' veya 'al' kullanın.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Kullanım: /trap yetki");
                }
                break;

            case "pvp":
                if (args.length < 2) {
                    sendHelp(player);
                    return true;
                }
                Trap tPvp = plugin.getTrapManager().getTrapAt(player.getLocation());
                if (tPvp == null || tPvp.getOwnerTownName() == null) return true;
                
                // Check if player is from owning town or has permission
                Resident pvpResident = TownyAPI.getInstance().getResident(player);
                if (pvpResident == null || !pvpResident.hasTown()) return true;
                Town pvpTown = pvpResident.getTownOrNull();
                if (pvpTown == null || !pvpTown.getName().equals(tPvp.getOwnerTownName())) {
                    player.sendMessage(ChatColor.RED + "Bu trap'ın PvP ayarlarını değiştiremezsiniz!");
                    return true;
                }
                
                Trap.TrapRole role = tPvp.getMembers().get(player.getUniqueId());
                if (role == null || role == Trap.TrapRole.MEMBER) {
                    player.sendMessage(ColorUtils.getMsg("no-permission"));
                    return true;
                }
                
                double pvpCost = 1000.0;
                if (pvpTown.getAccount().getHoldingBalance() < pvpCost) {
                    player.sendMessage(ColorUtils.getMsg("pvp-no-funds"));
                    return true;
                }
                pvpTown.getAccount().withdraw(pvpCost, "PvP Değişimi");
                boolean enable = args[1].equalsIgnoreCase("aç");
                tPvp.setPvpEnabled(enable);
                plugin.getTrapManager().save();
                player.sendMessage(ColorUtils.getMsg("pvp-status-changed").replace("%status%", (enable ? "Açık" : "Kapalı")));
                break;

            case "bilgi":
                player.sendMessage(ColorUtils.rainbow("--- Trap Listesi ---"));
                for (Trap t : plugin.getTrapManager().getAllTraps()) {
                    String status = t.getOwnerTownName() == null ? ChatColor.GREEN + "Satılık" : ChatColor.RED + "Sahibi: " + t.getOwnerTownName();
                    player.sendMessage(ColorUtils.format("&eID: " + t.getId() + " - " + status));
                }
                break;

            case "menu":
                // Delegate to trap-menu command
                return new TrapMenuCommand(plugin).onCommand(sender, command, label, args);

            case "seviye":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /trap seviye <bilgi|artır>");
                    return true;
                }
                
                Resident levelResident = TownyAPI.getInstance().getResident(player);
                if (levelResident == null || !levelResident.hasTown()) {
                    player.sendMessage(ChatColor.RED + "Bir şehirde olmalısınız!");
                    return true;
                }
                Town levelTown = levelResident.getTownOrNull();
                if (levelTown == null) return true;
                
                // Find town's trap
                Trap levelTrap = null;
                for (Trap t : plugin.getTrapManager().getAllTraps()) {
                    if (levelTown.getName().equals(t.getOwnerTownName())) {
                        levelTrap = t;
                        break;
                    }
                }
                
                if (levelTrap == null) {
                    player.sendMessage(ChatColor.RED + "Şehrinize ait bir trap bulunamadı!");
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("bilgi")) {
                    // Show trap level information
                    player.sendMessage(ChatColor.GOLD + "=== Trap Seviye Bilgisi ===");
                    player.sendMessage(ChatColor.YELLOW + "Trap ID: " + ChatColor.WHITE + levelTrap.getId());
                    player.sendMessage(ChatColor.YELLOW + "Şehir: " + ChatColor.WHITE + levelTown.getName());
                    // Note: TrapStats functionality would need to be implemented
                    player.sendMessage(ChatColor.GRAY + "Seviye sistemi geliştirme aşamasındadır.");
                } else if (args[1].equalsIgnoreCase("artır")) {
                    // Increase trap level (placeholder for future implementation)
                    player.sendMessage(ChatColor.YELLOW + "Seviye artırma sistemi yakında eklenecek!");
                }
                break;

            case "help":
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Trap Yardım ===");
        player.sendMessage(ChatColor.YELLOW + "/trap al <id>" + ChatColor.GRAY + " - Trap satın alır (sadece belediye başkanı)");
        player.sendMessage(ChatColor.YELLOW + "/trap sat <id>" + ChatColor.GRAY + " - Trap'ı satış fiyatının yarısına sat");
        player.sendMessage(ChatColor.YELLOW + "/trap yetki <ver|al> <oyuncu>" + ChatColor.GRAY + " - Trap yetkisi verir/alır (sadece belediye başkanı)");
        player.sendMessage(ChatColor.YELLOW + "/trap seviye <bilgi|artır>" + ChatColor.GRAY + " - Trap seviye bilgisi/geliştirme");
        player.sendMessage(ChatColor.YELLOW + "/trap pvp <aç|kapat>" + ChatColor.GRAY + " - Trap PvP ayarını değiştirir");
        player.sendMessage(ChatColor.YELLOW + "/trap bilgi" + ChatColor.GRAY + " - Tüm trap'ları listeler");
        player.sendMessage(ChatColor.YELLOW + "/trap menu" + ChatColor.GRAY + " - Trap menüsünü açar");
        player.sendMessage(ChatColor.YELLOW + "/trap help" + ChatColor.GRAY + " - Bu yardım menüsünü gösterir");
    }
}
