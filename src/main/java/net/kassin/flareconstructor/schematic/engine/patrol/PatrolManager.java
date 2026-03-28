package net.kassin.flareconstructor.schematic.engine.patrol;

import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.orchestrator.entity.AgentController;
import net.kassin.flareconstructor.orchestrator.entity.AgentState;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class PatrolManager {

    private final FlareConstructorPlugin plugin;
    private final Random random = new Random();
    private final ConstructionProject session;

    private final Map<AgentController, AgentContext> activeContexts = new HashMap<>();

    public PatrolManager(FlareConstructorPlugin plugin, ConstructionProject session) {
        this.plugin = plugin;
        this.session = session;
    }

    public CompletableFuture<Void> setupAndDeploy(List<AgentController> agents, PerimeterGenerator.PatrolData patrolData, ConstructionProject session) {
        CompletableFuture<Void> deployFuture = new CompletableFuture<>();
        List<Location> track = patrolData.track();

        if (track.isEmpty() || agents.isEmpty()) {
            deployFuture.complete(null);
            return deployFuture;
        }

        for (int i = 0; i < agents.size(); i++) {
            AgentController agent = agents.get(i);
            if (agent.isCycling()) continue;

            int startIndex = getNearestNodeIndex(agent.getEntity().getLocation(), track);
            int direction = (i % 2 == 0) ? 1 : -1;
            startIndex = (startIndex + (i * direction * 2) + track.size()) % track.size();

            Location startNode = track.get(startIndex);
            agent.setCycling(true);
            activeContexts.put(agent, new AgentContext(startIndex, direction));

            agent.navigateTo(startNode, 1.2D);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (session.isCancelled()) {
                    activeContexts.keySet().forEach(AgentController::stop);
                    deployFuture.complete(null);
                    this.cancel();
                    return;
                }

                int readyCount = 0;

                for (Map.Entry<AgentController, AgentContext> entry : activeContexts.entrySet()) {
                    AgentController agent = entry.getKey();
                    AgentContext ctx = entry.getValue();

                    if (!agent.getEntity().isValid()) {
                        readyCount++;
                        continue;
                    }

                    if (ctx.positioned) {
                        readyCount++;
                        continue;
                    }

                    Location targetNode = track.get(ctx.currentIndex);
                    Location currentLoc = agent.getEntity().getLocation();

                    double dx = currentLoc.getX() - targetNode.getX();
                    double dz = currentLoc.getZ() - targetNode.getZ();

                    if ((dx * dx + dz * dz) < 1.0) {
                        agent.stopWalking();
                        ctx.positioned = true;

                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (!agent.getEntity().isValid()) return;
                            Location snapLoc = agent.getEntity().getLocation();
                            Vector lookDir = patrolData.center().toVector().subtract(snapLoc.toVector()).setY(0).normalize();
                            snapLoc.setDirection(lookDir);

                            agent.getEntity().teleport(snapLoc);
                            agent.playAnimation("initial");
                        }, 1L);

                        readyCount++;
                    } else {
                        ctx.walkTicks++;
                        if (ctx.walkTicks > 200) {
                            agent.stopWalking();

                            Location snapLoc = targetNode.clone();
                            Vector lookDir = patrolData.center().toVector().subtract(snapLoc.toVector()).setY(0).normalize();
                            snapLoc.setDirection(lookDir);
                            agent.getEntity().teleport(snapLoc);

                            ctx.positioned = true;
                            readyCount++;
                        }
                    }
                }

                if (readyCount >= activeContexts.size()) {
                    deployFuture.complete(null);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return deployFuture;
    }

    public void startAI(PerimeterGenerator.PatrolData patrolData, ConstructionProject session) {
        if (activeContexts.isEmpty()) return;

        for (Map.Entry<AgentController, AgentContext> entry : activeContexts.entrySet()) {
            AgentController agent = entry.getKey();
            AgentContext ctx = entry.getValue();

            ctx.state = AgentState.WORKING;
            ctx.waitTicks = 60 + random.nextInt(20);
            ctx.workStarted = false;
        }

        startGlobalPatrolAI(activeContexts, patrolData.track(), patrolData.center(), session);
    }

    private int getNearestNodeIndex(Location loc, List<Location> track) {
        int bestIndex = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < track.size(); i++) {
            double dist = track.get(i).distanceSquared(loc);
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void startGlobalPatrolAI(Map<AgentController, AgentContext> contextMap,
                                     List<Location> track, Location center, ConstructionProject session) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (session.isCancelled()) {
                    for (AgentController agent : contextMap.keySet()) {
                        agent.stop();
                    }
                    this.cancel();
                    return;
                }

                var iterator = contextMap.entrySet().iterator();

                while (iterator.hasNext()) {

                    var entry = iterator.next();
                    AgentController agent = entry.getKey();
                    AgentContext ctx = entry.getValue();

                    if (!agent.getEntity().isValid()) {
                        agent.stop();
                        iterator.remove();
                        continue;
                    }

                    processAgentAI(agent, ctx, track, center);
                }

                if (contextMap.isEmpty()) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void processAgentAI(AgentController agent, AgentContext ctx, List<Location> track, Location center) {
        switch (ctx.state) {
            case IDLE -> {
                if (random.nextDouble() < 0.15) ctx.direction *= -1;

                ctx.currentIndex += ctx.direction;
                if (ctx.currentIndex >= track.size()) ctx.currentIndex = 0;
                if (ctx.currentIndex < 0) ctx.currentIndex = track.size() - 1;

                Location target = track.get(ctx.currentIndex);

                agent.navigateTo(target, 1.2D);

                ctx.state = AgentState.WALKING;
                ctx.walkTicks = 0;
            }

            case WALKING -> {
                Location currentLoc = agent.getEntity().getLocation();
                Location targetLoc = track.get(ctx.currentIndex);

                double dx = currentLoc.getX() - targetLoc.getX();
                double dz = currentLoc.getZ() - targetLoc.getZ();

                if ((dx * dx + dz * dz) < 1.0) {
                    agent.stopWalking();
                    ctx.state = AgentState.WORKING;

                    ctx.waitTicks = 60;
                    ctx.workStarted = false;
                } else {
                    ctx.walkTicks++;
                    if (ctx.walkTicks > 100) {
                        ctx.state = AgentState.IDLE;
                    }
                }
            }

            case WORKING -> {
                ctx.waitTicks--;

                if (!ctx.workStarted && ctx.waitTicks == 50) {

                    Location snapLoc = agent.getEntity().getLocation();

                    Vector lookDir = center.toVector().subtract(snapLoc.toVector()).setY(0).normalize();
                    snapLoc.setDirection(lookDir);

                    agent.lookAt(lookDir);

                    agent.playAnimation("break");

                    ctx.workStarted = true;
                }

                if (ctx.waitTicks == 30) {
                    session.triggerWork();
                }

                if (ctx.waitTicks <= 0) {
                    ctx.state = AgentState.IDLE;
                }
            }
        }
    }

    private static class AgentContext {
        int currentIndex;
        int waitTicks = 0;
        int walkTicks = 0;
        int direction;
        boolean positioned = false;
        boolean workStarted = false;
        AgentState state = AgentState.IDLE;
        float lastWalkYaw = 0f;

        AgentContext(int startIndex, int direction) {
            this.currentIndex = startIndex;
            this.direction = direction;
        }
    }
}