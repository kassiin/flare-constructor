package net.kassin.flareconstructor;

import lombok.Getter;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import net.kassin.flareconstructor.orchestrator.entity.GlobalAgentManager;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class FlareConstructorPlugin extends JavaPlugin {

    @Getter
    private static FlareConstructorPlugin instance;
    @Getter
    private ConstructorInitializer initializer;
    private GlobalAgentManager globalAgentManager;

    @Override
    public void onEnable() {
        reloadConfig();
        instance = this;
        this.initializer = new ConstructorInitializer(this);
        globalAgentManager = new GlobalAgentManager(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("[Flare] Iniciando protocolo de limpeza de obras (Shutdown)...");

        if (initializer != null && initializer.getProjectRegistry() != null) {

            for (ConstructionProject session : initializer.getProjectRegistry().getAllActive()) {

                session.cancel();

                session.clearVisualSnapshot();

                if (session.getWorksiteBounds() != null) {
                    initializer.getWorksiteTracker().untrackWorksite(session.getWorksiteBounds());
                }
            }
            getLogger().info("[Flare] Todas as obras foram canceladas em segurança e os hologramas removidos.");
        }
    }

}