package net.kassin.flareconstructor.schematic.engine.impl;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import net.kassin.flareconstructor.schematic.engine.BlockFlusher;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class WorldEditBlockFlusher implements BlockFlusher {

    private final FlareConstructorPlugin plugin;

    public WorldEditBlockFlusher(FlareConstructorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> flushAsync(List<Layers.Layer> layers, World world, ConstructionProject session) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    executeFlush(layers, world, session);
                    future.complete(null);
                } catch (CancellationException e) {
                    future.completeExceptionally(e);
                } catch (Exception e) {
                    future.completeExceptionally(new RuntimeException("Falha no WorldEdit", e));
                }
            }
        }.runTaskAsynchronously(plugin);

        session.registerTask(task);
        return future;
    }

    private void executeFlush(List<Layers.Layer> layers, World world, ConstructionProject session) {

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .fastMode(true)
                .checkMemory(false)
                .allowedRegionsEverywhere()
                .build()) {

            for (Layers.Layer layer : layers) {

                for (BlockEntry entry : layer.blocks()) {

                    if (session.isCancelled()) {
                        throw new CancellationException("Cancelado no flush físico");
                    }

                    Material mat = BukkitAdapter.adapt(entry.state().getBlockType());
                    if (mat == null || mat == Material.AIR) continue;

                    session.notifyBlocksPlaced(new Location(world, entry.x(), entry.y(), entry.z()));

                    try {
                        editSession.smartSetBlock(BlockVector3.at(entry.x(), entry.y(), entry.z()), entry.state());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

}