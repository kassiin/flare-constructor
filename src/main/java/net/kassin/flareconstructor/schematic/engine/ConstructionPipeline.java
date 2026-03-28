package net.kassin.flareconstructor.schematic.engine;

import net.kassin.flareconstructor.menu.configuration.BuildData;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConstructionPipeline {

    private final PhaseRunner phaseRunner;
    private final BlockFlusher blockFlusher;

    public ConstructionPipeline(PhaseRunner phaseRunner, BlockFlusher blockFlusher) {
        this.phaseRunner = phaseRunner;
        this.blockFlusher = blockFlusher;
    }

    public CompletableFuture<Void> execute(Layers layers, World world, BuildData data, ConstructionProject session) {

        List<Layers.Layer> phase1 = layers.phase1();
        List<Layers.Layer> phase2 = layers.phase2();
        List<Layers.Layer> phase2Filtered;

        if (phase2.isEmpty()) {
            phase2Filtered = phase2;
        } else {
            phase2Filtered = phase2.subList(1, phase2.size());
        }

        return phaseRunner.runPhaseAsync(phase1, world, data, session)
                .thenCompose(v -> blockFlusher.flushAsync(phase1, world, session))
                .thenCompose(v -> phaseRunner.runPhaseAsync(phase2Filtered, world, data, session))
                .thenCompose(v -> blockFlusher.flushAsync(phase2Filtered, world, session))
                .thenRun(session::clearVisualSnapshot);
    }

}