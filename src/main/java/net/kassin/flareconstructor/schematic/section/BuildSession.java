package net.kassin.flareconstructor.schematic.section;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BuildSession {

    private static final Cache<@NotNull UUID, BuildSession> ACTIVE_SESSIONS = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .removalListener((UUID key, BuildSession session, RemovalCause cause) -> {
                if (cause.wasEvicted()) session.cancel();
            })
            .build();

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<BlockSnapshot> placed = new ArrayList<>();

    private final Long2ObjectMap<BlockState> visualSnapshot = Long2ObjectMaps.synchronize(new Long2ObjectLinkedOpenHashMap<>());

    @Getter
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    @Getter
    @Setter
    private Location center;

    @Getter
    private boolean cancelled = false;

    @Setter
    @Getter
    private CompletableFuture<Void> future;

    public static BuildSession create(UUID playerId) {
        BuildSession session = new BuildSession();
        ACTIVE_SESSIONS.put(playerId, session);
        return session;
    }

    public static BuildSession get(UUID playerId) {
        return ACTIVE_SESSIONS.getIfPresent(playerId);
    }

    public static void remove(UUID playerId) {
        ACTIVE_SESSIONS.invalidate(playerId);
    }

    public static boolean hasActive(UUID playerId) {
        return ACTIVE_SESSIONS.asMap().containsKey(playerId);
    }

    public static Collection<BuildSession> getAllActive() {
        return ACTIVE_SESSIONS.asMap().values();
    }

    public void registerTask(BukkitTask task) {
        tasks.add(task);
    }

    public void cancel() {
        this.cancelled = true;
        if (future != null && !future.isDone()) future.cancel(true);
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        viewers.clear();
    }

    public void registerPlaced(World world, int x, int y, int z,
                               Material previous, boolean isPhantom) {
        if (!isPhantom) {
            placed.add(new BlockSnapshot(world, x, y, z, previous));
        }
    }

    public void updateVisualSnapshot(int x, int y, int z, BlockState state) {
        long key = packKey(x, y, z);
        visualSnapshot.put(key, state);
    }

    public Map<Long, BlockState> getVisualSnapshotCopy() {
        synchronized (visualSnapshot) {
            return new LinkedHashMap<>(visualSnapshot);
        }
    }

    public void clearVisualSnapshot() {
        visualSnapshot.clear();
        viewers.clear();
    }

    public static long packKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (y & 0xFFF) << 26)
                | ((long) (z & 0x3FFFFFF));
    }

    public static int unpackX(long key) {
        int v = (int) (key >>> 38) & 0x3FFFFFF;
        return (v & 0x2000000) != 0 ? v | 0xFC000000 : v;
    }

    public static int unpackY(long key) {
        int v = (int) (key >>> 26) & 0xFFF;
        return (v & 0x800) != 0 ? v | 0xFFFFF000 : v;
    }

    public static int unpackZ(long key) {
        int v = (int) (key) & 0x3FFFFFF;
        return (v & 0x2000000) != 0 ? v | 0xFC000000 : v;
    }

    public void undo() {
        if (placed.isEmpty()) return;

        List<BlockSnapshot> reversed = new ArrayList<>(placed);
        Collections.reverse(reversed);

        Map<World, List<BlockSnapshot>> byWorld = new LinkedHashMap<>();
        for (BlockSnapshot snap : reversed) {
            byWorld.computeIfAbsent(snap.world(), w -> new ArrayList<>()).add(snap);
        }

        CompletableFuture.runAsync(() -> {
            for (Map.Entry<World, List<BlockSnapshot>> entry : byWorld.entrySet()) {

                try (EditSession editSession =
                             WorldEdit.getInstance()
                                     .newEditSessionBuilder()
                                     .world(BukkitAdapter.adapt(entry.getKey()))
                                     .fastMode(true)
                                     .checkMemory(false)
                                     .allowedRegionsEverywhere()
                                     .build()) {

                    for (BlockSnapshot snap : entry.getValue()) {
                        BlockState state =
                                BukkitAdapter.adapt(snap.previous().createBlockData());

                        try {
                            editSession.smartSetBlock(BlockVector3.at(snap.x(), snap.y(), snap.z()), state);

                        } catch (Exception ignored) {
                        }
                    }

                }
            }
            placed.clear();
        });
    }

    public void clear() {
        placed.clear();
        tasks.clear();
    }

    record BlockSnapshot(World world, int x, int y, int z, Material previous) {
    }
}