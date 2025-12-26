package com.glacio.trap.commands;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.managers.TrapLevelManager;
import com.glacio.trap.managers.TrapManager;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TrapShopCommand implements CommandExecutor, TabCompleter {
    private final TrapSystem plugin;
    private final TrapManager trapManager;
    private final TrapLevelManager levelManager;
    private final NumberFormat currencyFormat;

    public TrapShopCommand(TrapSystem plugin) {
        this.plugin = plugin;
        this.trapManager = plugin.getTrapManager();
        this.levelManager = plugin.getTrapLevelManager();
        this.currencyFormat = NumberFormat.getInstance();
        this.currencyFormat.setMaximumFractionDigits(2);
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
            case "menu":
                return handleMenuCommand(player);
            case "list":
                return handleListCommand(player);
            case "info":
                return handleInfoCommand(player, args);
            case "sell":
                return handleSellCommand(player, args);
            case "buy":
                return handleBuyCommand(player, args);
            case "cancel":
                return handleCancelCommand(player, args);
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
            
            if (price <= 0) {
                player.sendMessage(ColorUtils.colorize("&cFiyat 0'dan büyük olmalı!"));
                return true;
            }
            
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
            
            if (!resident.isMayor()) {
                player.sendMessage(ColorUtils.getMsg("only-mayor"));
                return true;
            }
            
            // Check if trap belongs to player's town
            if (!trapManager.isTrapOwnedByTown(trapId, town.getUUID())) {
                player.sendMessage(ColorUtils.getMsg("not-your-trap"));
                return true;
            }
            
            // Check if trap is already for sale
            if (levelManager.isTrapForSale(trapId)) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak zaten satılık!"));
                return true;
            }
            
            // Put trap for sale
            levelManager.setTrapForSale(trapId, price, town.getName());
            player.sendMessage(ColorUtils.colorize("&aTuzak başarıyla satışa çıkarıldı! Fiyat: &e" + currencyFormat.format(price) + " &aTL"));
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz sayı formatı!"));
        }
        
        return true;
    }
    
    private boolean handleMenuCommand(Player player) {
        plugin.getShopManager().openShopMenu(player);
        return true;
    }
    
    private boolean handleListCommand(Player player) {
        List<Map.Entry<Integer, TrapStats>> trapsForSale = plugin.getTrapLevelManager().getTrapsForSale();
        
        if (trapsForSale.isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&cSatılık tuzak bulunmuyor!"));
            return true;
        }
        
        player.sendMessage(ColorUtils.colorize("&6===== &eSatılık Tuzaklar &6====="));
        for (Map.Entry<Integer, TrapStats> entry : trapsForSale) {
            int trapId = entry.getKey();
            TrapStats stats = entry.getValue();
            Trap trap = plugin.getTrapManager().getTrapById(trapId);
            
            if (trap != null) {
                String owner = trap.getOwnerTownName() != null ? trap.getOwnerTownName() : "Bilinmiyor";
                player.sendMessage(ColorUtils.colorize("&eTuzak #" + trapId + " &7- &aSahip: " + owner + " &7- &6Fiyat: " + 
                    String.format("%.2f", stats.getSalePrice()) + " TL"));
            }
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
            Trap trap = plugin.getTrapManager().getTrapById(trapId);
            
            if (trap == null) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
            String owner = trap.getOwnerTownName() != null ? trap.getOwnerTownName() : "Yok";
            String location = trap.getCenter() != null ? 
                trap.getCenter().getWorld().getName() + " " + trap.getCenter().getBlockX() + "," + trap.getCenter().getBlockZ() : "Bilinmiyor";
            
            player.sendMessage(ColorUtils.colorize("&6===== &eTuzak Bilgileri #" + trapId + " &6====="));
            player.sendMessage(ColorUtils.colorize("&eSahip: &a" + owner));
            player.sendMessage(ColorUtils.colorize("&eSeviye: &b" + stats.getLevel()));
            player.sendMessage(ColorUtils.colorize("&eDeneyim: &b" + stats.getExperience()));
            player.sendMessage(ColorUtils.colorize("&eKonum: &7" + location));
            
            if (stats.isForSale()) {
                player.sendMessage(ColorUtils.colorize("&eDurum: &aSatılık"));
                player.sendMessage(ColorUtils.colorize("&eFiyat: &6" + String.format("%.2f", stats.getSalePrice()) + " TL"));
            } else {
                player.sendMessage(ColorUtils.colorize("&eDurum: &cSatılık Değil"));
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID!"));
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
            TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
            
            if (!stats.isForSale()) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak satılık değil!"));
                return true;
            }
            
            // Use shop manager to handle purchase
            plugin.getShopManager().handleTrapPurchase(player, trapId, stats);
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID!"));
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
            Trap trap = plugin.getTrapManager().getTrapById(trapId);
            
            if (trap == null) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            // Check if player owns this trap
            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown()) {
                player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                return true;
            }
            
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(ColorUtils.getMsg("must-be-in-town"));
                return true;
            }
            
            if (!isPlayerTrapOwner(player, trapId)) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak size ait değil!"));
                return true;
            }
            
            TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
            if (!stats.isForSale()) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak zaten satılık değil!"));
                return true;
            }
            
            // Cancel sale
            plugin.getTrapLevelManager().cancelTrapSale(trapId);
            player.sendMessage(ColorUtils.colorize("&aTuzak satışı başarıyla iptal edildi!"));
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID!"));
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
            Trap trap = plugin.getTrapManager().getTrapById(trapId);
            
            if (trap == null) {
                player.sendMessage(ColorUtils.getMsg("invalid-trap-id"));
                return true;
            }
            
            // Check if player owns this trap
            if (!isPlayerTrapOwner(player, trapId)) {
                player.sendMessage(ColorUtils.colorize("&cBu tuzak size ait değil!"));
                return true;
            }
            
            TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
            
            // Use shop manager to handle upgrade
            plugin.getShopManager().handleTrapUpgrade(player, trapId, stats);
            
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&cGeçersiz tuzak ID!"));
        }
        
        return true;
    }
    
    private boolean isPlayerTrapOwner(Player player, int trapId) {
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null || trap.getOwnerTownName() == null) {
            return false;
        }
        
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown()) {
            return false;
        }
        
        Town town = resident.getTownOrNull();
        return town != null && town.getName().equals(trap.getOwnerTownName());
    }
    
    private void sendHelp(Player player) {
        // Implementation for help command
        player.sendMessage(ColorUtils.colorize("&6===== &eTuzak Dükkanı Yardımı &6====="));
        player.sendMessage(ColorUtils.colorize("&e/trapshop menu &7- Tuzak dükkanı menüsünü aç"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop list &7- Satılık tuzakları listele"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop info <id> &7- Tuzak bilgilerini göster"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop sell <id> <fiyat> &7- Tuzak satışa çıkar"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop buy <id> &7- Tuzak satın al"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop cancel <id> &7- Satışı iptal et"));
        player.sendMessage(ColorUtils.colorize("&e/trapshop upgrade <id> &7- Tuzak yükselt"));
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("menu");
            completions.add("list");
            completions.add("info");
            completions.add("sell");
            completions.add("buy");
            completions.add("cancel");
            completions.add("upgrade");
            completions.add("help");
        } else if (args.length == 2) {
            // Add tab completion for trap IDs based on context
            if (args[0].equalsIgnoreCase("info") || 
                args[0].equalsIgnoreCase("sell") || 
                args[0].equalsIgnoreCase("buy") ||
                args[0].equalsIgnoreCase("cancel") ||
                args[0].equalsIgnoreCase("upgrade")) {
                
                // Add your trap ID completion logic here
                // For example: completions.addAll(trapManager.getTrapIds());
            }
        }
        
        // Filter based on what the user has typed so far
        if (args.length > 0) {
            String current = args[args.length - 1].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(current));
        }
        
        return completions;
    }
}