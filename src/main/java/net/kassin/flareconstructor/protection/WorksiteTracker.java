package net.kassin.flareconstructor.protection;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorksiteTracker {

    private final Long2ObjectMap<List<WorksiteBounds>> chunkGrid = 
            Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    private long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    public void trackWorksite(WorksiteBounds bounds) {
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                long key = getChunkKey(x, z);
                chunkGrid.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(bounds);
            }
        }
    }

    public void untrackWorksite(WorksiteBounds bounds) {
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                long key = getChunkKey(x, z);
                
                List<WorksiteBounds> boundsList = chunkGrid.get(key);
                if (boundsList != null) {
                    boundsList.remove(bounds);
                    if (boundsList.isEmpty()) {
                        chunkGrid.remove(key); 
                    }
                }
            }
        }
    }

    public boolean isInsideActiveWorksite(Location loc) {
        long key = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        
        List<WorksiteBounds> boundsList = chunkGrid.get(key);
        
        if (boundsList == null) return false;

        for (WorksiteBounds bounds : boundsList) {
            if (bounds.contains(loc)) {
                return true;
            }
        }
        return false;
    }

}