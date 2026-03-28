package net.kassin.flareconstructor.orchestrator.entity;

import lombok.Getter;
import lombok.Setter;
import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.api.CustomModelProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Pig;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class AgentController {

    private final Plugin plugin;

    @Getter
    private final Pig entity;

    @Getter
    @Setter
    private boolean cycling = false;

    @Getter
    @Setter
    private AgentState state = AgentState.IDLE;

    private Location currentTarget;
    private double vectorSpeed;

    private String currentAnimation = "";

    // Pré-alocados para performance O(1)
    private final Location reusableLocation = new Location(null, 0, 0, 0);
    private final Vector reusableVector = new Vector(0, 0, 0);

    private final GlobalAgentManager agentManager;

    public AgentController(Plugin plugin, Pig entity, GlobalAgentManager agentManager) {
        this.plugin = plugin;
        this.entity = entity;
        this.agentManager = agentManager;
        this.entity.setAware(true);
        agentManager.register(this);
    }

    public void navigateTo(Location target, double speed) {
        if (!entity.isValid()) return;

        this.state = AgentState.WALKING;
        this.currentTarget = target;
        this.vectorSpeed = speed * 0.15;

        entity.getLocation(reusableLocation);
        reusableVector.setX(target.getX() - reusableLocation.getX());
        reusableVector.setY(0);
        reusableVector.setZ(target.getZ() - reusableLocation.getZ());

        if (reusableVector.lengthSquared() > 0) {
            lookAt(reusableVector);
        }

        playAnimation("walk");
    }

    public void tickNavigationLogic() {
        if (state != AgentState.WALKING || currentTarget == null || !entity.isValid()) return;

        entity.getLocation(reusableLocation);

        double dx = currentTarget.getX() - reusableLocation.getX();
        double dz = currentTarget.getZ() - reusableLocation.getZ();
        double distanceSq = dx * dx + dz * dz;

        if (distanceSq > 0.1) {
            double distance = Math.sqrt(distanceSq);

            double velX = (dx / distance) * vectorSpeed;
            double velZ = (dz / distance) * vectorSpeed;

            // Aplica Velocidade
            reusableVector.setX(velX).setY(entity.getVelocity().getY()).setZ(velZ);
            entity.setVelocity(reusableVector);

            // Delega a rotação de movimento para o ModelEngine usando os vetores do Spigot
            reusableVector.setX(dx).setY(0).setZ(dz);
            lookAt(reusableVector);
        }
    }

    public void lookAt(Vector direction) {
        if (!entity.isValid() || direction.lengthSquared() == 0) {
          //  Bukkit.getLogger().info("return: lookAt");
            return;
        }

     //   Bukkit.getLogger().info("calling: lookAt");

        reusableLocation.setDirection(direction);
        float yaw = reusableLocation.getYaw();

        FlareCorePlugin.getAPI().getCustomModelProvider().setRotation(entity, yaw);

        entity.setRotation(yaw, 0);
    }

    public void playAnimation(String animationId) {
        if (!entity.isValid()) return;
        if (currentAnimation.equals(animationId)) return;

        CustomModelProvider provider = FlareCorePlugin.getAPI().getCustomModelProvider();
        if (!currentAnimation.isEmpty()) {
            provider.stopAnimation(currentAnimation, "flare_miner", entity);
        }

        provider.runAnimation(animationId, "flare_miner", entity);
        this.currentAnimation = animationId;
    }

    public void stopWalking() {
        if (entity.isValid()) {
            entity.setVelocity(new Vector(0, entity.getVelocity().getY(), 0));
        }
    }

    public void stop() {
        this.cycling = false;
        this.state = AgentState.IDLE;
        this.currentTarget = null;
        stopWalking();
        playAnimation("initial");
    }

    public void destroy() {
        stop();
        if (entity.isValid()) entity.remove();
        agentManager.unregister(this);
    }
}