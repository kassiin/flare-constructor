package net.kassin.flareconstructor.schematic.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.section.BuildSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public final class PacketDispatcher {

    private PacketDispatcher() {}

    public static void sendToViewers(Map<Long, List<EncodedBlock>> bySection, Set<UUID> viewers) {
        if (viewers.isEmpty()) return;

        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) continue;

            sendToUser(user, bySection);
        }
    }

    public static void sendToUser(User user, Map<Long, List<EncodedBlock>> bySection) {
        bySection.forEach((key, blockList) ->
                user.sendPacket(new WrapperPlayServerMultiBlockChange(
                        decodeSectionKey(key), false, blockList.toArray(new EncodedBlock[0]))));
    }

    public static void sendCatchUp(Player player, BuildSession session) {
        Map<Long, BlockState> snapshot = session.getVisualSnapshotCopy();
        if (snapshot.isEmpty()) return;

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;

        sendToUser(user, buildSectionMapFromSnapshot(snapshot));
    }

    public static void clearVisuals(Player player, BuildSession session) {
        Map<Long, BlockState> snapshot = session.getVisualSnapshotCopy();
        if (snapshot.isEmpty()) return;

        World world = session.getCenter().getWorld();
        if (world == null) return;

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;

        sendToUser(user, buildSectionMapFromWorld(snapshot, world));
    }

    public static Map<Long, List<EncodedBlock>> buildSectionMap(
            List<BlockEntry> entries, int from, int to, BuildSession session) {

        Map<Long, List<EncodedBlock>> bySection = new LinkedHashMap<>();

        for (int i = from; i < to; i++) {
            BlockEntry entry = entries.get(i);
            Material mat = BukkitAdapter.adapt(entry.state().getBlockType());
            if (mat == null || mat == Material.AIR) continue;

            int globalId = SpigotConversionUtil
                    .fromBukkitBlockData(BukkitAdapter.adapt(entry.state()))
                    .getGlobalId();

            bySection.computeIfAbsent(toSectionKey(entry.x() >> 4, entry.y() >> 4, entry.z() >> 4), k -> new ArrayList<>())
                    .add(new EncodedBlock(globalId, entry.x() & 0xF, entry.y() & 0xF, entry.z() & 0xF));

            session.updateVisualSnapshot(entry.x(), entry.y(), entry.z(), entry.state());
        }

        return bySection;
    }

    private static Map<Long, List<EncodedBlock>> buildSectionMapFromSnapshot(Map<Long, BlockState> snapshot) {
        Map<Long, List<EncodedBlock>> bySection = new LinkedHashMap<>();

        for (Map.Entry<Long, BlockState> e : snapshot.entrySet()) {
            int x = BuildSession.unpackX(e.getKey());
            int y = BuildSession.unpackY(e.getKey());
            int z = BuildSession.unpackZ(e.getKey());

            Material mat = BukkitAdapter.adapt(e.getValue().getBlockType());
            if (mat == null || mat == Material.AIR) continue;

            int globalId = SpigotConversionUtil
                    .fromBukkitBlockData(BukkitAdapter.adapt(e.getValue()))
                    .getGlobalId();

            bySection.computeIfAbsent(toSectionKey(x >> 4, y >> 4, z >> 4), k -> new ArrayList<>())
                    .add(new EncodedBlock(globalId, x & 0xF, y & 0xF, z & 0xF));
        }

        return bySection;
    }

    private static Map<Long, List<EncodedBlock>> buildSectionMapFromWorld(
            Map<Long, BlockState> snapshot, World world) {

        Map<Long, List<EncodedBlock>> bySection = new LinkedHashMap<>();

        for (long posKey : snapshot.keySet()) {
            int x = BuildSession.unpackX(posKey);
            int y = BuildSession.unpackY(posKey);
            int z = BuildSession.unpackZ(posKey);

            int globalId = SpigotConversionUtil
                    .fromBukkitBlockData(world.getBlockAt(x, y, z).getBlockData())
                    .getGlobalId();

            bySection.computeIfAbsent(toSectionKey(x >> 4, y >> 4, z >> 4), k -> new ArrayList<>())
                    .add(new EncodedBlock(globalId, x & 0xF, y & 0xF, z & 0xF));
        }

        return bySection;
    }

    public static long toSectionKey(int cx, int cy, int cz) {
        return ((long) (cx & 0x3FFFFF))
                | ((long) (cz & 0x3FFFFF) << 22)
                | ((long) cy << 44);
    }

    private static Vector3i decodeSectionKey(long key) {
        int cx = (int) (key & 0x3FFFFF);
        if ((cx & 0x200000) != 0) cx |= 0xFFC00000;
        int cz = (int) ((key >>> 22) & 0x3FFFFF);
        if ((cz & 0x200000) != 0) cz |= 0xFFC00000;
        int cy = (int) (key >>> 44);
        return new Vector3i(cx, cy, cz);
    }
}
