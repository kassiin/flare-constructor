package net.kassin.flareconstructor.listener;

import net.kassin.flareconstructor.FlareConstructorPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BuildJoinListener implements Listener {

    private final FlareConstructorPlugin plugin;
    private static final double VISUAL_RADIUS_SQ = 100.0 * 100.0;

    public BuildJoinListener(FlareConstructorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

    }

}
