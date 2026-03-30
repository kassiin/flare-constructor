package net.kassin.flareconstructor.listener;

import net.kassin.flareconstructor.protection.WorksiteTracker;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public record WorksiteProtectionListener(WorksiteTracker worksiteTracker) implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (worksiteTracker.isInsideActiveWorksite(event.getBlock().getLocation())) {
            if (event.getPlayer().hasPermission("flareconstructor.admin")) return;

            event.setCancelled(true);
            event.getPlayer().sendMessage("§c🚧 Você não pode destruir blocos em um canteiro de obras ativo!");
        }

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (worksiteTracker.isInsideActiveWorksite(event.getBlock().getLocation())) {

            if (event.getPlayer().hasPermission("flareconstructor.admin")) return;

            event.setCancelled(true);
            event.getPlayer().sendMessage("§c🚧 Você não pode construir em um canteiro de obras ativo!");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (worksiteTracker.isInsideActiveWorksite(to)) {
            Player player = event.getPlayer();
            player.sendActionBar(Component.text("§e🚧 Canteiro de Obras Ativo 🚧"));
        }

    }

}