package net.kassin.flareconstructor.utils;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class EntityPlacementValidator {

    public static boolean isAreaClear(Block clickedBlock, Player player) {
        double originX = clickedBlock.getX() + 0.5;
        double originY = clickedBlock.getY() + 1.0;
        double originZ = clickedBlock.getZ() + 0.5;

        BlockFace facing = getCardinalDirection(player);

        double minY = originY;
        double maxY = originY + 2.0;

        double minX = originX, maxX = originX;
        double minZ = originZ, maxZ = originZ;

        if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
            minX -= 1.0;
            maxX += 1.0;
            
            if (facing == BlockFace.NORTH) {
                maxZ += 1.0; 
            } else {
                minZ -= 1.0;
            }
        } 
        else if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
            minZ -= 1.0;
            maxZ += 1.0;

            if (facing == BlockFace.EAST) {
                minX -= 1.0;
            } else {
                maxX += 1.0;
            }
        }

        BoundingBox entityBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        int bMinX = (int) Math.floor(entityBox.getMinX());
        int bMaxX = (int) Math.floor(entityBox.getMaxX() - 0.0001);
        int bMinY = (int) Math.floor(entityBox.getMinY());
        int bMaxY = (int) Math.floor(entityBox.getMaxY() - 0.0001);
        int bMinZ = (int) Math.floor(entityBox.getMinZ());
        int bMaxZ = (int) Math.floor(entityBox.getMaxZ() - 0.0001);

        org.bukkit.World world = clickedBlock.getWorld();

        for (int x = bMinX; x <= bMaxX; x++) {
            for (int y = bMinY; y <= bMaxY; y++) {
                for (int z = bMinZ; z <= bMaxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    
                    if (!block.isPassable()) {
                        return false; 
                    }
                }
            }
        }

        return true;
    }

    private static BlockFace getCardinalDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        yaw %= 360;
        
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }
}