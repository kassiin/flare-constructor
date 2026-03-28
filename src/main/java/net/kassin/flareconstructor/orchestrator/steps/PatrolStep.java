package net.kassin.flareconstructor.orchestrator.steps;

import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.orchestrator.ConstructionContext;
import net.kassin.flareconstructor.orchestrator.ConstructionStep;
import net.kassin.flareconstructor.schematic.engine.patrol.ConstructionBarrierFence;
import net.kassin.flareconstructor.schematic.engine.patrol.PatrolManager;
import net.kassin.flareconstructor.schematic.engine.patrol.PerimeterGenerator;

import java.util.concurrent.CompletableFuture;

public class PatrolStep implements ConstructionStep {

    private final FlareConstructorPlugin plugin;

    public PatrolStep(FlareConstructorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> execute(ConstructionContext context) {
        CompletableFuture<Void> stepFuture = new CompletableFuture<>();

        PerimeterGenerator.PatrolData patrolData = PerimeterGenerator.generate(
                context.getLayers(),
                context.getOrigin().getWorld(),
                3
        );

        if (patrolData == null || patrolData.track().isEmpty()) {
            stepFuture.complete(null);
            return stepFuture;
        }

        PatrolManager patrolManager = new PatrolManager(plugin, context.getSession());

        patrolManager.setupAndDeploy(context.getAgents(), patrolData, context.getSession())
                .thenRun(() -> {

                    ConstructionBarrierFence fence = new ConstructionBarrierFence(
                            context.getLayers(),
                            context.getOrigin().getWorld()
                    );

                    //   context.setBarrierFence(fence);

                    //   CompletableFuture.runAsync(fence::place).thenRun(() -> {

                    plugin.getServer().getScheduler().runTask(plugin, () -> {

                        if (context.getSession().isCancelled()) {
                            stepFuture.complete(null);
                            return;
                        }

                        patrolManager.startAI(patrolData, context.getSession());

                        stepFuture.complete(null);
                    });
                });
        //    });

        return stepFuture;
    }
}