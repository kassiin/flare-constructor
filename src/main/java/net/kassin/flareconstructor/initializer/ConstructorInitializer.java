package net.kassin.flareconstructor.initializer;

import lombok.Getter;
import net.flareplugins.core.utils.configuration.Config;
import net.kassin.flareconstructor.ConstructionMessage;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.command.BuildCommand;
import net.kassin.flareconstructor.command.CancelCommand;
import net.kassin.flareconstructor.listener.*;
import net.kassin.flareconstructor.menu.configuration.ConstructionRegistry;
import net.kassin.flareconstructor.menu.configuration.ConstructorGuiConfiguration;
import net.kassin.flareconstructor.menu.registry.MenuRegistry;
import net.kassin.flareconstructor.menu.type.MenuType;
import net.kassin.flareconstructor.menu.window.ConstructionMainMenu;
import net.kassin.flareconstructor.menu.window.ConstructionReplacementMenu;
import net.kassin.flareconstructor.menu.window.ConstructionSettingsMenu;
import net.kassin.flareconstructor.protection.WorksiteTracker;
import net.kassin.flareconstructor.schematic.SchematicAnalyzer;
import net.kassin.flareconstructor.schematic.SchematicLoader;
import net.kassin.flareconstructor.schematic.session.ProjectRegistry;

@Getter
public class ConstructorInitializer {

    private final FlareConstructorPlugin plugin;

    private final Config guiConfig;
    private final Config constructionsConfig;

    private final ConstructorGuiConfiguration guiConfiguration;
    private final ConstructionRegistry constructionRegistry;
    private ConstructionMessage message;
    private final MenuRegistry menuRegistry;

    private final ProjectRegistry projectRegistry;

    private final WorksiteTracker worksiteTracker;
    private final SchematicLoader schematicLoader;
    private final SchematicAnalyzer schematicAnalyzer;

    public ConstructorInitializer(FlareConstructorPlugin plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();

        this.guiConfig = new Config(plugin, "gui.yml");
        this.constructionsConfig = new Config(plugin, "constructions.yml");

        this.guiConfiguration = new ConstructorGuiConfiguration();
        this.constructionRegistry = new ConstructionRegistry();
        this.message = new ConstructionMessage(plugin);

        this.menuRegistry = new MenuRegistry();

        this.worksiteTracker = new WorksiteTracker();

        this.projectRegistry = new ProjectRegistry(worksiteTracker);

        this.schematicLoader = new SchematicLoader(plugin, constructionRegistry);
        this.schematicAnalyzer = new SchematicAnalyzer(plugin, schematicLoader);

        loadRegistry();
        loadAll();
        registerCommandsAndListeners();

        loadSchematicsCache();
    }

    public void reload() {
        plugin.reloadConfig();
        guiConfig.reloadDefaultConfig();
        constructionsConfig.reloadDefaultConfig();
        loadAll();
        loadRegistry();
        loadSchematicsCache();
    }

    private void loadSchematicsCache() {
        schematicLoader.scanAllowedSchematics();
        schematicAnalyzer.preloadSchematicsAsync(constructionRegistry.getAvailableBuildIds());
    }

    private void loadRegistry() {
        menuRegistry.register(MenuType.MAIN_CONSTRUCTION, new ConstructionMainMenu(this));
        menuRegistry.register(MenuType.SETTINGS, new ConstructionSettingsMenu(this, projectRegistry));
        menuRegistry.register(MenuType.REPLACEMENT, new ConstructionReplacementMenu(this));
    }

    private void loadAll() {
        guiConfiguration.load(guiConfig);
        constructionRegistry.load(constructionsConfig);
    }

    private void registerCommandsAndListeners() {
        BuildCommand buildCommand = new BuildCommand(plugin, constructionRegistry, message);
        plugin.getCommand("build").setExecutor(buildCommand);
        plugin.getCommand("build").setTabCompleter(buildCommand);

        plugin.getCommand("buildcancel").setExecutor(new CancelCommand(projectRegistry));

        plugin.getServer().getPluginManager().registerEvents(new BuildJoinListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlaceBenchBlockListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new InteractBenchListener(menuRegistry), plugin);
        plugin.getServer().getPluginManager().registerEvents(new WindowListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ReplacementInventoryListener(menuRegistry), plugin);
        plugin.getServer().getPluginManager().registerEvents(new WorksiteProtectionListener(worksiteTracker), plugin);
    }

}