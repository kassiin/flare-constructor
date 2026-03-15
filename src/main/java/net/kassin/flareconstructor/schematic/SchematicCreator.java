package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public final class SchematicCreator {

    private SchematicCreator() {
    }

    public static void createAndRegister(FlareConstructorPlugin plugin, Player player, String id) throws Exception {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));

        Region region;
        try {
            region = session.getSelection(session.getSelectionWorld());
        } catch (IncompleteRegionException e) {
            throw new IllegalArgumentException("selecao_incompleta");
        }

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(session.getPlacementPosition(BukkitAdapter.adapt(player)));

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(session.getSelectionWorld())
                .checkMemory(false)
                .build()) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );
            Operations.complete(forwardExtentCopy);
        }

        File folder = new File(plugin.getDataFolder(), "schematics");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, id + ".schem");
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        }

        FileConfiguration config = plugin.getInitializer().getConstructionsConfig();
        List<String> builds = config.getStringList("builds");

        if (!builds.contains(id)) {
            builds.add(id);
            config.set("builds", builds);
            plugin.getInitializer().getConstructionsConfig().saveConfig();
            plugin.getInitializer().getConstructionRegistry().load(config);
        }
    }

}