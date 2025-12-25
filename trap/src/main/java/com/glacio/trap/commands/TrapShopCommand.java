package com.glacio.trap.commands;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.managers.TrapLevelManager;
import com.glacio.trap.managers.TrapManager;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TrapShopCommand implements CommandExecutor, TabCompleter {
    private final TrapSystem plugin;
    private final TrapManager trapManager;
    private final TrapLevelManager levelManager;
    private final NumberFormat currencyFormat;

    public TrapShopCommand(TrapSystem plugin) {
        this.plugin = plugin;
        this.trapManager = plugin.getTrapManager();
        this.levelManager = plugin.getTrapLevelManager();
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("tr", "TR"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.getMsg("only-players"));
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell":
                return handleSellCommand(player, args);
            case "buy":
                return handleBuyCommand(player, args);
            case "cancel":
                return handleCancelCommand(player, args);
            case "list":
                return handleListCommand(player);
            case "info":
                return handleInfoCommand(player, args);
            case "upgrade":
                return handleUpgradeCommand(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleSellCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtils.colorize("&cKullanım: /trapshop sell <trap-id> <fiyat>"));
            return true;
        }

        try {
            int trapId = Integer.parseInt(args[1]);
            double price = Double.parseDouble(args[2]);
            
            // Validate price
            double minPrice = plugin.getConfig().getDouble("shop.min-price", 1000.0);
            double maxPrice = plugin.getConfig().getDouble("shop.max-price", 1000000.0);
            
            if (price < minPrice) {
                player.sendMessage(ColorUtils.colorize("&cMinimum fiyat: &e" + currencyFormat.format(minPrice)));
                return true;
            }
            
            if (price > maxPrice) {
                player.sendMessage(ColorUtils.colorize("&cMaksimum fiyat: &e" + currencyFormat.format(maxPrice)));
                return true;
            }
            
            // Check if player is the owner of the trap
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) {
                player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                return true;
            }
            
            Town town = resident.getTownOrNull();
            if (town == null) return true;
            
            // Check if trap exists and is owned by player's town
            if (!trapManager.doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            if (!trapManager.getTrapById(trapId).getOwnerTownName().equals(town.getName())) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak sizin kasabanıza ait değil!"));
                return true;
            }
            
            // Set trap for sale
            levelManager.setTrapForSale(trapId, price, town.getName());
            player.sendMessage(ColorUtils.colorize("&aTuzak başarıyla satışa çıkarıldı! Fiyat: &e" + currencyFormat.format(price)));
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz sayı formatı!"));
        }
        
        return true;
    }
    
    private boolean handleBuyCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cKullanım: /trapshop buy <trap-id>"));
            return true;
        }
        
        try {
            int trapId = Integer.parseInt(args[1]);
            
            // Check if trap exists and is for sale
            if (!trapManager.doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            TrapStats stats = levelManager.getTrapStats(trapId);
            if (!stats.isForSale()) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak satılık değil!"));
                return true;
            }
            
            // Check if player has a town
            Resident buyerResident = TownyAPI.getInstance().getResident(player);
            if (buyerResident == null || !buyerResident.hasTown()) {
                player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                return true;
            }
            
            Town buyerTown = buyerResident.getTownOrNull();
            if (buyerTown == null) return true;
            
            // Check if buyer is the mayor
            if (!buyerResident.isMayor()) {
                player.sendMessage(ColorUtils.getMsg("only-mayor"));
                return true;
            }
            
            // Check if town already has a trap
            if (trapManager.getTrapByTown(buyerTown.getName()) != null) {
                player.sendMessage(ColorUtils.getMsg("already-has-trap"));
                return true;
            }
            
            double price = stats.getSalePrice();
            double tax = price * (plugin.getConfig().getDouble("shop.tax-percentage", 10.0) / 100.0);
            double totalCost = price + tax;
            
            // Check if town has enough money
            if (buyerTown.getAccount().getHoldingBalance() < totalCost) {
                player.sendMessage(ColorUtils.colorize("&cYeterli paranız yok! Gerekli: &e" + 
                    currencyFormat.format(totalCost) + " &c(Vergi Dahil: " + 
                    currencyFormat.format(tax) + ")"));
                return true;
            }
            
            // Transfer ownership
            try {
                // Pay the seller (town)
                Town sellerTown = TownyAPI.getInstance().getTown(stats.getSellingTown());
                if (sellerTown != null) {
                    buyerTown.getAccount().payTo(price, sellerTown, "Trap Satın Alımı (ID: " + trapId + ")");
                }
                
                // Pay tax to server
                if (tax > 0) {
                    buyerTown.getAccount().withdraw(tax, "Trap Vergisi (ID: " + trapId + ")");
                }
                
                // Transfer trap ownership
                trapManager.getTrapById(trapId).setOwnerTownName(buyerTown.getName());
                stats.removeFromSale();
                
                // Notify both parties
                player.sendMessage(ColorUtils.colorize("&aTuzak başarıyla satın alındı! Ödenen: &e" + 
                    currencyFormat.format(totalCost) + " &a(Vergi: " + currencyFormat.format(tax) + ")"));
                
                // Notify seller if online
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getUniqueId().equals(sellerTown.getMayor().getUUID())) {
                        online.sendMessage(ColorUtils.colorize("&aTrap #" + trapId + " satıldı! Kazanç: &e" + 
                            currencyFormat.format(price)));
                        break;
                    }
                }
                
            } catch (Exception e) {
                player.sendMessage(ColorUtils.colorize("&cEkonomi işlemi sırasında bir hata oluştu!"));
                plugin.getLogger().severe("Trap purchase failed: " + e.getMessage());
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID'si!"));
        }
        
        return true;
    }
    
    private boolean handleCancelCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cKullanım: /trapshop cancel <trap-id>"));
            return true;
        }
        
        try {
            int trapId = Integer.parseInt(args[1]);
            
            // Check if trap exists
            if (!trapManager.doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            // Check if player is the owner
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) {
                player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                return true;
            }
            
            Town town = resident.getTownOrNull();
            if (town == null) return true;
            
            if (!trapManager.getTrapById(trapId).getOwnerTownName().equals(town.getName())) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak sizin kasabanıza ait değil!"));
                return true;
            }
            
            // Cancel sale
            levelManager.cancelTrapSale(trapId);
            player.sendMessage(ColorUtils.colorize("&aTuzak satışı iptal edildi."));
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID'si!"));
        }
        
        return true;
    }
    
    private boolean handleListCommand(Player player) {
        List<Map.Entry<Integer, TrapStats>> trapsForSale = levelManager.getTrapsForSale();
        
        if (trapsForSale.isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&cŞu anda satılık tuzak bulunmuyor."));
            return true;
        }
        
        player.sendMessage(ColorUtils.colorize("&6&l=== &eSatılık Tuzaklar &6&l==="));
        
        for (Map.Entry<Integer, TrapStats> entry : trapsForSale) {
            int trapId = entry.getKey();
            TrapStats stats = entry.getValue();
            
            String sellerTown = stats.getSellingTown();
            String levelInfo = "Seviye " + stats.getLevel();
            String price = currencyFormat.format(stats.getSalePrice()) + " ⛃";
            
            player.sendMessage(ColorUtils.colorize(
                String.format("&7[&a%d&7] &e%s &7- &b%s &7- &6%s", 
                    trapId, levelInfo, sellerTown, price)
            ));
        }
        
        return true;
    }
    
    private boolean handleInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cKullanım: /trapshop info <trap-id>"));
            return true;
        }
        
        try {
            int trapId = Integer.parseInt(args[1]);
            
            // Check if trap exists
            if (!trapManager.doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            TrapStats stats = levelManager.getTrapStats(trapId);
            
            // Get trap info
            player.sendMessage(ColorUtils.colorize("&6&l=== &eTuzak Bilgileri #" + trapId + " &6&l==="));
            player.sendMessage(ColorUtils.colorize("&7Seviye: &e" + stats.getLevel()));
            player.sendMessage(ColorUtils.colorize("&7Tecrübe: &e" + stats.getExperience() + 
                " &7/ " + levelManager.getExpForNextLevel(stats.getLevel())));
            player.sendMessage(ColorUtils.colorize("&7Yakalanan Oyuncular: &e" + stats.getPlayerCaptures()));
            player.sendMessage(ColorUtils.colorize("&7Toplam Hasar: &e" + String.format("%.1f", stats.getTotalDamageDealt())));
            player.sendMessage(ColorUtils.colorize("&7Toplam Ölüm: &e" + stats.getTotalKills()));
            
            if (stats.isForSale()) {
                player.sendMessage(ColorUtils.colorize("&7Durum: &aSatılık - " + 
                    currencyFormat.format(stats.getSalePrice()) + " ⛃"));
                player.sendMessage(ColorUtils.colorize("&7Satıcı: &e" + stats.getSellingTown()));
            } else {
                player.sendMessage(ColorUtils.colorize("&7Durum: &cSatılık Değil"));
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID'si!"));
        }
        
        return true;
    }
    
    private boolean handleUpgradeCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&cKullanım: /trapshop upgrade <trap-id>"));
            return true;
        }
        
        try {
            int trapId = Integer.parseInt(args[1]);
            
            // Check if trap exists
            if (!trapManager.doesTrapExist(trapId)) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            // Check if player is the owner
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) {
                player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                return true;
            }
            
            Town town = resident.getTownOrNull();
            if (town == null) return true;
            
            if (!trapManager.getTrapById(trapId).getOwnerTownName().equals(town.getName())) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak sizin kasabanıza ait değil!"));
                return true;
            }
            
            // Check if trap is already at max level
            TrapStats stats = levelManager.getTrapStats(trapId);
            if (stats.getLevel() >= levelManager.getMaxLevel()) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak zaten maksimum seviyede!"));
                return true;
            }
            
            // Calculate upgrade cost
            int nextLevel = stats.getLevel() + 1;
            Map<String, Object> nextLevelInfo = levelManager.getLevelInfo(nextLevel);
            double upgradeCost = (double) nextLevelInfo.getOrDefault("price", 0.0);
            
            // Check if town can afford the upgrade
            if (town.getAccount().getHoldingBalance() < upgradeCost) {
                player.sendMessage(ColorUtils.colorize("&cYükseltme için yeterli paranız yok! Gerekli: &e" + 
                    currencyFormat.format(upgradeCost)));
                return true;
            }
            
            // Perform upgrade
            try {
                town.getAccount().withdraw(upgradeCost, "Trap Yükseltme (ID: " + trapId + ")");
                stats.setLevel(nextLevel);
                
                // Apply level bonuses
                // TODO: Apply level-based bonuses here
                
                player.sendMessage(ColorUtils.colorize("&aTuzak başarıyla yükseltildi! Yeni seviye: &e" + nextLevel));
                player.sendMessage(ColorUtils.colorize("&7Harcanan: &e" + currencyFormat.format(upgradeCost) + " ⛃"));
                
            } catch (Exception e) {
                player.sendMessage(ColorUtils.colorize("&cEkonomi işlemi sırasında bir hata oluştu!"));
                plugin.getLogger().severe("Trap upgrade failed: " + e.getMessage());
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID'si!"));
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage(ColorUtils.colorize("&6&l=== &eTuzak Marketi &6&l==="));
        player.sendMessage(ColorUtils.colorize("&e/trapshop list &7- Satılık tuzakları listeler"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop info <id> &7- Tuzak bilgilerini gösterir"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop sell <id> <fiyat> &7- Tuzak satışa çıkarır"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop buy <id> &7- Tuzak satın alır"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop cancel <id> &7- Tuzak satışını iptal eder"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop upgrade <id> &7- Tuzak seviyesini yükseltir"));
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            String[] subcommands = {"list", "info", "sell", "buy", "cancel", "upgrade"};
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Trap IDs for info/sell/buy/cancel/upgrade
            if (args[0].equalsIgnoreCase("info") || 
                args[0].equalsIgnoreCase("buy") || 
                args[0].equalsIgnoreCase("upgrade")) {
                
                // For buy, only show traps that are for sale
                if (args[0].equalsIgnoreCase("buy")) {
                    for (Map.Entry<Integer, TrapStats> entry : levelManager.getTrapsForSale()) {
                        completions.add(String.valueOf(entry.getKey()));
                    }
                } 
                // For upgrade, only show traps owned by player's town
                else if (args[0].equalsIgnoreCase("upgrade")) {
                    Resident resident = TownyAPI.getInstance().getResident(player);
                    if (resident != null && resident.hasTown()) {
                        String townName = resident.getTownOrNull().getName();
                        for (int trapId : trapManager.getTrapIds()) {
                            if (trapManager.getTrapById(trapId).getOwnerTownName().equals(townName)) {
                                completions.add(String.valueOf(trapId));
                            }
                        }
                    }
                } 
                // For info and cancel, show all traps
                else {
                    for (int trapId : trapManager.getTrapIds()) {
                        completions.add(String.valueOf(trapId));
                    }
                }
            }
        }
        
        return completions;
    }
}
