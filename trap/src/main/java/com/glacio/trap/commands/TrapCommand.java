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
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

        if (command.getName().equalsIgnoreCase("trap-editor")) {
            if (!player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) {
                player.sendMessage(ColorUtils.getMsg("no-permission"));
                return true;
            }
            ItemStack wand = new ItemStack(Material.BLAZE_ROD);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.rainbow("Trap Wand"));
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            player.sendMessage(ColorUtils.getMsg("wand-given"));
            return true;
        }

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
                // Check if player is in a town and is the mayor
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
                
                // Complete the purchase
                town.getAccount().withdraw(price, "Trap Satın Alımı (ID: " + targetTrap.getId() + ")");
                targetTrap.setOwnerTownName(town.getName());
                targetTrap.getMembers().put(player.getUniqueId(), Trap.TrapRole.ADMIN);
                plugin.getTrapManager().save();
                player.sendMessage(ColorUtils.getMsg("trap-purchased")
                    .replace("%id%", String.valueOf(targetTrap.getId())));
                break;

            case "bilgi":
                player.sendMessage(ColorUtils.rainbow("--- Trap Listesi ---"));
                for (Trap t : plugin.getTrapManager().getAllTraps()) {
                    String status = t.getOwnerTownName() == null ? ChatColor.GREEN + "Satılık" : ChatColor.RED + "Sahibi: " + t.getOwnerTownName();
                    player.sendMessage(ColorUtils.format("&eID: " + t.getId() + " - " + status));
                }
                break;

            case "pvp":
                if (args.length < 2) {
                    sendHelp(player);
                    return true;
                }
                Trap tPvp = plugin.getTrapManager().getTrapAt(player.getLocation());
                if (tPvp == null || tPvp.getOwnerTownName() == null) return true;
                Trap.TrapRole role = tPvp.getMembers().get(player.getUniqueId());
                if (role == null || role == Trap.TrapRole.MEMBER) {
                    player.sendMessage(ColorUtils.getMsg("no-permission"));
                    return true;
                }
                Town pvpTown = TownyAPI.getInstance().getTown(tPvp.getOwnerTownName());
                if (pvpTown == null) return true;
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
                
            case "yetkial":
                if (args.length < 2) {
                    sendHelp(player);
                    return true;
                }
                Trap tAuth = plugin.getTrapManager().getTrapAt(player.getLocation());
                if (tAuth == null || !player.getUniqueId().equals(getAdminOfTrap(tAuth))) {
                    player.sendMessage(ColorUtils.getMsg("only-admin-can-auth"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    tAuth.getMembers().remove(target.getUniqueId());
                    plugin.getTrapManager().save();
                    player.sendMessage(ColorUtils.getMsg("auth-removed").replace("%player%", target.getName()));
                }
                break;
            case "help":
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-header")));
        player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-al")));
        player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-bilgi")));
        player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-pvp")));
        player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-yetkial")));
        if (player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) {
            player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-editor")));
            player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-ekle")));
            player.sendMessage(ColorUtils.format(plugin.getLang().getString("help-sil")));
        }
    }

    private UUID getAdminOfTrap(Trap trap) {
        for (Map.Entry<UUID, Trap.TrapRole> entry : trap.getMembers().entrySet()) {
            if (entry.getValue() == Trap.TrapRole.ADMIN) return entry.getKey();
        }
        return null;
    }
}
