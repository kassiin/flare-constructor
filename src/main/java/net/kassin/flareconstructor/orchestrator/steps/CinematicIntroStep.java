package net.kassin.flareconstructor.orchestrator.steps;

import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.api.CustomModelProvider;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.orchestrator.ConstructionContext;
import net.kassin.flareconstructor.orchestrator.ConstructionStep;
import net.kassin.flareconstructor.orchestrator.entity.AgentController;
import net.kassin.flareconstructor.orchestrator.entity.GlobalAgentManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CinematicIntroStep implements ConstructionStep {

    private final FlareConstructorPlugin plugin;
    private final GlobalAgentManager globalAgentManager;

    public CinematicIntroStep(FlareConstructorPlugin plugin, GlobalAgentManager globalAgentManager) {
        this.plugin = plugin;
        this.globalAgentManager = globalAgentManager;
    }

    @Override
    public CompletableFuture<Void> execute(ConstructionContext context) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location houseOrigin = context.getHouseOrigin();
            int agentsToSpawn = context.getBuildData().agents();

            placeHouse(houseOrigin);

            List<Location> formationSlots = calculateFormationSlots(houseOrigin, agentsToSpawn);

            startCinematicSequence(context, houseOrigin, formationSlots, future);
        });

        return future;
    }

    private List<Location> calculateFormationSlots(Location origin, int count) {
        List<Location> slots = new ArrayList<>();

        Vector forward = origin.getDirection().clone().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        Location baseTarget = origin.clone().add(forward.clone().multiply(3));

        for (int i = 0; i < count; i++) {
            double offsetMultiplier = (i == 0) ? 0 : (i % 2 != 0) ? (i + 1) / 2.0 : -(i / 2.0);

            Location slot = baseTarget.clone().add(right.clone().multiply(offsetMultiplier * 1.5));
            slot.setY(getSafeY(slot.getWorld(), slot.getBlockX(), slot.getBlockY(), slot.getBlockZ()));
            slot.setDirection(forward);

            slots.add(slot);
        }

        return slots;
    }

    private void startCinematicSequence(ConstructionContext context, Location spawnLoc, List<Location> formationSlots, CompletableFuture<Void> future) {
        int agentsToSpawn = formationSlots.size();

        Vector lookDirection = spawnLoc.getDirection().clone().setY(0).normalize();

        new BukkitRunnable() {
            int ticks = 0;
            int spawnedCount = 0;
            final Set<AgentController> arrivedAgents = new HashSet<>();

            @Override
            public void run() {
                ticks++;

                if (ticks < 60) return;

                if (arrivedAgents.size() >= agentsToSpawn) {
                    context.getPlayer().sendMessage("§a[Cinemática] Todos os agentes posicionados!");
                    future.complete(null);
                    this.cancel();
                    return;
                }

                if ((ticks - 60) % 20 == 0 && spawnedCount < agentsToSpawn) {
                    Location targetNode = formationSlots.get(spawnedCount);
                    AgentController agent = spawnAgent(spawnLoc);

                    context.getAgents().add(agent);
                    agent.navigateTo(targetNode, 1.2D);
                    spawnedCount++;
                }

                for (int i = 0; i < spawnedCount; i++) {
                    AgentController agent = context.getAgents().get(i);

                    if (arrivedAgents.contains(agent) || !agent.getEntity().isValid()) continue;

                    Location targetNode = formationSlots.get(i);
                    double distanceSq = agent.getEntity().getLocation().distanceSquared(targetNode);

                    if (distanceSq < 1.0) {
                        agent.stop();
                        agent.lookAt(lookDirection);

                        arrivedAgents.add(agent);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void placeHouse(Location location) {
        CustomModelProvider provider = FlareCorePlugin.getAPI().getCustomModelProvider();
        Entity entity = location.getWorld().spawn(location, org.bukkit.entity.ItemDisplay.class,
                display -> display.setInvisible(true));

        provider.applyModel("flare_house", entity);
        provider.runAnimation("initial", "flare_house", entity);
    }

    private AgentController spawnAgent(Location spawnLocation) {
        CustomModelProvider provider = FlareCorePlugin.getAPI().getCustomModelProvider();

        Pig entity = spawnLocation.getWorld().spawn(spawnLocation, Pig.class, mob -> {
            mob.setInvisible(true);
            mob.setCollidable(false);
            mob.setSilent(true);
            mob.setGravity(true);
            mob.setInvulnerable(true);

            mob.setAware(true);
            Bukkit.getMobGoals().removeAllGoals(mob);

            mob.getPersistentDataContainer().set(
                    new NamespacedKey("flare", "constructor"),
                    PersistentDataType.STRING,
                    "constructor_entity"
            );
        });

        provider.applyModel("flare_miner", entity);
        provider.runAnimation("initial", "flare_miner", entity);

        return new AgentController(plugin, entity, globalAgentManager);
    }

    private int getSafeY(World world, int x, int baseY, int z) {
        for (int checkY = baseY + 3; checkY >= baseY - 3; checkY--) {
            if (world.getBlockAt(x, checkY - 1, z).getType().isSolid()
                    && !world.getBlockAt(x, checkY, z).getType().isSolid()
                    && !world.getBlockAt(x, checkY + 1, z).getType().isSolid()) {
                return checkY;
            }
        }
        return baseY;
    }

}