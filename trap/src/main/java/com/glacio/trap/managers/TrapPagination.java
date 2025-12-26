package com.glacio.trap.managers;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TrapPagination {
    
    public static class PageState {
        private final int currentPage;
        private final int totalPages;
        private final int totalItems;
        
        public PageState(int currentPage, int totalPages, int totalItems) {
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
        }
        
        public int getCurrentPage() { return currentPage; }
        public int getTotalPages() { return totalPages; }
        public int getTotalItems() { return totalItems; }
        public boolean hasNextPage() { return currentPage < totalPages - 1; }
        public boolean hasPreviousPage() { return currentPage > 0; }
    }
    
    public static final int ITEMS_PER_PAGE = 28;
    private static final int[] VALID_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,  // Row 2
        19, 20, 21, 22, 23, 24, 25,  // Row 3
        28, 29, 30, 31, 32, 33, 34,  // Row 4
        37, 38, 39, 40, 41, 42, 43   // Row 5
    };
    
    public static PageState createPageState(int totalItems, int currentPage) {
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        return new PageState(currentPage, totalPages, totalItems);
    }
    
    public static void addPageNavigation(Inventory inventory, PageState pageState, Map<String, Object> viewData) {
        if (pageState.hasPreviousPage()) {
            List<String> prevLore = Arrays.asList(
                "§7Sayfa " + (pageState.getCurrentPage() + 1) + " -> " + pageState.getCurrentPage(),
                "§eÖnceki sayfaya git"
            );
            inventory.setItem(48, TrapShopGUI.createGuiItem(Material.ARROW, "§e§lÖnceki Sayfa", prevLore, false));
        }
        
        if (pageState.hasNextPage()) {
            List<String> nextLore = Arrays.asList(
                "§7Sayfa " + (pageState.getCurrentPage() + 1) + " -> " + (pageState.getCurrentPage() + 2),
                "§eSonraki sayfaya git"
            );
            inventory.setItem(50, TrapShopGUI.createGuiItem(Material.ARROW, "§e§lSonraki Sayfa", nextLore, false));
        }
        
        // Sayfa bilgisi
        List<String> infoLore = Arrays.asList(
            "§7Toplam " + pageState.getTotalItems() + " tuzak",
            "§7Sayfa " + (pageState.getCurrentPage() + 1) + "/" + pageState.getTotalPages()
        );
        inventory.setItem(49, TrapShopGUI.createGuiItem(Material.PAPER, "§e§lSayfa Bilgisi", infoLore, false));
    }
    
    public static int getStartIndex(int page) {
        return page * ITEMS_PER_PAGE;
    }
    
    public static int getEndIndex(int startIndex, int totalItems) {
        return Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
    }
    
    public static int getSlotForIndex(int itemIndex) {
        if (itemIndex >= 0 && itemIndex < VALID_SLOTS.length) {
            return VALID_SLOTS[itemIndex];
        }
        return -1; // Geçersiz slot
    }
    
    public static boolean isValidNavigationSlot(int slot) {
        return slot == 48 || slot == 49 || slot == 50; // Önceki, Bilgi, Sonraki
    }
    
    public static NavigationAction getNavigationAction(int slot) {
        switch (slot) {
            case 48: return NavigationAction.PREVIOUS;
            case 49: return NavigationAction.INFO;
            case 50: return NavigationAction.NEXT;
            default: return NavigationAction.NONE;
        }
    }
    
    public enum NavigationAction {
        PREVIOUS, NEXT, INFO, NONE
    }
}
