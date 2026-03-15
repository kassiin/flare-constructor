package net.kassin.flareconstructor.menu.window;

import com.cryptomorin.xseries.XMaterial;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.async.FlareCoreAsyncExecutor;
import net.flareplugins.core.utils.items.ItemBuilder;
import net.flareplugins.core.utils.window.PaginatedWindow;
import net.flareplugins.core.utils.window.WindowButton;
import net.flareplugins.core.utils.window.WindowSize;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import net.kassin.flareconstructor.menu.configuration.BuildData;
import net.kassin.flareconstructor.menu.configuration.settings.ReplacementMenuSettings;
import net.kassin.flareconstructor.menu.context.MenuContext;
import net.kassin.flareconstructor.menu.type.MenuType;
import net.kassin.flareconstructor.schematic.SchematicLoader;
import net.kassin.flareconstructor.schematic.SchematicRemapper;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConstructionReplacementMenu extends AbstractConstructionMenu {

    private final Map<UUID, BuildData> editingData = new ConcurrentHashMap<>();
    private final Map<String, List<Material>> materialCache = new ConcurrentHashMap<>();
    private final Map<UUID, Material> selectingMaterial = new ConcurrentHashMap<>();
    private final Set<UUID> refreshing = ConcurrentHashMap.newKeySet();

    public ConstructionReplacementMenu(ConstructorInitializer initializer) {
        super(initializer);
    }

    @Override
    public void open(MenuContext context) {
        Player player = context.player();
        BuildData data = context.data();

        if (data == null) return;

        editingData.put(player.getUniqueId(), data);
        selectingMaterial.remove(player.getUniqueId());

        if (materialCache.containsKey(data.id())) {
            render(player, true);
        } else {

            FlareCorePlugin.getAPI().getAsyncAPI().run(() -> {
                Clipboard clipboard = SchematicLoader.load(FlareConstructorPlugin.getInstance(), data.id());
                List<Material> mats = SchematicRemapper.getUniqueMaterials(clipboard);
                materialCache.put(data.id(), mats);

                FlareConstructorPlugin.getInstance().getServer().getScheduler().runTask(
                        FlareConstructorPlugin.getInstance(), () -> render(player, true)
                );
            }).exceptionally(ex -> {
                FlareConstructorPlugin.getInstance().getServer().getScheduler().runTask(
                        FlareConstructorPlugin.getInstance(), () -> {
                            ex.printStackTrace();
                        }
                );
                return null;
            });
        }
    }

    public boolean isRefreshing(Player player) {
        return refreshing.contains(player.getUniqueId());
    }

    public boolean isSelecting(Player player) {
        return selectingMaterial.containsKey(player.getUniqueId());
    }

    public void cancelSelection(Player player) {
        selectingMaterial.remove(player.getUniqueId());
    }

    public void applyReplacement(Player player, Material newMaterial) {
        Material oldMaterial = selectingMaterial.remove(player.getUniqueId());
        if (oldMaterial == null) return;

        BuildData currentData = editingData.get(player.getUniqueId());
        if (currentData == null) return;

        Map<Material, Material> newReplacements = new HashMap<>(currentData.replacements());
        newReplacements.put(oldMaterial, newMaterial);

        BuildData newData = new BuildData(
                currentData.id(),
                currentData.delayTicks(),
                currentData.blocksPerTick(),
                currentData.agents(),
                currentData.benchLocation(),
                newReplacements
        );

        editingData.put(player.getUniqueId(), newData);

        ConstructionSettingsMenu settings = menuRegistry.get(MenuType.SETTINGS);
        settings.syncData(player, newData);

        render(player, false);
    }

    private void render(Player player, boolean forceNew) {
        BuildData currentData = editingData.get(player.getUniqueId());
        if (currentData == null) return;

        ReplacementMenuSettings cfg = guiConfig.getReplacementSettings();
        PaginatedWindow window;

        if (!forceNew && player.getOpenInventory().getTopInventory().getHolder() instanceof PaginatedWindow openWindow) {
            window = openWindow;
            window.specialButtons.clear();
        } else {
            window = new PaginatedWindow(
                    WindowSize.ROW_6,
                    cfg.title(),
                    cfg.layout(),
                    guiConfig.getPaginationTheme()
            );
        }

        window.setSpecialButton('O', new WindowButton(new ItemStack(Material.AIR)));

        ItemStack backIcon = ItemBuilder.builder(XMaterial.ARROW)
                .nameComponent(cfg.backName())
                .build();

        window.setSpecialButton('E', new WindowButton(backIcon).addAction((click, p) -> {
            selectingMaterial.remove(p.getUniqueId());
            ConstructionSettingsMenu settings = menuRegistry.get(MenuType.SETTINGS);
            settings.open(MenuContext.create(p, currentData));
        }));

        List<WindowButton> buttons = new ArrayList<>();
        List<Material> schematicMats = materialCache.getOrDefault(currentData.id(), Collections.emptyList());
        Material currentlySelecting = selectingMaterial.get(player.getUniqueId());

        for (Material mat : schematicMats) {
            Material replacedWith = currentData.replacements().getOrDefault(mat, mat);
            boolean isSelected = (currentlySelecting == mat);

            String rawName = isSelected ? cfg.nameSelected() : cfg.nameUnselected();
            rawName = rawName.replace("<material>", mat.name());

            List<Component> lore = new ArrayList<>();

            for (String line : cfg.loreBase()) {
                lore.add(message.process(line.replace("<current_material>", replacedWith.name())));
            }

            List<String> conditionalLore = isSelected ? cfg.loreSelected() : cfg.loreUnselected();
            for (String line : conditionalLore) {
                lore.add(message.process(line));
            }

            ItemStack icon = ItemBuilder.builder(XMaterial.matchXMaterial(replacedWith))
                    .nameComponent(message.process(rawName))
                    .loreComponent(lore)
                    .build();

            buttons.add(new WindowButton(icon).addAction((click, p) -> {
                if (isSelected) {
                    selectingMaterial.remove(p.getUniqueId());
                } else {
                    selectingMaterial.put(p.getUniqueId(), mat);
                }
                render(p, false);
            }));
        }
        window.setItems(buttons);

        refreshing.add(player.getUniqueId());
        try {
            window.viewPaginated(player);
        } finally {
            refreshing.remove(player.getUniqueId());
        }
    }

}