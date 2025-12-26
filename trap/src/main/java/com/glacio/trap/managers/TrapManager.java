package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TrapManager {
    private final TrapSystem plugin;
    private final Map<Integer, Trap> traps = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public TrapManager(TrapSystem plugin) {
        this.plugin = plugin;
        load();
    }

    public void addTrap(int id, Location p1, Location p2) {
        traps.put(id, new Trap(id, p1, p2));
        save();
    }

    public void removeTrap(int id) {
        traps.remove(id);
        save();
    }

    public Trap getTrap(int id) {
        return traps.get(id);
    }

    public Trap getTrapAt(Location loc) {
        for (Trap trap : traps.values()) {
            if (trap.isInside(loc)) return trap;
        }
        return null;
    }

    public Collection<Trap> getAllTraps() {
        return traps.values();
    }

    public boolean doesTrapExist(int id) {
        return traps.containsKey(id);
    }
    
    /**
     * Checks if a trap is owned by a specific town
     * @param trapId The ID of the trap to check
     * @param townUuid The UUID of the town to check ownership against
     * @return true if the trap exists and is owned by the specified town, false otherwise
     */
    public boolean isTrapOwnedByTown(int trapId, UUID townUuid) {
        Trap trap = traps.get(trapId);
        if (trap == null) {
            return false;
        }
        
        // Get the town name from the trap
        String townName = trap.getOwnerTownName();
        if (townName == null || townUuid == null) {
            return false;
        }
        
        // Get the town by UUID and check if the names match
        try {
            com.palmergames.bukkit.towny.object.Town town = 
                com.palmergames.bukkit.towny.TownyAPI.getInstance().getTown(townUuid);
            return town != null && townName.equalsIgnoreCase(town.getName());
        } catch (Exception e) {
            return false;
        }
    }

    public Trap getTrapById(int id) {
        return traps.get(id);
    }

    public List<Trap> getTrapByTown(String townName) {
        return traps.values().stream()
                .filter(trap -> townName.equals(trap.getOwnerTownName()))
                .collect(Collectors.toList());
    }

    public Set<Integer> getTrapIds() {
        return traps.keySet();
    }

    public void saveTraps() {
        save();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "traps.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        traps.clear();

        ConfigurationSection section = config.getConfigurationSection("traps");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int id = Integer.parseInt(key);
            Location p1 = config.getLocation("traps." + key + ".p1");
            Location p2 = config.getLocation("traps." + key + ".p2");
            Trap trap = new Trap(id, p1, p2);
            trap.setOwnerTownName(config.getString("traps." + key + ".owner"));
            trap.setPvpEnabled(config.getBoolean("traps." + key + ".pvp"));
            
            ConfigurationSection membersSection = config.getConfigurationSection("traps." + key + ".members");
            if (membersSection != null) {
                for (String uuidStr : membersSection.getKeys(false)) {
                    trap.getMembers().put(UUID.fromString(uuidStr), Trap.TrapRole.valueOf(config.getString("traps." + key + ".members." + uuidStr)));
                }
            }
            traps.put(id, trap);
        }
    }

    public void save() {
        config.set("traps", null);
        for (Trap trap : traps.values()) {
            String path = "traps." + trap.getId();
            config.set(path + ".p1", trap.getPos1());
            config.set(path + ".p2", trap.getPos2());
            config.set(path + ".owner", trap.getOwnerTownName());
            config.set(path + ".pvp", trap.isPvpEnabled());
            for (Map.Entry<UUID, Trap.TrapRole> entry : trap.getMembers().entrySet()) {
                config.set(path + ".members." + entry.getKey().toString(), entry.getValue().name());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
