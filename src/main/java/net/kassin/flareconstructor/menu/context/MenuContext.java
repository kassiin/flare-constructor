package net.kassin.flareconstructor.menu.context;

import net.kassin.flareconstructor.menu.configuration.BuildData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record MenuContext(Player player, String buildId, Location benchLocation, BuildData data) {

    public static MenuContext create(Player player, Location benchLocation) {
        return new MenuContext(player, null, benchLocation, null);
    }

    public static MenuContext create(Player player, String buildId, Location benchLocation) {
        return new MenuContext(player, buildId, benchLocation, null);
    }

    public static MenuContext create(Player player, BuildData data) {
        return new MenuContext(player, data.id(), data.benchLocation(), data);
    }

}