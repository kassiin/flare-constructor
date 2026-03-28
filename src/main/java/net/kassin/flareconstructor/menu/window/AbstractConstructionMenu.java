package net.kassin.flareconstructor.menu.window;

import net.flareplugins.core.api.FlareItemFactory;
import net.flareplugins.core.hook.itemhook.ItemFactory;
import net.kassin.flareconstructor.ConstructionMessage;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import net.kassin.flareconstructor.menu.configuration.ConstructionRegistry;
import net.kassin.flareconstructor.menu.configuration.ConstructorGuiConfiguration;
import net.kassin.flareconstructor.menu.context.MenuContext;
import net.kassin.flareconstructor.menu.registry.MenuRegistry;
import net.kassin.flareconstructor.protection.WorksiteTracker;
import net.kassin.flareconstructor.schematic.SchematicAnalyzer;
import net.kassin.flareconstructor.schematic.SchematicLoader;

public abstract class AbstractConstructionMenu {

    protected final ConstructorGuiConfiguration guiConfig;
    protected final ConstructionRegistry registry;
    protected final ConstructionMessage message;
    protected final FlareItemFactory itemFactory;
    protected final MenuRegistry menuRegistry;
    protected final SchematicLoader schematicLoader;
    protected final SchematicAnalyzer schematicAnalyzer;

    protected final WorksiteTracker worksiteTracker;

    public AbstractConstructionMenu(ConstructorInitializer initializer) {
        this.guiConfig = initializer.getGuiConfiguration();
        this.registry = initializer.getConstructionRegistry();
        this.message = initializer.getMessage();
        this.menuRegistry = initializer.getMenuRegistry();
        this.schematicLoader = initializer.getSchematicLoader();
        this.schematicAnalyzer = initializer.getSchematicAnalyzer();

        this.worksiteTracker = initializer.getWorksiteTracker();

        this.itemFactory = net.flareplugins.core.FlareCorePlugin.getAPI().getItemFactory();
    }

    public abstract void open(MenuContext context);

}