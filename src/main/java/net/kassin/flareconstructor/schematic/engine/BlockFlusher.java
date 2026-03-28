package net.kassin.flareconstructor.schematic.engine;

import net.kassin.flareconstructor.schematic.blocks.Layers;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.World;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BlockFlusher {
    CompletableFuture<Void> flushAsync(List<Layers.Layer> layers, World world, ConstructionProject session);
}