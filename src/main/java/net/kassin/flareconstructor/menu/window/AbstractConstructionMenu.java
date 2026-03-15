package net.kassin.flareconstructor.menu.window;

import net.flareplugins.core.hook.itemhook.ItemFactory;
import net.kassin.flareconstructor.ConstructionMessage;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import net.kassin.flareconstructor.menu.configuration.ConstructionRegistry;
import net.kassin.flareconstructor.menu.configuration.ConstructorGuiConfiguration;
import net.kassin.flareconstructor.menu.context.MenuContext;
import net.kassin.flareconstructor.menu.registry.MenuRegistry;

public abstract class AbstractConstructionMenu {
    protected final ConstructorGuiConfiguration guiConfig;
    protected final ConstructionRegistry registry;
    protected final ConstructionMessage message;
    protected final ItemFactory itemFactory;
    protected final MenuRegistry menuRegistry;

    public AbstractConstructionMenu(ConstructorInitializer initializer) {
        this.guiConfig = initializer.getGuiConfiguration();
        this.registry = initializer.getConstructionRegistry();
        this.message = initializer.getMessage();
        this.menuRegistry = initializer.getMenuRegistry();

        this.itemFactory = net.flareplugins.core.FlareCorePlugin.getAPI().getItemFactory();
    }

    public abstract void open(MenuContext context);

}