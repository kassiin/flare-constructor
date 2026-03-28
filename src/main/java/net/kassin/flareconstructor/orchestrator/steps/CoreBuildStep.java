package net.kassin.flareconstructor.orchestrator.steps;

import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.orchestrator.ConstructionContext;
import net.kassin.flareconstructor.orchestrator.ConstructionStep;
import net.kassin.flareconstructor.schematic.engine.ConstructionPipeline;
import net.kassin.flareconstructor.schematic.engine.impl.AsyncPacketPhaseRunner;
import net.kassin.flareconstructor.schematic.engine.impl.WorldEditBlockFlusher;

import java.util.concurrent.CompletableFuture;

public class CoreBuildStep implements ConstructionStep {

    private final ConstructionPipeline pipeline;

    public CoreBuildStep(FlareConstructorPlugin plugin) {
        this.pipeline = new ConstructionPipeline(
                new AsyncPacketPhaseRunner(plugin),
                new WorldEditBlockFlusher(plugin)
        );
    }

    @Override
    public CompletableFuture<Void> execute(ConstructionContext context) {
        return pipeline.execute(context.getLayers(), context.getOrigin().getWorld(), context.getBuildData(), context.getSession());
    }
}