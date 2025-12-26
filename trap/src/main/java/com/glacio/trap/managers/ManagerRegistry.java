package com.glacio.trap.managers;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.events.TrapSellEvent;
import com.glacio.trap.gui.*;
import com.glacio.trap.handlers.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ManagerRegistry - Projenin BAŞI
 * Tüm GUI ve Handler nesnelerini tek bir yerde yönetir
 * Zombi nesne problemini çözer
 * Herkese buradan dağıtım yapar
 */
public class ManagerRegistry {
    private final TrapSystem plugin;
    private final Map<Class<?>, Object> managers = new HashMap<>();
    
    // Önce Handler'ları oluştur (BEYİNLER)
    private final TrapShopSellHandler sellHandler;
    private final TrapShopBuyHandler buyHandler;
    private final TrapShopUpgradeHandler upgradeHandler;
    private final TrapShopMainMenuHandler mainMenuHandler;
    private final TrapShopActionHandler actionHandler;
    private final TrapMarket trapMarket;
    
    // Sonra GUI'leri oluştur ve Handler'ları içine gönder (GÖZLER)
    private final TrapSellGUI sellGUI;
    private final TrapBuyGUI buyGUI;
    private final TrapUpgradeGUI upgradeGUI;
    private final TrapMainMenuGUI mainMenuGUI;
    
    public ManagerRegistry(TrapSystem plugin) {
        this.plugin = plugin;
        
        // ÖNCE BEYİNLER (Handler'lar)
        this.sellHandler = new TrapShopSellHandler(plugin);
        this.buyHandler = new TrapShopBuyHandler(plugin);
        this.upgradeHandler = new TrapShopUpgradeHandler(plugin);
        this.mainMenuHandler = new TrapShopMainMenuHandler(plugin, new HashMap<>());
        this.actionHandler = new TrapShopActionHandler(plugin);
        this.trapMarket = new TrapMarket(plugin);
        
        // SONRA GÖZLER (GUI'ler)
        this.sellGUI = new TrapSellGUI(plugin, sellHandler);
        this.buyGUI = new TrapBuyGUI(plugin);
        this.upgradeGUI = new TrapUpgradeGUI(plugin, upgradeHandler);
        this.mainMenuGUI = new TrapMainMenuGUI(plugin);
        
        // Map'e de ekle (geniş erişim için)
        managers.put(TrapShopSellHandler.class, sellHandler);
        managers.put(TrapShopBuyHandler.class, buyHandler);
        managers.put(TrapShopUpgradeHandler.class, upgradeHandler);
        managers.put(TrapShopMainMenuHandler.class, mainMenuHandler);
        managers.put(TrapShopActionHandler.class, actionHandler);
        managers.put(TrapMarket.class, trapMarket);
        managers.put(TrapSellGUI.class, sellGUI);
        managers.put(TrapBuyGUI.class, buyGUI);
        managers.put(TrapUpgradeGUI.class, upgradeGUI);
        managers.put(TrapMainMenuGUI.class, mainMenuGUI);
    }
    
    /**
     * BEYİNLER (Handler'lar)
     */
    public TrapShopSellHandler getSellHandler() { return sellHandler; }
    public TrapShopBuyHandler getBuyHandler() { return buyHandler; }
    public TrapShopUpgradeHandler getUpgradeHandler() { return upgradeHandler; }
    public TrapShopMainMenuHandler getMainMenuHandler() { return mainMenuHandler; }
    public TrapShopActionHandler getActionHandler() { return actionHandler; }
    public TrapMarket getTrapMarket() { return trapMarket; }
    
    /**
     * GÖZLER (GUI'ler)
     */
    public TrapSellGUI getSellGUI() { return sellGUI; }
    public TrapBuyGUI getBuyGUI() { return buyGUI; }
    public TrapUpgradeGUI getUpgradeGUI() { return upgradeGUI; }
    public TrapMainMenuGUI getMainMenuGUI() { return mainMenuGUI; }
    
    /**
     * Geniş erişim için (isteğe bağlı)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        T manager = (T) managers.get(clazz);
        if (manager == null) {
            throw new IllegalArgumentException("Manager not found: " + clazz.getSimpleName());
        }
        return manager;
    }
}
