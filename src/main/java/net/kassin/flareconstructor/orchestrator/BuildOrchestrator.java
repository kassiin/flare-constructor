package net.kassin.flareconstructor.orchestrator;

import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.orchestrator.entity.AgentController;
import net.kassin.flareconstructor.orchestrator.steps.CinematicIntroStep;
import net.kassin.flareconstructor.orchestrator.steps.CoreBuildStep;
import net.kassin.flareconstructor.orchestrator.steps.PatrolStep;
import net.kassin.flareconstructor.orchestrator.steps.PreparationStep;
import net.kassin.flareconstructor.protection.WorksiteBounds;
import net.kassin.flareconstructor.protection.WorksiteTracker;
import net.kassin.flareconstructor.schematic.SchematicLoader;
import net.kassin.flareconstructor.schematic.session.ProjectRegistry;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class BuildOrchestrator {

    private final FlareConstructorPlugin plugin;
    private final List<ConstructionStep> steps = new ArrayList<>();
    private final ProjectRegistry projectRegistry;

    public BuildOrchestrator(FlareConstructorPlugin plugin,
                             ProjectRegistry projectRegistry,
                             SchematicLoader schematicLoader,
                             WorksiteTracker worksiteTracker) {
        this.plugin = plugin;
        this.projectRegistry = projectRegistry;

        steps.add(new CinematicIntroStep(plugin, plugin.getGlobalAgentManager()));
        steps.add(new PreparationStep(schematicLoader, worksiteTracker));
        steps.add(new PatrolStep(plugin));
        steps.add(new CoreBuildStep(plugin));
    }

    public void start(ConstructionContext context) {
        executeStep(0, context);
    }

    private void executeStep(int index, ConstructionContext context) {
        if (index >= steps.size() || context.getSession().isCancelled()) {

            Bukkit.getLogger().info("§a[Flare] §fObra concluída com sucesso!");
            context.getPlayer().sendMessage("§a[Flare] §fObra concluída com sucesso!");

            if (context.getBarrierFence() != null) {
                context.getBarrierFence().remove();
            }

            context.getAgents().forEach(AgentController::destroy);
            projectRegistry.removeProject(context.getPlayer().getUniqueId());
            return;
        }

        String stepMessage = switch (index) {
            case 0 -> "§e[1/4] §fPosicionando a equipe de construção...";
            case 1 -> "§e[2/4] §fPreparando o terreno e calculando a planta...";
            case 2 -> "§e[3/4] §fIsolando a área e iniciando a patrulha...";
            case 3 -> "§e[4/4] §fConstrução em andamento! Subindo os blocos...";
            default -> "§eEtapa " + (index + 1) + "...";
        };

        if (index == 3) {
            Bukkit.getLogger().info("iniciou");
        }

        context.getPlayer().sendMessage(stepMessage);

        ConstructionStep step = steps.get(index);

        step.execute(context).whenComplete((result, ex) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    context.getPlayer().sendMessage("§c[Flare] Erro fatal durante a construção: " + ex.getMessage());
                    return;
                }
                executeStep(index + 1, context);
            });
        });
    }

}