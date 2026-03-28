package net.kassin.flareconstructor.schematic.engine.patrol;

import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class PerimeterGenerator {

    public static PatrolData generate(Layers layers, World world, int expandDistance) {

        List<BlockEntry> allBlocks = new ArrayList<>();

        allBlocks.addAll(layers.phase1().stream().flatMap(l -> l.blocks().stream()).toList());
        allBlocks.addAll(layers.phase2().stream().flatMap(l -> l.blocks().stream()).toList());

        if (allBlocks.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int baseY = Integer.MAX_VALUE;

        for (BlockEntry b : allBlocks) {
            if (b.x() < minX) minX = b.x();
            if (b.x() > maxX) maxX = b.x();
            if (b.z() < minZ) minZ = b.z();
            if (b.z() > maxZ) maxZ = b.z();
            if (b.y() < baseY) baseY = b.y();
        }

        int outerMinX = minX - expandDistance;
        int outerMaxX = maxX + expandDistance;
        int outerMinZ = minZ - expandDistance;
        int outerMaxZ = maxZ + expandDistance;

        Location center = new Location(world, (minX + maxX) / 2.0, baseY, (minZ + maxZ) / 2.0);
        List<Location> track = new ArrayList<>();
        int spacing = 5;

        for (int x = outerMinX; x < outerMaxX; x += spacing) {
            track.add(new Location(world, x + 0.5, getSafeY(world, x, baseY, outerMinZ), outerMinZ + 0.5));
        }
        for (int z = outerMinZ; z < outerMaxZ; z += spacing) {
            track.add(new Location(world, outerMaxX + 0.5, getSafeY(world, outerMaxX, baseY, z), z + 0.5));
        }
        for (int x = outerMaxX; x > outerMinX; x -= spacing) {
            track.add(new Location(world, x + 0.5, getSafeY(world, x, baseY, outerMaxZ), outerMaxZ + 0.5));
        }
        for (int z = outerMaxZ; z > outerMinZ; z -= spacing) {
            track.add(new Location(world, outerMinX + 0.5, getSafeY(world, outerMinX, baseY, z), z + 0.5));
        }

        return new PatrolData(track, center);
    }

    private static int getSafeY(World world, int x, int baseY, int z) {
        for (int checkY = baseY + 4; checkY >= baseY - 4; checkY--) {

            if (world.getBlockAt(x, checkY - 1, z).getType().isSolid()
                    && !world.getBlockAt(x, checkY, z).getType().isSolid()
                    && !world.getBlockAt(x, checkY + 1, z).getType().isSolid()) {

                return checkY;
            }

        }
        return baseY;
    }

    public record PatrolData(List<Location> track, Location center) {}
}