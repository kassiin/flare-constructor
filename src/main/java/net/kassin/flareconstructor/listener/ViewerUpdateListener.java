package net.kassin.flareconstructor.listener;

import com.sk89q.worldedit.world.block.BlockState;
import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.api.AsyncAPI;
import net.kassin.flareconstructor.ConstructionMessage;
import net.kassin.flareconstructor.api.FlareListener;
import net.kassin.flareconstructor.schematic.blocks.BlockEntry;
import net.kassin.flareconstructor.schematic.packets.PacketDispatcher;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import net.kassin.flareconstructor.schematic.tracking.ProjectSpatialTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class ViewerUpdateListener extends FlareListener {

    private final ProjectSpatialTracker tracker;
    private final AsyncAPI asyncAPI;
    private final ConstructionMessage message;

    public ViewerUpdateListener(ProjectSpatialTracker tracker, ConstructionMessage message) {
        this.tracker = tracker;
        this.message = message;
        this.asyncAPI = FlareCorePlugin.getAPI().getAsyncAPI();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkCross(PlayerMoveEvent event) {
        int fromChunkX = event.getFrom().getBlockX() >> 4;
        int fromChunkZ = event.getFrom().getBlockZ() >> 4;

        int toChunkX = event.getTo().getBlockX() >> 4;
        int toChunkZ = event.getTo().getBlockZ() >> 4;

        if (fromChunkX == toChunkX && fromChunkZ == toChunkZ) return;

        List<ConstructionProject> oldProjects = tracker.getProjectsInChunk(fromChunkX, fromChunkZ);
        List<ConstructionProject> newProjects = tracker.getProjectsInChunk(toChunkX, toChunkZ);

        Player player = event.getPlayer();

        // Remove dos projetos que ficaram para trás (opcional, mas recomendado)
        for (ConstructionProject project : oldProjects) {
            if (!newProjects.contains(project)) {
                project.getViewers().remove(player.getUniqueId());
                // Dica: Aqui você poderia mandar um pacote limpando a área pra ele.
            }
        }

        for (ConstructionProject project : newProjects) {
            if (!oldProjects.contains(project)) {
                project.getViewers().add(player.getUniqueId());

                sendCatchUpPackets(player, project);

            }
        }
    }

    private void sendCatchUpPackets(Player player, ConstructionProject project) {
        Map<Long, com.sk89q.worldedit.world.block.BlockState> historyMap = project.getVisualSnapshotCopy();

        if (historyMap.isEmpty()) return;

        asyncAPI.run(() -> {
            Set<UUID> singleViewer = Set.of(player.getUniqueId());

            List<BlockEntry> catchUpBlocks = new ArrayList<>(historyMap.size());

            for (Map.Entry<Long, com.sk89q.worldedit.world.block.BlockState> entry : historyMap.entrySet()) {
                long key = entry.getKey();
                int x = ConstructionProject.unpackX(key);
                int y = ConstructionProject.unpackY(key);
                int z = ConstructionProject.unpackZ(key);
                BlockState state = entry.getValue();

                catchUpBlocks.add(new BlockEntry(null, x, y, z, state));
            }

            PacketDispatcher.sendToViewers(
                    PacketDispatcher.buildSectionMap(catchUpBlocks, 0, catchUpBlocks.size(), project),
                    singleViewer
            );
        });
    }

}