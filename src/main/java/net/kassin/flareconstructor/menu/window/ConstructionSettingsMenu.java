package net.kassin.flareconstructor.menu.window;

import com.cryptomorin.xseries.XMaterial;
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
import net.kassin.flareconstructor.schematic.SchematicBuilder;
import net.kassin.flareconstructor.schematic.section.BuildSession;
import net.kassin.flareconstructor.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConstructionSettingsMenu extends AbstractConstructionMenu {

    private final Map<UUID, BuildData> editingData = new ConcurrentHashMap<>();

    public ConstructionSettingsMenu(ConstructorInitializer initializer) {
        super(initializer);
    }

    @Override
    public void open(MenuContext context) {
        Player player = context.player();

        if (context.data() != null) {
            editingData.put(player.getUniqueId(), context.data());
        } else if (context.buildId() != null && context.benchLocation() != null) {
            BuildData existing = editingData.get(player.getUniqueId());
            if (existing == null || !existing.id().equals(context.buildId()) || !existing.benchLocation().equals(context.benchLocation())) {
                editingData.put(player.getUniqueId(), new BuildData(context.buildId(), 1, 100, 1, context.benchLocation(), new HashMap<>()));
            }
        }

        render(player);
    }

    public void syncData(Player player, BuildData data) {
        editingData.put(player.getUniqueId(), data);
    }

    private void render(Player player) {
        BuildData currentData = editingData.get(player.getUniqueId());
        if (currentData == null) return;

        SettingsMenuSettings cfg = guiConfig.getSettingsMenuSettings();

        PaginatedWindow window = new PaginatedWindow(
                WindowSize.ROW_6,
                cfg.title(),
                cfg.layout(),
                guiConfig.getPaginationTheme()
        );

        window.setSpecialButton('O', new WindowButton(new ItemStack(Material.AIR)));

        ItemStack delayIcon = ItemBuilder.builder(XMaterial.CLOCK)
                .nameComponent(message.process("<yellow>Delay (Ticks): <green>" + currentData.delayTicks()))
                .loreComponent(
                        List.of(message.process(""),
                                message.process("<green>Botão Esquerdo para Aumentar (+1)"),
                                message.process("<red>Botão Direito para Diminuir (-1)")
                        )
                ).build();

        window.setSpecialButton('D', new WindowButton(delayIcon).addAction((click, p) -> {
            int newVal = currentData.delayTicks();
            if (click.isLeftClick()) newVal++;
            else if (click.isRightClick()) newVal = Math.max(1, newVal - 1);
            updateAndRender(p, new BuildData(currentData.id(), newVal, currentData.blocksPerTick(), currentData.agents(), currentData.benchLocation(), currentData.replacements()));
        }));

        ItemStack blocksIcon = ItemBuilder.builder(XMaterial.BRICKS)
                .nameComponent(message.process("<yellow>Blocos por Tick: <green>" + currentData.blocksPerTick()))
                .loreComponent(
                        List.of(
                                message.process(""),
                                message.process("<green>Botão Esquerdo para Aumentar (+10)"),
                                message.process("<red>Botão Direito para Diminuir (-10)"))
                ).build();

        window.setSpecialButton('B', new WindowButton(blocksIcon).addAction((click, p) -> {
            int newVal = currentData.blocksPerTick();
            if (click.isLeftClick()) newVal += 10;
            else if (click.isRightClick()) newVal = Math.max(1, newVal - 10);
            updateAndRender(p, new BuildData(currentData.id(), currentData.delayTicks(), newVal, currentData.agents(), currentData.benchLocation(), currentData.replacements()));
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
            updateAndRender(p, new BuildData(currentData.id(), currentData.delayTicks(), currentData.blocksPerTick(), newVal, currentData.benchLocation(), currentData.replacements()));
        }));

        ItemStack replaceIcon = ItemBuilder.builder(XMaterial.CRAFTING_TABLE)
                .nameComponent(message.process("<yellow>Substituir Materiais"))
                .loreComponent(List.of(message.process("<gray>Clique para alterar os blocos.")))
                .build();

        window.setSpecialButton('R', new WindowButton(replaceIcon).addAction((click, p) -> {
            ConstructionReplacementMenu replaceMenu = menuRegistry.get(MenuType.REPLACEMENT);
            replaceMenu.open(MenuContext.create(p, currentData));
        }));

        ItemStack confirmIcon = ItemBuilder.builder(XMaterial.EMERALD_BLOCK)
                .nameComponent(message.process("<green><bold>CONFIRMAR CONSTRUÇÃO"))
                .loreComponent(List.of(message.process("<gray>Clique para iniciar a obra!")))
                .build();

        window.setSpecialButton('C', new WindowButton(confirmIcon).addAction((click, p) -> {
            p.closeInventory();
            editingData.remove(p.getUniqueId());

            Location origin = LocationUtils.getGridAlignedOrigin(currentData.benchLocation(), 5);

            BuildSession session = BuildSession.create(p.getUniqueId());
            session.getViewers().add(p.getUniqueId());

            p.sendMessage("§e[Debug] Iniciando pipeline de construção...");

            CompletableFuture<Void> future = SchematicBuilder.build(
                    FlareConstructorPlugin.getInstance(),
                    currentData,
                    origin,
                    session,
                    () -> p.sendMessage("§a[Debug] Fase 1 (Visual) concluída!"),
                    () -> {
                        p.sendMessage("§a[Debug] Construção totalmente finalizada!");
                        BuildSession.remove(p.getUniqueId());
                    },
                    err -> {
                        p.sendMessage("§c[ERRO GRAVE] Falha na construção: " + err);
                        System.out.println("[FlareConstructor] Erro no SchematicBuilder: " + err);
                        BuildSession.remove(p.getUniqueId());
                    }
            );

            session.setFuture(future);
        }));

        window.viewPaginated(player);
    }

    private void updateAndRender(Player player, BuildData newData) {
        editingData.put(player.getUniqueId(), newData);
        render(player);
    }
}