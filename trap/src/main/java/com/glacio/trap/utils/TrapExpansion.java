package com.glacio.trap.utils;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class TrapExpansion extends PlaceholderExpansion {
    private final TrapSystem plugin;

    public TrapExpansion(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "trap";
    }

    @Override
    public @NotNull String getAuthor() {
        return "glacio";
    }

    @Override
    public @NotNull String getVersion() {
        return "0.1101";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.equals("owner") || lower.equals("current_owner")) {
            Trap trap = plugin.getTrapManager().getTrapAt(player.getLocation());
            return getOwner(trap);
        }

        int underscoreIndex = lower.indexOf('_');
        if (underscoreIndex <= 0) return null;

        String idPart = lower.substring(0, underscoreIndex);
        String field = lower.substring(underscoreIndex + 1);

        int id;
        try {
            id = Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            return null;
        }

        Trap trap = plugin.getTrapManager().getTrap(id);

        return switch (field) {
            case "owner" -> getOwner(trap);
            case "status" -> getStatus(trap);
            default -> null;
        };
    }

    private String getOwner(Trap trap) {
        if (trap == null) return "Yok";
        String owner = trap.getOwnerTownName();
        if (owner == null) return "Yok";
        return owner;
    }

    private String getStatus(Trap trap) {
        if (trap == null) return "Yok";
        return trap.getOwnerTownName() == null ? "Satılık" : "Dolu";
    }
}
