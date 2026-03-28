package net.kassin.flareconstructor.orchestrator;

import java.util.concurrent.CompletableFuture;

public interface ConstructionStep {
    CompletableFuture<Void> execute(ConstructionContext context);
}