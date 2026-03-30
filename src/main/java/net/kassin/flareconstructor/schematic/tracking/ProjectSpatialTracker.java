package net.kassin.flareconstructor.schematic.tracking;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;

import java.util.ArrayList;
import java.util.List;

public class ProjectSpatialTracker {

    private final Long2ObjectOpenHashMap<List<ConstructionProject>> chunkMap = new Long2ObjectOpenHashMap<>();
    private static final int VIEW_CHUNK_RADIUS = 4;

    public static long getChunkKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ & 0xFFFFFFFFL) << 32;
    }

    public void registerProject(ConstructionProject project, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int minChunkX = (minBlockX >> 4) - VIEW_CHUNK_RADIUS;
        int maxChunkX = (maxBlockX >> 4) + VIEW_CHUNK_RADIUS;
        int minChunkZ = (minBlockZ >> 4) - VIEW_CHUNK_RADIUS;
        int maxChunkZ = (maxBlockZ >> 4) + VIEW_CHUNK_RADIUS;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long key = getChunkKey(cx, cz);
                chunkMap.computeIfAbsent(key, k -> new ArrayList<>()).add(project);
            }
        }
    }

    public void unregisterProject(ConstructionProject project, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int minChunkX = (minBlockX >> 4) - VIEW_CHUNK_RADIUS;
        int maxChunkX = (maxBlockX >> 4) + VIEW_CHUNK_RADIUS;
        int minChunkZ = (minBlockZ >> 4) - VIEW_CHUNK_RADIUS;
        int maxChunkZ = (maxBlockZ >> 4) + VIEW_CHUNK_RADIUS;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long key = getChunkKey(cx, cz);
                List<ConstructionProject> projects = chunkMap.get(key);
                if (projects != null) {
                    projects.remove(project);
                    if (projects.isEmpty()) chunkMap.remove(key);
                }
            }
        }
    }

    public List<ConstructionProject> getProjectsInChunk(int chunkX, int chunkZ) {
        long key = getChunkKey(chunkX, chunkZ);
        return chunkMap.getOrDefault(key, List.of());
    }

}