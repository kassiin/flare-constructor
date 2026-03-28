package net.kassin.flareconstructor.protection;

import org.bukkit.Location;

/**
 * Representa os limites matemáticos de um canteiro de obras ativo.
 */
public record WorksiteBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public boolean contains(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        return x >= minX && x <= maxX && 
               y >= minY && y <= maxY && 
               z >= minZ && z <= maxZ;
    }
}