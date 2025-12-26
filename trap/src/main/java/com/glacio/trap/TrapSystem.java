package com.glacio.trap;

import com.glacio.trap.commands.TrapCommand;
import com.glacio.trap.commands.TrapAdminCommand;
import com.glacio.trap.commands.TrapShopCommand;
import com.glacio.trap.commands.TrapTabCompleter;
import com.glacio.trap.listeners.TrapListener;
import com.glacio.trap.listeners.MenuListener;
import com.glacio.trap.listeners.ShopListener;
import com.glacio.trap.managers.TrapLevelManager;
import com.glacio.trap.managers.TrapManager;
import com.glacio.trap.managers.TrapMenuManager;
import com.glacio.trap.managers.TrapShopManager;
import com.glacio.trap.managers.ManagerRegistry;
import com.glacio.trap.models.Trap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapSystem extends JavaPlugin {
    private static TrapSystem instance;
    private TrapManager trapManager;
    private TrapLevelManager trapLevelManager;
    private TrapMenuManager trapMenuManager;
    private TrapShopManager trapShopManager;
    private ManagerRegistry managerRegistry;
    private Economy econ = null;
    private final Map<UUID, Location[]> selections = new HashMap<>();
    private FileConfiguration langConfig;
    private FileConfiguration commandsConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        createLangConfig();
        createCommandsConfig();
        
        // Wait a moment for plugins to fully load before checking for Vault
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().warning(" Vault economy servisi bulunamadı! Bazı özellikler çalışmayabilir.");
                getLogger().warning(" Lütfen Vault'un kurulu ve bir economy plugin'inin olduğundan emin olun.");
                // Don't disable the plugin, just warn
            }
        }, 20L); // 1 tick delay

        // Initialize managers
        this.trapManager = new TrapManager(this);
        this.trapLevelManager = new TrapLevelManager(this);
        this.trapMenuManager = new TrapMenuManager(this);
        this.trapShopManager = new TrapShopManager(this);
        this.managerRegistry = new ManagerRegistry(this);
        
        // Register commands
        if (getCommand("trap") != null) {
            getCommand("trap").setExecutor(new TrapCommand(this));
            getCommand("trap").setTabCompleter(new TrapTabCompleter(this));
        }
        if (getCommand("trapadmin") != null) {
            getCommand("trapadmin").setExecutor(new TrapAdminCommand(this));
            getCommand("trapadmin").setTabCompleter(new TrapTabCompleter(this));
        }
        if (getCommand("trapshop") != null) {
            getCommand("trapshop").setExecutor(new TrapShopCommand(this));
        }
        
        getServer().getPluginManager().registerEvents(new TrapListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        long taxInterval = getConfig().getLong("tax-interval-minutes", 60) * 20 * 60;
        Bukkit.getScheduler().runTaskTimer(this, this::processTaxes, taxInterval, taxInterval);

        getLogger().info("winter-trap V0.1103 aktif edildi!");
    }

    @Override
    public void onDisable() {
        // Save all trap data before shutdown
        if (trapLevelManager != null) {
            trapLevelManager.onDisable();
        }
        if (trapManager != null) {
            trapManager.saveTraps();
        }
    }

    private void createLangConfig() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void createCommandsConfig() {
        File commandsFile = new File(getDataFolder(), "commands.yml");
        if (!commandsFile.exists()) {
            saveResource("commands.yml", false);
        }
        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
    }

    public FileConfiguration getLang() {
        return langConfig;
    }

    public FileConfiguration getCommandsConfig() {
        return commandsConfig;
    }

    public void reloadConfigs() {
        reloadConfig();
        createLangConfig();
        createCommandsConfig();
        
        // Reload level configuration
        if (trapLevelManager != null) {
            trapLevelManager.reloadLevelsConfig();
        }
        
        // Reload menu configuration
        if (trapMenuManager != null) {
            trapMenuManager.reloadMenuConfig();
        }
        
        // Reload shop configuration
        if (trapShopManager != null) {
            trapShopManager.reloadShopConfig();
        }
        
        // Refresh town data with delay
        int delay = commandsConfig.getInt("settings.town-detection-delay", 100);
        getServer().getScheduler().runTaskLater(this, () -> {
            refreshTownData();
            getLogger().info("Towny verileri yenilendi - yeni şehirler algılanabilir.");
        }, delay);
    }
    
    private void refreshTownData() {
        // Refresh all trap town associations
        if (this.getTrapManager() != null) {
            for (Trap trap : this.getTrapManager().getAllTraps()) {
                if (trap.getOwnerTownName() != null) {
                    // Verify town still exists
                    com.palmergames.bukkit.towny.object.Town town = 
                        com.palmergames.bukkit.towny.TownyAPI.getInstance().getTown(trap.getOwnerTownName());
                    if (town == null) {
                        // Town no longer exists, clear ownership
                        trap.setOwnerTownName(null);
                        trap.getMembers().clear();
                        getLogger().info("Trap " + trap.getId() + " sahipsiz kaldı (şehir silindi): " + trap.getOwnerTownName());
                    }
                }
            }
            this.getTrapManager().saveTraps();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void processTaxes() {
        double taxAmount = getConfig().getDouble("tax-amount", 100.0);
        trapManager.getAllTraps().forEach(trap -> {
            if (trap.getOwnerTownName() != null) {
                com.palmergames.bukkit.towny.object.Town town = 
                    com.palmergames.bukkit.towny.TownyAPI.getInstance().getTown(trap.getOwnerTownName());
                if (town != null && town.getAccount() != null) {
                    if (town.getAccount().getHoldingBalance() >= taxAmount) {
                        town.getAccount().withdraw(taxAmount, "Trap Vergisi");
                    } else {
                        trap.setOwnerTownName(null);
                        trap.getMembers().clear();
                        trapManager.save();
                        getLogger().info(town.getName() + " vergiyi ödeyemediği için trap " + trap.getId() + " boşa düştü.");
                    }
                }
            }
        });
    }

    public static TrapSystem getInstance() { return instance; }
    public TrapManager getTrapManager() { return trapManager; }
    public ManagerRegistry getManagerRegistry() { return managerRegistry; }
    public TrapLevelManager getTrapLevelManager() { return trapLevelManager; }
    public TrapMenuManager getMenuManager() { return trapMenuManager; }
    public TrapShopManager getShopManager() { return trapShopManager; }
    public Economy getEconomy() { return econ; }
    public Map<UUID, Location[]> getSelections() { return selections; }
}
