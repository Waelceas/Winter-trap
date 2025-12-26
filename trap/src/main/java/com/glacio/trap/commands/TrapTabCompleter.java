package com.glacio.trap.commands;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TrapTabCompleter implements TabCompleter {
    private final TrapSystem plugin;

    public TrapTabCompleter(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("trap")) {
            if (args.length == 1) {
                // Main subcommands
                String[] subcommands = {"yardım", "help", "al", "sat", "yetki", "pvp", "bilgi", "menu", "seviye"};
                for (String sub : subcommands) {
                    if (sub.startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2) {
                String subcommand = args[0].toLowerCase();
                
                switch (subcommand) {
                    case "al":
                        // Show available trap IDs
                        for (int trapId : plugin.getTrapManager().getTrapIds()) {
                            if (plugin.getTrapManager().getTrapById(trapId).getOwnerTownName() == null) {
                                completions.add(String.valueOf(trapId));
                            }
                        }
                        break;
                        
                    case "sat":
                        // Show trap IDs owned by player's town
                        if (sender instanceof Player) {
                            for (int trapId : plugin.getTrapManager().getTrapIds()) {
                                if (plugin.getTrapManager().getTrapById(trapId).getOwnerTownName() != null) {
                                    completions.add(String.valueOf(trapId));
                                }
                            }
                        }
                        break;
                        
                    case "yetki":
                        // Show ver|al options first
                        String[] yetkiOptions = {"ver", "al"};
                        for (String option : yetkiOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                        
                    case "pvp":
                        // PvP options
                        String[] pvpOptions = {"aç", "kapat"};
                        for (String option : pvpOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                        
                    case "seviye":
                        // Level options
                        String[] levelOptions = {"bilgi", "artır"};
                        for (String option : levelOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                }
            } else if (args.length == 3) {
                String subcommand = args[0].toLowerCase();
                
                switch (subcommand) {
                    case "yetki":
                        if (args[1].equalsIgnoreCase("ver")) {
                            // Show online players from same town for giving permission
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                com.palmergames.bukkit.towny.TownyAPI townyAPI = com.palmergames.bukkit.towny.TownyAPI.getInstance();
                                com.palmergames.bukkit.towny.object.Resident resident = townyAPI.getResident(player);
                                if (resident != null && resident.hasTown()) {
                                    com.palmergames.bukkit.towny.object.Town town = resident.getTownOrNull();
                                    if (town != null) {
                                        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                                            com.palmergames.bukkit.towny.object.Resident onlineResident = townyAPI.getResident(onlinePlayer);
                                            if (onlineResident != null && onlineResident.hasTown()) {
                                                com.palmergames.bukkit.towny.object.Town onlineTown = onlineResident.getTownOrNull();
                                                if (onlineTown != null && onlineTown.getName().equals(town.getName())) {
                                                    if (onlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                                        completions.add(onlinePlayer.getName());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (args[1].equalsIgnoreCase("al")) {
                            // Show players who currently have permission
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                com.palmergames.bukkit.towny.TownyAPI townyAPI = com.palmergames.bukkit.towny.TownyAPI.getInstance();
                                com.palmergames.bukkit.towny.object.Resident resident = townyAPI.getResident(player);
                                if (resident != null && resident.hasTown()) {
                                    com.palmergames.bukkit.towny.object.Town town = resident.getTownOrNull();
                                    if (town != null) {
                                        for (Trap trap : plugin.getTrapManager().getAllTraps()) {
                                            if (town.getName().equals(trap.getOwnerTownName())) {
                                                for (java.util.UUID memberUUID : trap.getMembers().keySet()) {
                                                    Player memberPlayer = plugin.getServer().getPlayer(memberUUID);
                                                    if (memberPlayer != null && memberPlayer.isOnline()) {
                                                        if (memberPlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                                            completions.add(memberPlayer.getName());
                                                        }
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                }
            }
        } else if (command.getName().equalsIgnoreCase("trapadmin")) {
            if (args.length == 1) {
                // Admin subcommands
                String[] adminSubcommands = {"yardım", "help", "reload", "ekle", "sil", "editor"};
                for (String sub : adminSubcommands) {
                    if (sub.startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2) {
                String subcommand = args[0].toLowerCase();
                
                switch (subcommand) {
                    case "ekle":
                    case "sil":
                        // Show trap IDs
                        for (int trapId : plugin.getTrapManager().getTrapIds()) {
                            completions.add(String.valueOf(trapId));
                        }
                        break;
                        
                    case "editor":
                        // Editor options
                        String[] editorOptions = {"chunk", "wand"};
                        for (String option : editorOptions) {
                            if (option.startsWith(args[1].toLowerCase())) {
                                completions.add(option);
                            }
                        }
                        break;
                }
            }
        }
        
        return completions;
    }
}
