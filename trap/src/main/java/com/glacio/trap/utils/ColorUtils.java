package com.glacio.trap.utils;

import com.glacio.trap.TrapSystem;
import org.bukkit.ChatColor;

public class ColorUtils {
    public static String format(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', "&l&o" + message);
    }

    public static String getMsg(String path) {
        String prefix = TrapSystem.getInstance().getLang().getString("prefix", "");
        String msg = TrapSystem.getInstance().getLang().getString(path, "Missing: " + path);
        return format(prefix + msg);
    }

    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String rainbow(String text) {
        StringBuilder rainbowText = new StringBuilder();
        ChatColor[] colors = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, 
            ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE
        };
        
        for (int i = 0; i < text.length(); i++) {
            rainbowText.append(colors[i % colors.length]).append(ChatColor.BOLD).append(ChatColor.ITALIC).append(text.charAt(i));
        }
        return rainbowText.toString();
    }
}
