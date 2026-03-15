package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.*;

public final class SchematicLayerBuilder {

    private SchematicLayerBuilder() {
    }

    public static Layers buildLayers(Clipboard clipboard, Set<BlockVector3> treeBlocks, Location origin) {
        BlockVector3 clipMin = clipboard.getMinimumPoint();
        BlockVector3 dim = clipboard.getDimensions();

        TreeMap<Integer, Layers.Layer> phase1ByY = new TreeMap<>();
        TreeMap<Integer, Layers.Layer> phase2ByY = new TreeMap<>();

        for (int x = 0; x < dim.x(); x++) {
            for (int y = 0; y < dim.y(); y++) {
                for (int z = 0; z < dim.z(); z++) {
                    BlockVector3 rel = BlockVector3.at(x, y, z);
                    BlockState state = clipboard.getBlock(clipMin.add(x, y, z));

                    if (state.getBlockType().getMaterial().isAir()) continue;

                    Material mat = BukkitAdapter.adapt(state.getBlockType());
                    if (mat == null || mat == Material.AIR) continue;

                    int worldX = origin.getBlockX() + x;
                    int worldY = origin.getBlockY() + y;
                    int worldZ = origin.getBlockZ() + z;

                    phase2ByY.computeIfAbsent(y, k -> new Layers.Layer(new ArrayList<>()))
                            .blocks().add(new BlockEntry(worldX, worldY, worldZ, state));

                    phase1ByY.computeIfAbsent(y, k -> new Layers.Layer(new ArrayList<>()))
                            .blocks().add(resolvePhase1Entry(rel, y, worldX, worldY, worldZ, mat, state, treeBlocks));
                }
            }
        }

        phase1ByY.values().forEach(layer -> Collections.shuffle(layer.blocks()));
        phase2ByY.values().forEach(layer -> Collections.shuffle(layer.blocks()));

        return new Layers(new ArrayList<>(phase1ByY.values()), new ArrayList<>(phase2ByY.values()));
    }

    private static BlockEntry resolvePhase1Entry(
            BlockVector3 rel, int y,
            int worldX, int worldY, int worldZ,
            Material mat, BlockState state,
            Set<BlockVector3> treeBlocks) {

        boolean useReal = y == 0 || treeBlocks.contains(rel) || !isStructural(mat);

        if (useReal) return new BlockEntry(worldX, worldY, worldZ, state);

        Material phantomMat = toPhantomMaterial(mat);
        BlockState phantomState = createStateWithData(state, phantomMat);

        return new BlockEntry(worldX, worldY, worldZ, phantomState);
    }

    private static BlockState createStateWithData(BlockState originalState, Material newMaterial) {
        BlockData oldData = BukkitAdapter.adapt(originalState);
        String oldString = oldData.getAsString();
        int bracketIndex = oldString.indexOf('[');

        BlockData newData;
        if (bracketIndex != -1) {
            String states = oldString.substring(bracketIndex);
            try {
                newData = Bukkit.createBlockData(newMaterial.getKey().toString() + states);
            } catch (IllegalArgumentException e) {
                newData = Bukkit.createBlockData(newMaterial);
            }
        } else {
            newData = Bukkit.createBlockData(newMaterial);
        }

        return BukkitAdapter.adapt(newData);
    }

    private static boolean isStructural(Material mat) {
        if (mat == null || !mat.isSolid()) return false;

        String name = mat.name();
        return !name.contains("SAPLING") && !name.contains("FLOWER")
                && !name.contains("GRASS") && !name.contains("FERN")
                && !name.contains("MUSHROOM") && !name.contains("VINE")
                && !name.contains("LILY") && !name.contains("CORAL")
                && !name.contains("BAMBOO") && !name.contains("LEAVES")
                && !name.contains("BANNER") && !name.contains("SIGN")
                && !name.contains("CANDLE") && !name.contains("CARPET")
                && mat != Material.TORCH && mat != Material.WALL_TORCH
                && mat != Material.SOUL_TORCH && mat != Material.LANTERN
                && mat != Material.SOUL_LANTERN && mat != Material.LADDER
                && mat != Material.GLASS_PANE;
    }

    private static Material toPhantomMaterial(Material mat) {
        String name = mat.name();
        if (name.endsWith("_STAIRS")) return Material.BRICK_STAIRS;
        if (name.endsWith("_SLAB")) return Material.BRICK_SLAB;
        if (name.endsWith("_WALL")) return Material.BRICK_WALL;
        return Material.BRICKS;
    }
}