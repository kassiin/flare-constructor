package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import net.kassin.flareconstructor.FlareConstructorPlugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SchematicAnalyzer {

    public record SchematicStats(int totalBlocks, int baseBlocks, int width, int height, int length) {

        public boolean validStats() {
            return (totalBlocks + baseBlocks + width + height + length) > 1;
        }

    }

    private final FlareConstructorPlugin plugin;
    private final SchematicLoader loader;

    // Agora o cache guarda o novo Record
    private final Map<String, SchematicStats> statsCache = new ConcurrentHashMap<>();

    public SchematicAnalyzer(FlareConstructorPlugin plugin, SchematicLoader loader) {
        this.plugin = plugin;
        this.loader = loader;
    }

    public SchematicStats getStats(String schematicId) {
        if (statsCache.containsKey(schematicId)) {
            return statsCache.get(schematicId);
        }

        try {
            Clipboard clipboard = loader.load(schematicId);
            int total = 0;
            int base = 0;

            com.sk89q.worldedit.regions.Region region = clipboard.getRegion();
            int minX = region.getMinimumPoint().x();
            int maxX = region.getMaximumPoint().x();
            int minZ = region.getMinimumPoint().z();
            int maxZ = region.getMaximumPoint().z();
            int minY = region.getMinimumPoint().y();
            int maxY = region.getMaximumPoint().y();

            int width = maxX - minX;
            int length = maxZ - minZ;
            int height = maxY - minY;

            for (BlockVector3 pt : region) {
                if (!clipboard.getBlock(pt).getBlockType().getMaterial().isAir()) {
                    total++;
                    if (pt.y() == minY) {
                        base++;
                    }
                }
            }

            SchematicStats stats = new SchematicStats(total, base, width, height, length);
            statsCache.put(schematicId, stats);
            return stats;

        } catch (Exception e) {
            plugin.getLogger().warning("Falha ao analisar a schematic: " + schematicId);
            e.printStackTrace();
            return new SchematicStats(1, 0, 10, 10, 10); // Fallback
        }
    }

    public void preloadSchematicsAsync(Set<String> schematicIds) {
        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            for (String id : schematicIds) {
                getStats(id);
            }
            long time = System.currentTimeMillis() - start;
            plugin.getLogger().info("[Flare] Analyzer: Cache de estatísticas gerado em " + time + "ms");
        });
    }
}