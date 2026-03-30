package net.kassin.flareconstructor.schematic.engine.impl;

import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.menu.configuration.BuildData;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import net.kassin.flareconstructor.schematic.engine.PhaseRunner;
import net.kassin.flareconstructor.schematic.packets.PacketDispatcher;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class AsyncPacketPhaseRunner implements PhaseRunner {

    private final FlareConstructorPlugin plugin;

    public AsyncPacketPhaseRunner(FlareConstructorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> runPhaseAsync(List<Layers.Layer> layers, World world, BuildData data, ConstructionProject session) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        List<BlockEntry> allBlocks = new ArrayList<>();

        layers.forEach(layer -> allBlocks.addAll(layer.blocks()));

        if (allBlocks.isEmpty()) {
            future.complete(null);
            return future;
        }

        int blocksPerStrike = data.blocksPerStrike();

        final int[] currentIndex = {0};

        session.setWorkCallback(() -> {
            if (session.isCancelled()) {
                future.completeExceptionally(new CancellationException("Fase de pacotes cancelada."));
                session.setWorkCallback(null);
                return;
            }

            int start = currentIndex[0];

            if (start >= allBlocks.size()) return;

            int end = Math.min(start + blocksPerStrike, allBlocks.size());

            currentIndex[0] = end;

            CompletableFuture.runAsync(() -> {
                PacketDispatcher.sendToViewers(
                        PacketDispatcher.buildSectionMap(allBlocks, start, end, session),
                        session.getViewers()
                );
            }).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    BlockEntry lastBlock = allBlocks.get(end - 1);
                    session.registerPlaced(lastBlock);
                    session.notifyBlocksPlaced(new Location(world, lastBlock.x(), lastBlock.y(), lastBlock.z()));

                    if (end >= allBlocks.size()) {
                        if (!future.isDone()) {
                            future.complete(null);
                            session.setWorkCallback(null);
                        }
                    }
                });
            });
        });

        return future;
    }

}