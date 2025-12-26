package com.glacio.trap.commands;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.ColorUtils;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrapMenuCommand implements CommandExecutor {
    private final TrapSystem plugin;

    public TrapMenuCommand(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.getMsg("only-players"));
            return true;
        }
        
        Player player = (Player) sender;
        Resident resident = TownyAPI.getInstance().getResident(player);
        
        if (resident == null || !resident.hasTown()) {
            player.sendMessage(ChatColor.RED + "Bir şehirde olmalısınız!");
            return true;
        }
        
        Town town = resident.getTownOrNull();
        if (town == null) return true;
        
        // Find traps owned by this town
        Trap ownedTrap = null;
        for (Trap trap : plugin.getTrapManager().getAllTraps()) {
            if (town.getName().equals(trap.getOwnerTownName())) {
                ownedTrap = trap;
                break;
            }
        }
        
        if (ownedTrap == null) {
            player.sendMessage(ChatColor.RED + "Şehrinize ait bir trap bulunamadı!");
            return true;
        }
        
        // Open visual menu
        plugin.getMenuManager().openTrapMenu(player, ownedTrap);
        return true;
    }
}
