package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.TrapStats;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TrapLevelManager {
    private final TrapSystem plugin;
    private final Map<Integer, Map<String, Object>> levelConfig;
    private final Map<Integer, TrapStats> trapStatsMap;
    private final File statsFile;
    private FileConfiguration statsConfig;

    public TrapLevelManager(TrapSystem plugin) {
        this.plugin = plugin;
        this.levelConfig = new HashMap<>();
        this.trapStatsMap = new HashMap<>();
        
        // Load levels configuration
        loadLevelsConfig();
        
        // Initialize stats file
        this.statsFile = new File(plugin.getDataFolder(), "trap_stats.yml");
        loadStats();
        
        // Auto-save task
        startAutoSaveTask();
    }
    
    private void loadLevelsConfig() {
        File configFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!configFile.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelStr : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelStr);
                    levelConfig.put(level, levelsSection.getConfigurationSection(levelStr).getValues(true));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid level in levels.yml: " + levelStr);
                }
            }
        }
    }
    
    private void loadStats() {
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
                statsConfig = new YamlConfiguration();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        // Load all trap stats
        if (statsConfig.contains("traps")) {
            ConfigurationSection trapsSection = statsConfig.getConfigurationSection("traps");
            if (trapsSection != null) {
                for (String trapId : trapsSection.getKeys(false)) {
                    try {
                        int id = Integer.parseInt(trapId);
                        Map<String, Object> data = trapsSection.getConfigurationSection(trapId).getValues(true);
                        trapStatsMap.put(id, TrapStats.deserialize(data));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load stats for trap " + trapId + ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    public void saveStats() {
        try {
            // Clear existing data
            for (String key : statsConfig.getKeys(false)) {
                statsConfig.set(key, null);
            }
            
            // Save all trap stats
            ConfigurationSection trapsSection = statsConfig.createSection("traps");
            for (Map.Entry<Integer, TrapStats> entry : trapStatsMap.entrySet()) {
                trapsSection.set(entry.getKey().toString(), entry.getValue().serialize());
            }
            
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save trap stats: " + e.getMessage());
        }
    }
    
    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveStats, 20 * 60 * 5, 20 * 60 * 5); // Save every 5 minutes
    }
    
    public TrapStats getTrapStats(int trapId) {
        return trapStatsMap.computeIfAbsent(trapId, k -> new TrapStats());
    }
    
    public void addExperience(int trapId, int amount, Player player) {
        TrapStats stats = getTrapStats(trapId);
        stats.addExperience(amount);
        
        // Check for level up
        checkLevelUp(trapId, stats, player);
    }
    
    private void checkLevelUp(int trapId, TrapStats stats, Player player) {
        int currentLevel = stats.getLevel();
        int nextLevel = currentLevel + 1;
        
        if (!levelConfig.containsKey(nextLevel)) {
            // Max level reached
            return;
        }
        
        int expNeeded = (int) levelConfig.get(nextLevel).getOrDefault("exp-required", 0);
        
        if (stats.getExperience() >= expNeeded) {
            // Level up!
            stats.setLevel(nextLevel);
            stats.setExperience(stats.getExperience() - expNeeded);
            
            // Notify player
            if (player != null && player.isOnline()) {
                player.sendTitle(
                    ColorUtils.colorize("&aSeviye Atladın!"),
                    ColorUtils.colorize("&eYeni Seviye: " + nextLevel),
                    10, 40, 10
                );
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            
            // Check for multi-level up
            checkLevelUp(trapId, stats, player);
        }
    }
    
    public boolean isTrapForSale(int trapId) {
        return getTrapStats(trapId).isForSale();
    }
    
    public void setTrapForSale(int trapId, double price, String sellingTown) {
        getTrapStats(trapId).setForSale(price, sellingTown);
    }
    
    public void cancelTrapSale(int trapId) {
        getTrapStats(trapId).removeFromSale();
    }
    
    public List<Map.Entry<Integer, TrapStats>> getTrapsForSale() {
        return trapStatsMap.entrySet().stream()
                .filter(entry -> entry.getValue().isForSale())
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getLevelInfo(int level) {
        return levelConfig.getOrDefault(level, new HashMap<>());
    }
    
    public int getMaxLevel() {
        return levelConfig.keySet().stream().max(Integer::compareTo).orElse(1);
    }
    
    public int getExpForNextLevel(int currentLevel) {
        if (currentLevel >= getMaxLevel()) {
            return 0;
        }
        return (int) levelConfig.getOrDefault(currentLevel + 1, Collections.singletonMap("exp-required", 0)).get("exp-required");
    }
    
    public void onDisable() {
        saveStats();
    }
}
