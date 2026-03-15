package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SchematicRemapper {

    private SchematicRemapper() {}

    public static List<Material> getUniqueMaterials(Clipboard clipboard) {
        Set<Material> mats = new HashSet<>();
        for (BlockVector3 pos : clipboard.getRegion()) {
            Material mat = BukkitAdapter.adapt(clipboard.getBlock(pos).getBlockType());
            if (mat != null && mat.isBlock() && !mat.isAir()) {
                mats.add(mat);
            }
        }
        return new ArrayList<>(mats);
    }

    public static void remap(Clipboard clipboard, Map<Material, Material> replacements) {
        BlockVector3 min = clipboard.getMinimumPoint();
        BlockVector3 max = clipboard.getMaximumPoint();

        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    BlockState state = clipboard.getBlock(pos);

                    Material mat = BukkitAdapter.adapt(state.getBlockType());
                    if (mat == null) continue;

                    Material replacement = replacements.get(mat);
                    if (replacement == null) continue;

                    BlockData oldData = BukkitAdapter.adapt(state);
                    String oldString = oldData.getAsString();
                    int bracketIndex = oldString.indexOf('[');

                    BlockData newData;
                    if (bracketIndex != -1) {
                        String states = oldString.substring(bracketIndex);
                        try {
                            newData = Bukkit.createBlockData(replacement.getKey().toString() + states);
                        } catch (IllegalArgumentException e) {
                            newData = Bukkit.createBlockData(replacement);
                        }
                    } else {
                        newData = Bukkit.createBlockData(replacement);
                    }

                    clipboard.setBlock(pos, BukkitAdapter.adapt(newData));
                }
            }
        }
    }
}