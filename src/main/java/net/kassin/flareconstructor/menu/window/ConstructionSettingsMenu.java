package net.kassin.flareconstructor.menu.window;

import com.cryptomorin.xseries.XMaterial;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.utils.items.ItemBuilder;
import net.flareplugins.core.utils.window.PaginatedWindow;
import net.flareplugins.core.utils.window.WindowButton;
import net.flareplugins.core.utils.window.WindowSize;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import net.kassin.flareconstructor.menu.configuration.BuildData;
import net.kassin.flareconstructor.menu.configuration.settings.SettingsMenuSettings;
import net.kassin.flareconstructor.menu.context.MenuContext;
import net.kassin.flareconstructor.menu.type.MenuType;
import net.kassin.flareconstructor.orchestrator.BuildOrchestrator;
import net.kassin.flareconstructor.orchestrator.ConstructionContext;
import net.kassin.flareconstructor.schematic.SchematicAnalyzer;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import net.kassin.flareconstructor.schematic.session.ProjectRegistry;
import net.kassin.flareconstructor.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConstructionSettingsMenu extends AbstractConstructionMenu {

    private final Cache<UUID, BuildData> editingData = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    private final ProjectRegistry projectRegistry;

    public ConstructionSettingsMenu(ConstructorInitializer initializer, ProjectRegistry projectRegistry) {
        super(initializer);
        this.projectRegistry = projectRegistry;
    }

    @Override
    public void open(MenuContext context) {
        Player player = context.player();

        if (context.data() != null) {
            editingData.put(player.getUniqueId(), context.data());
        } else if (context.buildId() != null && context.benchLocation() != null) {
            BuildData existing = editingData.getIfPresent(player.getUniqueId());
            if (existing == null ||
                    !existing.id().equals(context.buildId()) ||
                    !existing.benchLocation().equals(context.benchLocation())) {
                editingData.put(player.getUniqueId(), new BuildData(context.buildId(),
                        1, 1, context.benchLocation(), new HashMap<>()));
            }
        }

        render(player);
    }

    public void syncData(Player player, BuildData data) {
        editingData.put(player.getUniqueId(), data);
    }

    private void render(Player player) {

        BuildData currentData = editingData.getIfPresent(player.getUniqueId());

        if (currentData == null) return;

        FlareCorePlugin.getAPI().getAsyncAPI().supply(() ->
                schematicAnalyzer.getStats(currentData.id())
        ).exceptionally(ex -> {
            FlareConstructorPlugin.getInstance().getLogger().severe("Falha ao analisar blocos da casa " + currentData.id());
            return new SchematicAnalyzer.SchematicStats(1, 0);
        }).thenAccept(stats -> {

            Bukkit.getScheduler().runTask(FlareConstructorPlugin.getInstance(), () -> {

                SettingsMenuSettings cfg = guiConfig.getSettingsMenuSettings();

                int totalBlocks = stats.totalBlocks();
                int baseBlocks = stats.baseBlocks();

                PaginatedWindow window = new PaginatedWindow(
                        WindowSize.ROW_6,
                        cfg.title(),
                        cfg.layout(),
                        guiConfig.getPaginationTheme()
                );

                window.setSpecialButton('O', new WindowButton(new ItemStack(Material.AIR)));

                ItemStack blocksIcon = ItemBuilder.builder(XMaterial.BRICKS)
                        .nameComponent(message.process("<yellow>Blocos por Marretada: <green>" + currentData.blocksPerStrike()))
                        .loreComponent(
                                List.of(
                                        message.process(""),
                                        message.process("<green>Botão Esquerdo para Aumentar (+1)"),
                                        message.process("<red>Botão Direito para Diminuir (-1)"))
                        ).build();

                window.setSpecialButton('B', new WindowButton(blocksIcon).addAction((click, p) -> {
                    int newVal = currentData.blocksPerStrike();
                    if (click.isLeftClick()) newVal = Math.min(10, newVal + 1);
                    else if (click.isRightClick()) newVal = Math.max(1, newVal - 1);
                    updateAndRender(p, new BuildData(currentData.id(), newVal, currentData.agents(), currentData.benchLocation(), currentData.replacements()));
                }));

                ItemStack agentsIcon = ItemBuilder.builder(XMaterial.VILLAGER_SPAWN_EGG)
                        .nameComponent(message.process("<yellow>Quantidade de Agentes: <green>" + currentData.agents()))
                        .loreComponent(List.of(
                                message.process(""),
                                message.process("<green>Botão Esquerdo para Aumentar (+1)"),
                                message.process("<red>Botão Direito para Diminuir (-1)"))
                        ).build();

                window.setSpecialButton('A', new WindowButton(agentsIcon).addAction((click, p) -> {
                    int newVal = currentData.agents();
                    if (click.isLeftClick()) newVal = Math.min(5, newVal + 1);
                    else if (click.isRightClick()) newVal = Math.max(1, newVal - 1);

                    updateAndRender(p, new BuildData(currentData.id(),
                            currentData.blocksPerStrike(),
                            newVal,
                            currentData.benchLocation(),
                            currentData.replacements()));
                }));

                ItemStack replaceIcon = ItemBuilder.builder(XMaterial.CRAFTING_TABLE)
                        .nameComponent(message.process("<yellow>Substituir Materiais"))
                        .loreComponent(List.of(message.process("<gray>Clique para alterar os blocos.")))
                        .build();

                window.setSpecialButton('R', new WindowButton(replaceIcon).addAction((click, p) -> {
                    ConstructionReplacementMenu replaceMenu = menuRegistry.get(MenuType.REPLACEMENT);
                    replaceMenu.open(MenuContext.create(p, currentData));
                }));

                String estimatedTime = TimeUtils.getEstimatedTime(totalBlocks, baseBlocks,
                        currentData.agents(), currentData.blocksPerStrike());

                ItemStack confirmIcon = ItemBuilder.builder(XMaterial.EMERALD_BLOCK)
                        .nameComponent(message.process("<green><bold>CONFIRMAR CONSTRUÇÃO"))
                        .loreComponent(List.of(
                                message.process("<gray>Clique para iniciar a obra!"),
                                message.process(""),
                                message.process("<yellow>Blocos Totais: <white>" + totalBlocks),
                                message.process("<yellow>Tempo estimado: <white>" + estimatedTime)
                        ))
                        .build();

                window.setSpecialButton('C', new WindowButton(confirmIcon).addAction((click, p) -> {
                    p.closeInventory();
                    editingData.invalidate(p.getUniqueId());

                    Location benchLocation = currentData.benchLocation();
                    Location reference = benchLocation.clone();
                    reference.setPitch(0f);
                    reference.setYaw(reference.getYaw() + 90f);

                    Vector directionVector = reference.getDirection().normalize();

                    Location houseLocation = benchLocation.clone().add(directionVector.clone().multiply(5));
                    Location pathFinderTarget = houseLocation.clone().add(directionVector.clone().multiply(5));

                    ConstructionProject session = projectRegistry.createProject(p.getUniqueId());
                    session.getViewers().add(p.getUniqueId());

                    p.sendMessage("§e[Debug] Iniciando pipeline de construção...");

                    ConstructionContext context = new ConstructionContext(
                            currentData,
                            benchLocation,
                            houseLocation,
                            pathFinderTarget,
                            session,
                            p);

                    BuildOrchestrator orchestrator = new BuildOrchestrator(
                            FlareConstructorPlugin.getInstance(),
                            projectRegistry,
                            schematicLoader,
                            worksiteTracker
                    );

                    orchestrator.start(context);
                }));

                window.viewPaginated(player);

            });
        });
    }

    private void updateAndRender(Player player, BuildData newData) {
        editingData.put(player.getUniqueId(), newData);
        render(player);
    }

}