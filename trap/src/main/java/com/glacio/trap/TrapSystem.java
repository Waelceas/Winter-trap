package com.glacio.trap;

import com.glacio.trap.commands.TrapCommand;
import com.glacio.trap.commands.TrapShopCommand;
import com.glacio.trap.listeners.TrapListener;
import com.glacio.trap.managers.TrapLevelManager;
import com.glacio.trap.managers.TrapManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapSystem extends JavaPlugin {
    private static TrapSystem instance;
    private TrapManager trapManager;
    private TrapLevelManager trapLevelManager;
    private Economy econ = null;
    private final Map<UUID, Location[]> selections = new HashMap<>();
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        createLangConfig();
        
        if (!setupEconomy()) {
            getLogger().severe("Vault bulunamadı! Eklenti kapatılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        this.trapManager = new TrapManager(this);
        this.trapLevelManager = new TrapLevelManager(this);
        
        // Register commands
        if (getCommand("trap") != null) {
            getCommand("trap").setExecutor(new TrapCommand(this));
        }
        if (getCommand("trap-editor") != null) {
            getCommand("trap-editor").setExecutor(new TrapCommand(this));
        }
        if (getCommand("trapshop") != null) {
            getCommand("trapshop").setExecutor(new TrapShopCommand(this));
        }
        
        getServer().getPluginManager().registerEvents(new TrapListener(this), this);

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

    public FileConfiguration getLang() {
        return langConfig;
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
    public TrapLevelManager getTrapLevelManager() { return trapLevelManager; }
    public Economy getEconomy() { return econ; }
    public Map<UUID, Location[]> getSelections() { return selections; }
}
