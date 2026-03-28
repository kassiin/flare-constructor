package net.kassin.flareconstructor.orchestrator.entity;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GlobalAgentManager extends BukkitRunnable {

    private final Set<AgentController> activeAgents = new HashSet<>();

    public GlobalAgentManager(Plugin plugin) {
        this.runTaskTimer(plugin, 0L, 4L);
    }

    public void register(AgentController agent) {
        activeAgents.add(agent);
    }

    public void unregister(AgentController agent) {
        activeAgents.remove(agent);
    }

    @Override
    public void run() {
        Iterator<AgentController> iterator = activeAgents.iterator();

        while (iterator.hasNext()) {
            AgentController agent = iterator.next();

            if (!agent.getEntity().isValid()) {
                iterator.remove();
                continue;
            }

            agent.tickNavigationLogic();
        }

    }
}