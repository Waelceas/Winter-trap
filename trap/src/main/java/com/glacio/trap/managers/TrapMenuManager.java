package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TrapMenuManager {
    private final TrapSystem plugin;
    private FileConfiguration menuConfig;
    private final Map<Player, Trap> openMenus = new HashMap<>();

    public TrapMenuManager(TrapSystem plugin) {
        this.plugin = plugin;
        loadMenuConfig();
        plugin.getLogger().info("TrapMenuManager initialized!");
    }

    private void loadMenuConfig() {
        File configFile = new File(plugin.getDataFolder(), "menu.yml");
        if (!configFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void openTrapMenu(Player player, Trap trap) {
        // Store the trap for this player
        openMenus.put(player, trap);
        
        // Create inventory
        String title = ColorUtils.colorize(menuConfig.getString("menu.title", "&6&lTRAP MENÜSÜ"));
        int size = menuConfig.getInt("menu.size", 27);
        Inventory inventory = Bukkit.createInventory(null, size, title);
        
        // Fill inventory with items
        fillInventory(inventory, trap, player);
        
        // Open inventory
        player.openInventory(inventory);
        
        // Play sound
        String soundName = menuConfig.getString("sounds.open", "ENTITY_CHICKEN_EGG");
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Sound not found, ignore
        }
        
        // Start auto-refresh if enabled
        if (menuConfig.getBoolean("menu.auto-refresh", true)) {
            startAutoRefresh(player, inventory, trap);
        }
    }

    private void fillInventory(Inventory inventory, Trap trap, Player player) {
        // Clear inventory first
        inventory.clear();
        
        // Add decorative items
        if (menuConfig.getBoolean("decorative.enabled", true)) {
            fillDecorativeItems(inventory);
        }
        
        // Add main items
        addTrapInfoItem(inventory, trap, player);
        addStatisticsItem(inventory, trap, player);
        addPvPStatusItem(inventory, trap, player);
        addMembersItem(inventory, trap, player);
        addTeleportItem(inventory, trap, player);
        addSettingsItem(inventory, trap, player);
        addCloseItem(inventory, trap, player);
    }

    private void fillDecorativeItems(Inventory inventory) {
        String materialName = menuConfig.getString("decorative.material", "GRAY_STAINED_GLASS_PANE");
        String name = ColorUtils.colorize(menuConfig.getString("decorative.name", " "));
        List<String> lore = menuConfig.getStringList("decorative.lore");
        boolean enchanted = menuConfig.getBoolean("decorative.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, lore, enchanted);
            
            // Fill all empty slots
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, item);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid decorative material: " + materialName);
        }
    }

    private void addTrapInfoItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.trap_info.slot", 13);
        String materialName = menuConfig.getString("items.trap_info.material", "CHEST");
        String name = ColorUtils.colorize(menuConfig.getString("items.trap_info.name", "&e&lTRAP BİLGİLERİ"));
        
        // Get trap stats
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
        int level = stats.getLevel();
        String levelName = plugin.getTrapLevelManager().getLevelDisplayName(level);
        
        List<String> lore = menuConfig.getStringList("items.trap_info.lore");
        List<String> coloredLore = new ArrayList<>();
        
        for (String line : lore) {
            String colored = line
                .replace("%trap_id%", String.valueOf(trap.getId()))
                .replace("%trap_owner%", trap.getOwnerTownName() != null ? trap.getOwnerTownName() : "Yok")
                .replace("%trap_level%", levelName)
                .replace("%trap_status%", trap.getOwnerTownName() != null ? "&aAktif" : "&cSatılık");
            
            // Handle features
            if (colored.contains("%trap_features%")) {
                List<String> features = plugin.getTrapLevelManager().getAvailableFeatures(trap.getId());
                String featuresText = features.isEmpty() ? "&7Yok" : features.stream()
                    .map(f -> "&7• " + f)
                    .collect(Collectors.joining("\n"));
                colored = colored.replace("%trap_features%", featuresText);
            }
            
            coloredLore.add(ColorUtils.colorize(colored));
        }
        
        boolean enchanted = menuConfig.getBoolean("items.trap_info.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid trap_info material: " + materialName);
        }
    }

    private void addStatisticsItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.statistics.slot", 11);
        String materialName = menuConfig.getString("items.statistics.material", "BOOK");
        String name = ColorUtils.colorize(menuConfig.getString("items.statistics.name", "&c&lİSTATİSTİKLER"));
        
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
        
        List<String> lore = menuConfig.getStringList("items.statistics.lore");
        List<String> coloredLore = new ArrayList<>();
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String lastLevelUp = stats.getLastLevelUpTime() > 0 ? sdf.format(new Date(stats.getLastLevelUpTime())) : "Hiç";
        
        for (String line : lore) {
            String colored = line
                .replace("%total_damage%", String.format("%.1f", stats.getTotalDamageDealt()))
                .replace("%total_kills%", String.valueOf(stats.getTotalKills()))
                .replace("%player_captures%", String.valueOf(stats.getPlayerCaptures()))
                .replace("%experience%", String.valueOf(stats.getExperience()))
                .replace("%last_level_up%", lastLevelUp);
            
            coloredLore.add(ColorUtils.colorize(colored));
        }
        
        boolean enchanted = menuConfig.getBoolean("items.statistics.enchanted", true);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid statistics material: " + materialName);
        }
    }

    private void addPvPStatusItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.pvp_status.slot", 15);
        String materialName = menuConfig.getString("items.pvp_status.material", "IRON_SWORD");
        String name = ColorUtils.colorize(menuConfig.getString("items.pvp_status.name", "&a&lPVP DURUMU"));
        
        List<String> lore = menuConfig.getStringList("items.pvp_status.lore");
        List<String> coloredLore = new ArrayList<>();
        
        String pvpStatus = trap.isPvpEnabled() ? "&aAçık" : "&cKapalı";
        
        for (String line : lore) {
            String colored = line.replace("%pvp_status%", pvpStatus);
            coloredLore.add(ColorUtils.colorize(colored));
        }
        
        boolean enchanted = menuConfig.getBoolean("items.pvp_status.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid pvp_status material: " + materialName);
        }
    }

    private void addMembersItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.members.slot", 22);
        String materialName = menuConfig.getString("items.members.material", "PLAYER_HEAD");
        String name = ColorUtils.colorize(menuConfig.getString("items.members.name", "&b&lÜYELER"));
        
        TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
        Map<String, Object> levelInfo = plugin.getTrapLevelManager().getLevelInfo(stats.getLevel());
        int maxMembers = (int) levelInfo.getOrDefault("max-members", 3);
        int currentMembers = trap.getMembers().size();
        
        List<String> lore = menuConfig.getStringList("items.members.lore");
        List<String> coloredLore = new ArrayList<>();
        
        for (String line : lore) {
            String colored = line
                .replace("%max_members%", String.valueOf(maxMembers))
                .replace("%current_members%", String.valueOf(currentMembers));
            
            if (colored.contains("%member_list%")) {
                StringBuilder memberList = new StringBuilder();
                for (UUID memberUuid : trap.getMembers().keySet()) {
                    String memberName = Bukkit.getOfflinePlayer(memberUuid).getName();
                    if (memberName != null) {
                        Trap.TrapRole role = trap.getMembers().get(memberUuid);
                        memberList.append("&7• ").append(memberName).append(" (").append(role).append(")\n");
                    }
                }
                if (memberList.length() == 0) {
                    memberList.append("&7Üye yok");
                }
                colored = colored.replace("%member_list%", memberList.toString().trim());
            }
            
            coloredLore.add(ColorUtils.colorize(colored));
        }
        
        boolean enchanted = menuConfig.getBoolean("items.members.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid members material: " + materialName);
        }
    }

    private void addTeleportItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.teleport.slot", 9);
        String materialName = menuConfig.getString("items.teleport.material", "ENDER_PEARL");
        String name = ColorUtils.colorize(menuConfig.getString("items.teleport.name", "&d&lIŞINLAN"));
        
        List<String> lore = menuConfig.getStringList("items.teleport.lore");
        List<String> coloredLore = new ArrayList<>();
        
        String location = "Bilinmiyor";
        if (trap.getCenter() != null) {
            location = trap.getCenter().getWorld().getName() + " " + 
                       trap.getCenter().getBlockX() + ", " + 
                       trap.getCenter().getBlockY() + ", " + 
                       trap.getCenter().getBlockZ();
        }
        
        for (String line : lore) {
            String colored = line.replace("%trap_location%", location);
            coloredLore.add(ColorUtils.colorize(colored));
        }
        
        boolean enchanted = menuConfig.getBoolean("items.teleport.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid teleport material: " + materialName);
        }
    }

    private void addSettingsItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.settings.slot", 17);
        String materialName = menuConfig.getString("items.settings.material", "REDSTONE");
        String name = ColorUtils.colorize(menuConfig.getString("items.settings.name", "&6&lAYARLAR"));
        
        List<String> lore = menuConfig.getStringList("items.settings.lore");
        List<String> coloredLore = lore.stream()
            .map(ColorUtils::colorize)
            .collect(Collectors.toList());
        
        boolean enchanted = menuConfig.getBoolean("items.settings.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid settings material: " + materialName);
        }
    }

    private void addCloseItem(Inventory inventory, Trap trap, Player player) {
        int slot = menuConfig.getInt("items.close.slot", 26);
        String materialName = menuConfig.getString("items.close.material", "BARRIER");
        String name = ColorUtils.colorize(menuConfig.getString("items.close.name", "&c&lMENÜYÜ KAPAT"));
        
        List<String> lore = menuConfig.getStringList("items.close.lore");
        List<String> coloredLore = lore.stream()
            .map(ColorUtils::colorize)
            .collect(Collectors.toList());
        
        boolean enchanted = menuConfig.getBoolean("items.close.enchanted", false);
        
        try {
            Material material = Material.valueOf(materialName);
            ItemStack item = createItem(material, name, coloredLore, enchanted);
            inventory.setItem(slot, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid close material: " + materialName);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore, boolean enchanted) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            if (enchanted) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private void startAutoRefresh(Player player, Inventory inventory, Trap trap) {
        // Auto-refresh functionality disabled for performance
        // Can be enabled later if needed
    }

    public void closeMenu(Player player) {
        openMenus.remove(player);
        
        // Play close sound
        String soundName = menuConfig.getString("sounds.close", "BLOCK_CHEST_CLOSE");
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Sound not found, ignore
        }
    }

    public void handleClick(Player player, int slot) {
        Trap trap = openMenus.get(player);
        if (trap == null) {
            // Try to find trap from player's town
            List<Trap> playerTraps = getPlayerTownTraps(player);
            if (!playerTraps.isEmpty()) {
                trap = playerTraps.get(0);
                openMenus.put(player, trap);
            } else {
                player.sendMessage(ColorUtils.colorize("&cMenü için trap bulunamadı!"));
                player.closeInventory();
                return;
            }
        }
        
        // Play click sound
        String soundName = menuConfig.getString("sounds.click", "ENTITY_UI_BUTTON_CLICK");
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Sound not found, ignore
        }
        
        // Check which item was clicked
        ConfigurationSection itemsSection = menuConfig.getConfigurationSection("items");
        if (itemsSection == null) return;
        
        for (String itemKey : itemsSection.getKeys(false)) {
            int itemSlot = menuConfig.getInt("items." + itemKey + ".slot", -1);
            if (itemSlot == slot) {
                handleItemClick(player, trap, itemKey);
                break;
            }
        }
    }

    private void handleItemClick(Player player, Trap trap, String itemKey) {
        switch (itemKey) {
            case "pvp_status":
                // Toggle PvP
                trap.setPvpEnabled(!trap.isPvpEnabled());
                plugin.getTrapManager().saveTraps();
                
                String status = trap.isPvpEnabled() ? "Açık" : "Kapalı";
                String message = menuConfig.getString("messages.pvp-toggled", "&aPVP durumu %status% olarak değiştirildi!")
                    .replace("%status%", status);
                player.sendMessage(ColorUtils.colorize(message));
                break;
                
            case "teleport":
                // Teleport to trap
                if (trap.getCenter() != null) {
                    player.teleport(trap.getCenter());
                    String teleportMsg = menuConfig.getString("messages.teleporting", "&dTrap'a ışınlanıyor...");
                    player.sendMessage(ColorUtils.colorize(teleportMsg));
                    
                    String teleportSound = menuConfig.getString("sounds.teleport", "ENTITY_ENDERMAN_TELEPORT");
                    try {
                        Sound sound = Sound.valueOf(teleportSound);
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    } catch (IllegalArgumentException e) {
                        // Sound not found, ignore
                    }
                }
                break;
                
            case "close":
                // Close menu
                player.closeInventory();
                break;
                
            case "settings":
                // Open settings menu (could be implemented later)
                player.sendMessage(ColorUtils.colorize("&eAyarlar menüsü yakında eklenecek!"));
                break;
        }
    }

    public Trap getTrapForPlayer(Player player) {
        return openMenus.get(player);
    }
    
    /**
     * Check if player has permission to access trap menu
     */
    public boolean hasPermission(Player player) {
        return player.hasPermission("trap.menu") || player.hasPermission("trap.admin");
    }
    
    /**
     * Get player's town traps for menu access
     */
    public List<Trap> getPlayerTownTraps(Player player) {
        com.palmergames.bukkit.towny.TownyAPI townyAPI = com.palmergames.bukkit.towny.TownyAPI.getInstance();
        com.palmergames.bukkit.towny.object.Resident resident = townyAPI.getResident(player);
        
        if (resident == null || !resident.hasTown()) {
            return new ArrayList<>();
        }
        
        com.palmergames.bukkit.towny.object.Town town = resident.getTownOrNull();
        if (town == null) {
            return new ArrayList<>();
        }
        
        return plugin.getTrapManager().getAllTraps().stream()
            .filter(trap -> town.getName().equals(trap.getOwnerTownName()))
            .collect(Collectors.toList());
    }
    
    /**
     * Handle inventory close event
     */
    public void onInventoryClose(Player player) {
        closeMenu(player);
    }

    public void reloadMenuConfig() {
        loadMenuConfig();
        plugin.getLogger().info("Trap menu configuration reloaded!");
    }
    
    public String getMenuTitle() {
        return ColorUtils.colorize(menuConfig.getString("menu.title", "&6&lTRAP MENÜSÜ"));
    }
}
