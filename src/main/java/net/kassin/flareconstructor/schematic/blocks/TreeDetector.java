package net.kassin.flareconstructor.schematic.blocks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Material;

import java.util.*;

public final class TreeDetector {

    private TreeDetector() {}

    public static Set<BlockVector3> detect(Clipboard clipboard) {
        BlockVector3 clipMin = clipboard.getMinimumPoint();
        BlockVector3 dim = clipboard.getDimensions();

        Map<BlockVector3, Material> matMap = new HashMap<>();
        Set<BlockVector3> leaves = new HashSet<>();

        for (int x = 0; x < dim.x(); x++) {
            for (int y = 0; y < dim.y(); y++) {
                for (int z = 0; z < dim.z(); z++) {
                    BlockVector3 rel = BlockVector3.at(x, y, z);
                    Material mat = BukkitAdapter.adapt(
                            clipboard.getBlock(clipMin.add(x, y, z)).getBlockType());

                    if (mat == null) continue;

                    matMap.put(rel, mat);
                    String name = mat.name();

                    if (name.contains("LEAVES") || name.contains("SAPLING"))
                        leaves.add(rel);
                }
            }
        }

        Set<BlockVector3> treeBlocks = new HashSet<>();
        Set<BlockVector3> visited = new HashSet<>();

        for (BlockVector3 leaf : leaves) {
            if (visited.contains(leaf)) continue;

            Queue<BlockVector3> queue = new ArrayDeque<>();
            Set<BlockVector3> group = new HashSet<>();

            queue.add(leaf);
            visited.add(leaf);
            group.add(leaf);

            while (!queue.isEmpty()) {
                BlockVector3 cur = queue.poll();
                for (BlockVector3 neighbor : neighbors6(cur)) {
                    if (visited.contains(neighbor)) continue;

                    Material m = matMap.get(neighbor);
                    if (m == null) continue;

                    if (isTreeMaterial(m.name())) {
                        visited.add(neighbor);
                        group.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            treeBlocks.addAll(group);
        }

        return treeBlocks;
    }

    private static boolean isTreeMaterial(String name) {
        return name.contains("LEAVES") || name.contains("LOG")
                || name.contains("WOOD") || name.contains("STEM")
                || name.contains("HYPHAE") || name.contains("SAPLING");
    }

    private static List<BlockVector3> neighbors6(BlockVector3 pos) {
        return List.of(
                pos.add(1, 0, 0), pos.add(-1, 0, 0),
                pos.add(0, 1, 0), pos.add(0, -1, 0),
                pos.add(0, 0, 1), pos.add(0, 0, -1)
        );
    }
}
