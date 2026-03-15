package net.kassin.flareconstructor.utils;

import org.bukkit.Location;

public final class LocationUtils {

    private LocationUtils() {}

    public enum Cardinal { NORTH, SOUTH, EAST, WEST }

    public static Cardinal getCardinal(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135)  return Cardinal.WEST;
        if (yaw >= 135 && yaw < 225) return Cardinal.NORTH;
        if (yaw >= 225 && yaw < 315) return Cardinal.EAST;
        return Cardinal.SOUTH;
    }

    public static Location getGridAlignedOrigin(Location benchLocation, int distanceBlocks) {
        Location start = benchLocation.getBlock().getLocation();
        Cardinal cardinal = getCardinal(benchLocation.getYaw());

        return switch (cardinal) {
            case WEST  -> start.clone().add(-distanceBlocks, 0, 0);
            case NORTH -> start.clone().add(0, 0, -distanceBlocks);
            case EAST  -> start.clone().add(distanceBlocks, 0, 0);
            case SOUTH -> start.clone().add(0, 0, distanceBlocks);
        };
    }
}