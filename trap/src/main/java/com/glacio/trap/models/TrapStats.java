package com.glacio.trap.models;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapStats implements ConfigurationSerializable {
    private int level;
    private int experience;
    private int playerCaptures;
    private double totalDamageDealt;
    private int totalKills;
    private long lastLevelUpTime;
    private UUID lastDamager;
    private double salePrice;
    private String sellingTown;
    private long saleStartTime;

    public TrapStats() {
        this.level = 1;
        this.experience = 0;
        this.playerCaptures = 0;
        this.totalDamageDealt = 0;
        this.totalKills = 0;
        this.lastLevelUpTime = System.currentTimeMillis();
        this.salePrice = -1; // -1 means not for sale
    }

    // Getters and Setters
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        this.lastLevelUpTime = System.currentTimeMillis();
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public void addExperience(int amount) {
        this.experience += amount;
    }

    public int getPlayerCaptures() {
        return playerCaptures;
    }

    public void incrementPlayerCaptures() {
        this.playerCaptures++;
    }

    public double getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public void addDamageDealt(double amount) {
        this.totalDamageDealt += amount;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public void incrementKills() {
        this.totalKills++;
    }

    public long getLastLevelUpTime() {
        return lastLevelUpTime;
    }

    public UUID getLastDamager() {
        return lastDamager;
    }

    public void setLastDamager(UUID lastDamager) {
        this.lastDamager = lastDamager;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public void setForSale(double price, String sellingTown) {
        this.salePrice = price;
        this.sellingTown = sellingTown;
        this.saleStartTime = System.currentTimeMillis();
    }
    
    public void setSalePrice(double price) {
        this.salePrice = price;
    }

    public void removeFromSale() {
        this.salePrice = -1;
        this.sellingTown = null;
    }

    public boolean isForSale() {
        return salePrice > 0;
    }

    public String getSellingTown() {
        return sellingTown;
    }

    public long getSaleDuration() {
        return (System.currentTimeMillis() - saleStartTime) / 1000; // in seconds
    }

    // Serialization
    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("level", level);
        data.put("experience", experience);
        data.put("playerCaptures", playerCaptures);
        data.put("totalDamageDealt", totalDamageDealt);
        data.put("totalKills", totalKills);
        data.put("lastLevelUpTime", lastLevelUpTime);
        data.put("salePrice", salePrice);
        data.put("sellingTown", sellingTown);
        data.put("saleStartTime", saleStartTime);
        return data;
    }

    public static TrapStats deserialize(Map<String, Object> data) {
        TrapStats stats = new TrapStats();
        stats.level = (int) data.getOrDefault("level", 1);
        stats.experience = (int) data.getOrDefault("experience", 0);
        stats.playerCaptures = (int) data.getOrDefault("playerCaptures", 0);
        stats.totalDamageDealt = (double) data.getOrDefault("totalDamageDealt", 0.0);
        stats.totalKills = (int) data.getOrDefault("totalKills", 0);
        stats.lastLevelUpTime = (long) data.getOrDefault("lastLevelUpTime", System.currentTimeMillis());
        stats.salePrice = (double) data.getOrDefault("salePrice", -1.0);
        stats.sellingTown = (String) data.getOrDefault("sellingTown", null);
        stats.saleStartTime = (long) data.getOrDefault("saleStartTime", 0L);
        return stats;
    }
}
