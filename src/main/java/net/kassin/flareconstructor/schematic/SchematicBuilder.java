package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import net.flareplugins.core.FlareCorePlugin;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.menu.configuration.BuildData;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import net.kassin.flareconstructor.schematic.blocks.TreeDetector;
import net.kassin.flareconstructor.schematic.packets.PacketDispatcher;
import net.kassin.flareconstructor.schematic.section.BuildSession;
import net.kassin.flareconstructor.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class SchematicBuilder {

    private SchematicBuilder() {
    }

    public static CompletableFuture<Void> build(
            FlareConstructorPlugin plugin,
            BuildData buildData,
            Location origin,
            BuildSession session,
            Runnable onPhase1Done,
            Runnable onComplete,
            Consumer<String> onError) {

        session.setCenter(origin);

        return FlareCorePlugin.getAPI().getAsyncAPI().supply(() -> SchematicLoader.load(plugin, buildData.id()))
                .thenApplyAsync(clipboard -> {

                    if (!buildData.replacements().isEmpty()) {
                        SchematicRemapper.remap(clipboard, buildData.replacements());
                    }

                    com.sk89q.worldedit.regions.Region region = clipboard.getRegion();

                    int minX = region.getMinimumPoint().x();
                    int maxX = region.getMaximumPoint().x();
                    int minZ = region.getMinimumPoint().z();
                    int maxZ = region.getMaximumPoint().z();
                    int bottomY = region.getMinimumPoint().y();

                    int centerX = (minX + maxX) / 2;
                    int centerZ = (minZ + maxZ) / 2;

                    clipboard.setOrigin(BlockVector3.at(centerX, bottomY, centerZ));

                    int halfW = (maxX - minX) / 2;
                    int halfD = (maxZ - minZ) / 2;

                    LocationUtils.Cardinal cardinal = LocationUtils.getCardinal(origin.getYaw());
                    Location centeredOrigin = origin.clone();

                    switch (cardinal) {
                        case NORTH, SOUTH -> centeredOrigin.subtract(halfW, 0, 0);
                        case EAST, WEST -> centeredOrigin.subtract(0, 0, halfD);
                    }

                    return SchematicLayerBuilder.buildLayers(
                            clipboard,
                            TreeDetector.detect(clipboard),
                            centeredOrigin
                    );
                })
                .thenAcceptAsync(layers ->
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                startBuildPipeline(plugin, layers, origin, buildData.delayTicks(),
                                        buildData.blocksPerTick(), buildData.agents(), session, onPhase1Done, onComplete)))
                .exceptionally(e -> {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    plugin.getServer().getScheduler().runTask(plugin, () -> onError.accept(cause.getMessage()));
                    return null;
                });
    }

    private static void startBuildPipeline(
            FlareConstructorPlugin plugin,
            Layers layers,
            Location origin,
            int delayTicks,
            int blocksPerTick,
            int agents,
            BuildSession session,
            Runnable onPhase1Done,
            Runnable onComplete) {

        int clampedAgents = Math.max(1, Math.min(agents, 5));
        World world = origin.getWorld();

        schedulePacketPhase(plugin, layers.phase1(), 0, delayTicks, blocksPerTick,
                clampedAgents, world, session,
                () -> flushPhase(plugin, layers.phase1(), world, session, () -> {
                    onPhase1Done.run();
                    schedulePacketPhase(plugin, layers.phase2(), 0, delayTicks, blocksPerTick,
                            clampedAgents, world, session,
                            () -> flushPhase(plugin, layers.phase2(), world, session, () -> {
                                session.clearVisualSnapshot();
                                onComplete.run();
                            }));
                }));
    }

    private static void flushPhase(
            FlareConstructorPlugin plugin,
            List<Layers.Layer> layers,
            World world,
            BuildSession session,
            Runnable onComplete) {

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                flushToWorld(layers, world, session, onComplete);
            }
        }.runTaskAsynchronously(plugin);

        session.registerTask(task);
    }

    private static void flushToWorld(
            List<Layers.Layer> layers,
            World world,
            BuildSession session,
            Runnable onComplete) {

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .fastMode(true)
                .checkMemory(false)
                .allowedRegionsEverywhere()
                .build()) {

            for (Layers.Layer layer : layers) {
                for (BlockEntry entry : layer.blocks()) {
                    if (session.isCancelled()) return;

                    Material mat = BukkitAdapter.adapt(entry.state().getBlockType());
                    if (mat == null || mat == Material.AIR) continue;

                    session.registerPlaced(world, entry.x(), entry.y(), entry.z(),
                            world.getBlockAt(entry.x(), entry.y(), entry.z()).getType(), false);

                    try {
                        editSession.smartSetBlock(
                                BlockVector3.at(entry.x(), entry.y(), entry.z()), entry.state());
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        onComplete.run();
    }

    private static void schedulePacketPhase(
            FlareConstructorPlugin plugin,
            List<Layers.Layer> layers,
            int layerIndex,
            int delayTicks,
            int blocksPerTick,
            int agents,
            World world,
            BuildSession session,
            Runnable onComplete) {

        if (session.isCancelled()) return;

        if (layerIndex >= layers.size()) {
            onComplete.run();
            return;
        }

        Layers.Layer currentLayer = layers.get(layerIndex);

        if (currentLayer.isEmpty()) {
            schedulePacketPhase(plugin, layers, layerIndex + 1, delayTicks,
                    blocksPerTick, agents, world, session, onComplete);
            return;
        }

        Runnable advance = () -> {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    schedulePacketPhase(plugin, layers, layerIndex + 1, delayTicks,
                            blocksPerTick, agents, world, session, onComplete);
                }
            }.runTaskLaterAsynchronously(plugin, delayTicks);
            session.registerTask(task);
        };

        dispatchAgents(plugin, currentLayer.blocks(), agents, delayTicks,
                blocksPerTick, world, session, advance);
    }

    private static void dispatchAgents(
            FlareConstructorPlugin plugin,
            List<BlockEntry> blocks,
            int agents,
            int delayTicks,
            int blocksPerTick,
            World world,
            BuildSession session,
            Runnable onAllDone) {

        int total = blocks.size();
        int agentCount = Math.min(agents, total);
        int sliceSize = (int) Math.ceil((double) total / agentCount);
        AtomicInteger finishedAgents = new AtomicInteger(agentCount);

        for (int a = 0; a < agentCount; a++) {
            int start = a * sliceSize;
            int end = Math.min(start + sliceSize, total);

            if (start >= total) {
                if (finishedAgents.decrementAndGet() == 0) onAllDone.run();
                continue;
            }

            List<BlockEntry> slice = new ArrayList<>(blocks.subList(start, end));

            sendPacketBatches(plugin, slice, 0, blocksPerTick, world, delayTicks, session, () -> {
                if (finishedAgents.decrementAndGet() == 0) onAllDone.run();
            });
        }

    }

    private static void sendPacketBatches(
            FlareConstructorPlugin plugin,
            List<BlockEntry> entries,
            int offset,
            int batchSize,
            World world,
            int delayTicks,
            BuildSession session,
            Runnable onDone) {

        if (session.isCancelled()) return;

        int end = Math.min(offset + batchSize, entries.size());

        PacketDispatcher.sendToViewers(
                PacketDispatcher.buildSectionMap(entries, offset, end, session),
                session.getViewers());

        if (end < entries.size()) {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    sendPacketBatches(plugin, entries, end, batchSize, world, delayTicks, session, onDone);
                }
            }.runTaskLaterAsynchronously(plugin, delayTicks);
            session.registerTask(task);
        } else {
            onDone.run();
        }
    }
}
