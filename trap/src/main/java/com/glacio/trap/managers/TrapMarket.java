package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.models.TrapStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TrapMarket - Merkezi tuzak havuzu sistemi
 * Oyuncular tuzaklarını sattığında bu havuza eklenir,
 * diğer oyuncular bu havuzdan tuzak satın alabilir.
 */
public class TrapMarket {
    private final TrapSystem plugin;
    private final File marketFile;
    private FileConfiguration marketConfig;
    
    // Havuzdaki tuzaklar: trapId -> MarketEntry
    private final Map<Integer, MarketEntry> marketTraps;
    
    public TrapMarket(TrapSystem plugin) {
        this.plugin = plugin;
        this.marketFile = new File(plugin.getDataFolder(), "trapmarket.yml");
        this.marketTraps = new ConcurrentHashMap<>();
        
        loadMarket();
    }
    
    /**
     * Tuzak havuza ekler
     */
    public boolean addToMarket(int trapId, double price, String sellerTown) {
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) return false;
        
        // Tuzak sahipsiz olmalı (satış için)
        if (trap.getOwnerTownName() != null) return false;
        
        MarketEntry entry = new MarketEntry(trapId, price, sellerTown, System.currentTimeMillis());
        marketTraps.put(trapId, entry);
        saveMarket();
        
        plugin.getLogger().info("Trap #" + trapId + " added to market for " + price + " by " + sellerTown);
        return true;
    }
    
    /**
     * Tuzak havuzdan satın alır
     */
    public boolean buyFromMarket(int trapId, String buyerTown) {
        MarketEntry entry = marketTraps.get(trapId);
        if (entry == null) return false;
        
        Trap trap = plugin.getTrapManager().getTrapById(trapId);
        if (trap == null) return false;
        
        // Tuzak sahibini değiştir
        trap.setOwnerTownName(buyerTown);
        trap.setPurchaseDate(System.currentTimeMillis());
        trap.getMembers().clear();
        plugin.getTrapManager().saveTraps();
        
        // Havuzdan kaldır
        marketTraps.remove(trapId);
        saveMarket();
        
        plugin.getLogger().info("Trap #" + trapId + " bought from market by " + buyerTown);
        return true;
    }
    
    /**
     * Havuzdaki tuzakları listeler
     */
    public List<MarketEntry> getMarketTraps() {
        return new ArrayList<>(marketTraps.values());
    }
    
    /**
     * Tuzak havuzda mı?
     */
    public boolean isInMarket(int trapId) {
        return marketTraps.containsKey(trapId);
    }
    
    /**
     * Tuzak market bilgisini al
     */
    public MarketEntry getMarketEntry(int trapId) {
        return marketTraps.get(trapId);
    }
    
    /**
     * Market verilerini yükler
     */
    private void loadMarket() {
        if (!marketFile.exists()) {
            try {
                marketFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create trapmarket.yml file!");
                return;
            }
        }
        
        marketConfig = YamlConfiguration.loadConfiguration(marketFile);
        marketTraps.clear();
        
        // Market tuzaklarını yükle
        if (marketConfig.contains("market")) {
            for (String key : marketConfig.getConfigurationSection("market").getKeys(false)) {
                try {
                    int trapId = Integer.parseInt(key);
                    double price = marketConfig.getDouble("market." + key + ".price");
                    String sellerTown = marketConfig.getString("market." + key + ".seller");
                    long listedDate = marketConfig.getLong("market." + key + ".listedDate");
                    
                    MarketEntry entry = new MarketEntry(trapId, price, sellerTown, listedDate);
                    marketTraps.put(trapId, entry);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load market entry for trap: " + key);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + marketTraps.size() + " traps from market");
    }
    
    /**
     * Market verilerini kaydeder
     */
    private void saveMarket() {
        marketConfig.set("market", null); // Temizle
        
        for (Map.Entry<Integer, MarketEntry> entry : marketTraps.entrySet()) {
            String path = "market." + entry.getKey();
            marketConfig.set(path + ".price", entry.getValue().getPrice());
            marketConfig.set(path + ".seller", entry.getValue().getSellerTown());
            marketConfig.set(path + ".listedDate", entry.getValue().getListedDate());
        }
        
        try {
            marketConfig.save(marketFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save trapmarket.yml file!");
        }
    }
    
    /**
     * Market girişi sınıfı
     */
    public static class MarketEntry {
        private final int trapId;
        private final double price;
        private final String sellerTown;
        private final long listedDate;
        
        public MarketEntry(int trapId, double price, String sellerTown, long listedDate) {
            this.trapId = trapId;
            this.price = price;
            this.sellerTown = sellerTown;
            this.listedDate = listedDate;
        }
        
        public int getTrapId() { return trapId; }
        public double getPrice() { return price; }
        public String getSellerTown() { return sellerTown; }
        public long getListedDate() { return listedDate; }
        
        /**
         * Tuzak seviyesine göre fiyat hesaplar
         */
        public static double calculateMarketPrice(TrapSystem plugin, int trapId) {
            TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trapId);
            if (stats == null) return 5000.0; // Varsayılan fiyat
            
            int level = stats.getLevel();
            int kills = stats.getTotalKills();
            
            // Config'den fiyatları al
            double basePrice = plugin.getConfig().getDouble("market.base-price", 5000.0);
            double levelBonus = plugin.getConfig().getDouble("market.level-bonus", 2000.0);
            double killBonus = plugin.getConfig().getDouble("market.kill-bonus", 100.0);
            
            return basePrice + (level * levelBonus) + (kills * killBonus);
        }
    }
}
