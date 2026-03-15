package net.kassin.flareconstructor.menu.configuration;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import net.flareplugins.core.hook.itemhook.ItemFactory;
import net.flareplugins.core.utils.MiniMessageProvider;
import net.flareplugins.core.utils.items.ItemBuilder;
import net.flareplugins.core.utils.window.InventoryLayoutParser;
import net.flareplugins.core.utils.window.PaginationTheme;
import net.kassin.flareconstructor.ConstructionMessage;
import net.kassin.flareconstructor.menu.configuration.settings.MainMenuSettings;
import net.kassin.flareconstructor.menu.configuration.settings.ReplacementMenuSettings;
import net.kassin.flareconstructor.menu.configuration.settings.SettingsMenuSettings;
import net.kassin.flareconstructor.menu.theme.ConstructorPaginationTheme;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
public class ConstructorGuiConfiguration {

    private MainMenuSettings mainSettings;
    private SettingsMenuSettings settingsMenuSettings;
    private ReplacementMenuSettings replacementSettings;

    private FileConfiguration config;
    private PaginationTheme paginationTheme;

    public void load(FileConfiguration config, ConstructionMessage message) {
        this.config = config;
        this.paginationTheme = new ConstructorPaginationTheme();

        this.mainSettings = new MainMenuSettings(
                MiniMessageProvider.get().deserialize(config.getString("gui.titles.constructions", "<dark_gray>Menu de Construções</dark_gray>")),
                InventoryLayoutParser.parse(config.getStringList("gui.layouts.constructions"))
        );

        this.settingsMenuSettings = new SettingsMenuSettings(
                MiniMessageProvider.get().deserialize(config.getString("gui.titles.settings", "<dark_gray>Configuração da Obra</dark_gray>")),
                InventoryLayoutParser.parse(config.getStringList("gui.layouts.settings"))
        );

        this.replacementSettings = new ReplacementMenuSettings(
                MiniMessageProvider.get().deserialize(config.getString("gui.titles.replacement", "<dark_gray>Substituir Blocos</dark_gray>")),
                InventoryLayoutParser.parse(config.getStringList("gui.layouts.replacement")),
                MiniMessageProvider.get().deserialize(config.getString("gui.buttons.replacement.back.name", "<red>Voltar")),
                config.getString("gui.buttons.replacement.item.name_selected", "<green>» <material>"),
                config.getString("gui.buttons.replacement.item.name_unselected", "<yellow><material>"),
                config.getStringList("gui.buttons.replacement.item.lore_base"),
                config.getStringList("gui.buttons.replacement.item.lore_selected"),
                config.getStringList("gui.buttons.replacement.item.lore_unselected")
        );
    }

    public ItemStack getBuildButton(String buildId, ItemFactory factory, ConstructionMessage message) {
        String path = "gui.buttons.builds." + buildId;
        ConfigurationSection section = config.getConfigurationSection(path);

        if (section == null) {
            return ItemBuilder.builder(XMaterial.BARRIER)
                    .name("<red>Build não configurada no GUI: " + buildId)
                    .build();
        }

        ItemStack item = factory.createItem(section);

        if (item == null) {
            return ItemBuilder.builder(XMaterial.BARRIER)
                    .name("<red>Erro ao carregar item para: " + buildId)
                    .build();
        }

        ItemBuilder builder = ItemBuilder.builder(item);

        String rawName = section.getString("name", "");
        if (!rawName.isEmpty()) {
            builder.nameComponent(message.process(rawName));
        }

        List<String> rawLore = section.getStringList("lore");
        if (!rawLore.isEmpty()) {
            builder.loreComponent(rawLore.stream()
                    .map(message::process)
                    .toList());
        }

        return builder.build();
    }
}