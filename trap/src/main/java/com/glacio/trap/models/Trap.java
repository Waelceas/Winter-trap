package com.glacio.trap.models;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Trap {
    private final int id;
    private final Location pos1;
    private final Location pos2;
    private String ownerTownName;
    private boolean pvpEnabled;
    private Map<UUID, TrapRole> members;

    public enum TrapRole {
        MEMBER, MOD, ADMIN
    }

    public Trap(int id, Location pos1, Location pos2) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.ownerTownName = null;
        this.pvpEnabled = false;
        this.members = new HashMap<>();
    }

    public int getId() { return id; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public String getOwnerTownName() { return ownerTownName; }
    public void setOwnerTownName(String ownerTownName) { this.ownerTownName = ownerTownName; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public Map<UUID, TrapRole> getMembers() { return members; }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        
        double xMin = Math.min(pos1.getX(), pos2.getX());
        double xMax = Math.max(pos1.getX(), pos2.getX());
        double zMin = Math.min(pos1.getZ(), pos2.getZ());
        double zMax = Math.max(pos1.getZ(), pos2.getZ());

        // Y koordinatı kontrolü kaldırıldı, tüm dikey alanı kapsar.
        return loc.getX() >= xMin && loc.getX() <= xMax &&
               loc.getZ() >= zMin && loc.getZ() <= zMax;
    }
}
