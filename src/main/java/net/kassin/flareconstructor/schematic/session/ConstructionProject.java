package net.kassin.flareconstructor.schematic.session;

import com.sk89q.worldedit.world.registry.BlockMaterial;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import com.sk89q.worldedit.world.block.BlockState;
import lombok.Data;
import net.kassin.flareconstructor.orchestrator.observer.BuildObserver;
import net.kassin.flareconstructor.protection.WorksiteBounds;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ConstructionProject {

    private final UUID ownerId;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private final List<BukkitTask> tasks = Collections.synchronizedList(new ArrayList<>());
    private final List<BlockSnapshot> placedBlocks = Collections.synchronizedList(new ArrayList<>());
    private final Long2ObjectMap<BlockState> visualSnapshot = Long2ObjectMaps.synchronize(new Long2ObjectLinkedOpenHashMap<>());
    private final List<BuildObserver> observers = new ArrayList<>();
    private WorksiteBounds worksiteBounds;

    private Location center;
    private boolean cancelled = false;
    private CompletableFuture<Void> future;
    private Runnable onAgentWork;

    public ConstructionProject(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void addObserver(BuildObserver observer) {
        this.observers.add(observer);
    }

    public void notifyBuildStart(Location center) {
        observers.forEach(obs -> obs.onBuildStart(center));
    }

    public void notifyBlocksPlaced(Location target) {
        observers.forEach(obs -> obs.onBlocksPlaced(target));
    }

    public void notifyBuildComplete() {
        observers.forEach(BuildObserver::onBuildComplete);
    }

    public void registerTask(BukkitTask task) {
        tasks.add(task);
    }

    public void cancel() {
        this.cancelled = true;
        if (future != null && !future.isDone()) future.cancel(true);
        synchronized (tasks) {
            tasks.forEach(BukkitTask::cancel);
            tasks.clear();
        }
        viewers.clear();
    }

    public void registerPlaced(BlockEntry entry) {
        placedBlocks.add(new BlockSnapshot(entry.world(), entry.x(), entry.y(), entry.z(), entry.state().getBlockType().getMaterial()));
    }

    public void registerPlaced(World world, int x, int y, int z, BlockMaterial previous, boolean isPhantom) {
        if (!isPhantom) {
            placedBlocks.add(new BlockSnapshot(world, x, y, z, previous));
        }
    }

    public List<BlockSnapshot> getPlacedBlocks() {
        return Collections.unmodifiableList(placedBlocks);
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

    public void clear() {
        placedBlocks.clear();
        synchronized (tasks) {
            tasks.clear();
        }
    }

    public void setWorkCallback(Runnable onAgentWork) {
        this.onAgentWork = onAgentWork;
    }

    public void triggerWork() {
        if (onAgentWork != null) {
            onAgentWork.run();
        }
    }

    public static long packKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | ((long) (z & 0x3FFFFFF));
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

    public record BlockSnapshot(World world, int x, int y, int z, BlockMaterial previous) {
    }
}