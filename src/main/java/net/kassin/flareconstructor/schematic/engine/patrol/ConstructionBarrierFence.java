package net.kassin.flareconstructor.schematic.engine.patrol;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class ConstructionBarrierFence {

    private static final int FENCE_HEIGHT = 3;

    private static final int FENCE_OFFSET = 1;

    private final World world;
    private final int minX, maxX, minZ, maxZ, baseY;

    public ConstructionBarrierFence(Layers layers, World world) {
        this.world = world;

        int tmpMinX = Integer.MAX_VALUE, tmpMaxX = Integer.MIN_VALUE;
        int tmpMinZ = Integer.MAX_VALUE, tmpMaxZ = Integer.MIN_VALUE;
        int tmpBaseY = Integer.MAX_VALUE;

        List<BlockEntry> allBlocks = new ArrayList<>();
        allBlocks.addAll(layers.phase1().stream().flatMap(l -> l.blocks().stream()).toList());
        allBlocks.addAll(layers.phase2().stream().flatMap(l -> l.blocks().stream()).toList());

        for (BlockEntry b : allBlocks) {
            if (b.x() < tmpMinX) tmpMinX = b.x();
            if (b.x() > tmpMaxX) tmpMaxX = b.x();
            if (b.z() < tmpMinZ) tmpMinZ = b.z();
            if (b.z() > tmpMaxZ) tmpMaxZ = b.z();
            if (b.y() < tmpBaseY) tmpBaseY = b.y();
        }

        this.minX  = tmpMinX  - FENCE_OFFSET;
        this.maxX  = tmpMaxX  + FENCE_OFFSET;
        this.minZ  = tmpMinZ  - FENCE_OFFSET;
        this.maxZ  = tmpMaxZ  + FENCE_OFFSET;
        this.baseY = tmpBaseY;
    }

    public void place() {
        try (EditSession session = buildEditSession()) {
            forEachFenceBlock((x, y, z) ->
                session.setBlock(BlockVector3.at(x, y, z),
                    BlockTypes.BARRIER.getDefaultState())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void remove() {
        try (EditSession session = buildEditSession()) {
            forEachFenceBlock((x, y, z) ->
                session.setBlock(BlockVector3.at(x, y, z),
                    BlockTypes.AIR.getDefaultState())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void forEachFenceBlock(BlockConsumer consumer) {
        for (int y = baseY; y < baseY + FENCE_HEIGHT; y++) {
            // Sul (minZ)
            for (int x = minX; x <= maxX; x++) consumer.accept(x, y, minZ);
            // Norte (maxZ)
            for (int x = minX; x <= maxX; x++) consumer.accept(x, y, maxZ);
            // Oeste (minX)
            for (int z = minZ + 1; z < maxZ; z++) consumer.accept(minX, y, z);
            // Leste (maxX)
            for (int z = minZ + 1; z < maxZ; z++) consumer.accept(maxX, y, z);
        }
    }

    private EditSession buildEditSession() {
        return WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(world))
            .fastMode(true) // FAWE fast mode — sem history, sem lighting calc
            .build();
    }

    @FunctionalInterface
    private interface BlockConsumer {
        void accept(int x, int y, int z);
    }

    // ── Getters para o PerimeterGenerator ────────────────────────────────────

    /** Bounding box do cerco (para o PerimeterGenerator gerar nós dentro dela). */
    public int innerMinX() { return minX + 1; }
    public int innerMaxX() { return maxX - 1; }
    public int innerMinZ() { return minZ + 1; }
    public int innerMaxZ() { return maxZ - 1; }
    public int baseY()     { return baseY; }
}
