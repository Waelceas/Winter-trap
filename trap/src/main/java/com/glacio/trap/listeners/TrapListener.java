package com.glacio.trap.listeners;

import com.glacio.trap.TrapSystem;
import com.glacio.trap.models.Trap;
import com.glacio.trap.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class TrapListener implements Listener {
    private final TrapSystem plugin;

    public TrapListener(TrapSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWand(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null || event.getItem().getType() != Material.BLAZE_ROD) return;
        if (event.getItem().getItemMeta() == null || !event.getItem().getItemMeta().hasDisplayName()) return;
        if (!player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) return;

        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) return;

        UUID uuid = player.getUniqueId();
        Location[] locs = plugin.getSelections().computeIfAbsent(uuid, k -> new Location[2]);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            locs[0] = block.getLocation();
            player.sendMessage(ColorUtils.getMsg("pos1-selected"));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            locs[1] = block.getLocation();
            player.sendMessage(ColorUtils.getMsg("pos2-selected"));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Trap trap = plugin.getTrapManager().getTrapAt(event.getBlock().getLocation());
        if (trap == null) return; // Trap dışındaki alanlar serbest
        if (!checkAccess(event.getPlayer(), trap, Trap.TrapRole.MOD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Trap trap = plugin.getTrapManager().getTrapAt(event.getBlock().getLocation());
        if (trap == null) return; // Trap dışındaki alanlar serbest
        if (!checkAccess(event.getPlayer(), trap, Trap.TrapRole.MOD)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Material type = event.getClickedBlock().getType();
        if (type.name().contains("DOOR") || type.name().contains("FENCE_GATE") || type.name().contains("TRAPDOOR")) {
            Trap trap = plugin.getTrapManager().getTrapAt(event.getClickedBlock().getLocation());
            if (trap == null) return; // Trap dışındaki alanlar serbest
            if (!checkAccess(event.getPlayer(), trap, Trap.TrapRole.MEMBER)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;
        Trap trap = plugin.getTrapManager().getTrapAt(victim.getLocation());
        if (trap == null) return; // Trap dışındaki PvP serbest
        if (!trap.isPvpEnabled()) {
            event.setCancelled(true);
            damager.sendMessage(ColorUtils.getMsg("pvp-disabled-here"));
        } else {
            // Award experience for damage dealt in trap
            double damage = event.getDamage();
            plugin.getTrapLevelManager().addExperience(trap.getId(), (int) damage, damager);
            
            // Update trap stats
            com.glacio.trap.models.TrapStats stats = plugin.getTrapLevelManager().getTrapStats(trap.getId());
            stats.addDamageDealt(damage);
            stats.setLastDamager(damager.getUniqueId());
            
            // Check for kill and award bonus experience
            if (victim.getHealth() - damage <= 0) {
                stats.incrementKills();
                // Award bonus experience for player capture
                int captureExp = plugin.getTrapLevelManager().getExpForNextLevel(1) / 10; // 10% of first level requirement
                plugin.getTrapLevelManager().addExperience(trap.getId(), captureExp, damager);
                stats.incrementPlayerCaptures();
            }
        }
    }

    private boolean checkAccess(Player player, Trap trap, Trap.TrapRole required) {
        if (player.hasPermission(plugin.getConfig().getString("admin-permission", "admin.trap"))) return true;
        if (trap.getOwnerTownName() == null) return false;
        Trap.TrapRole playerRole = trap.getMembers().get(player.getUniqueId());
        if (playerRole == null) return false;
        return playerRole.ordinal() >= required.ordinal();
    }
}
