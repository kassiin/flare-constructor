package net.kassin.flareconstructor.menu.window;

import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.api.FlareItemFactory;
import net.flareplugins.core.hook.itemhook.ItemFactory;
import net.flareplugins.core.utils.window.PaginatedWindow;
import net.flareplugins.core.utils.window.WindowButton;
import net.flareplugins.core.utils.window.WindowSize;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import net.kassin.flareconstructor.menu.configuration.settings.MainMenuSettings;
import net.kassin.flareconstructor.menu.context.MenuContext;
import net.kassin.flareconstructor.menu.type.MenuType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ConstructionMainMenu extends AbstractConstructionMenu {

    public ConstructionMainMenu(ConstructorInitializer initializer) {
        super(initializer);
    }

    @Override
    public void open(MenuContext context) {

        Player player = context.player();
        Location benchLocation = context.benchLocation();
        FlareItemFactory itemFactory = FlareCorePlugin.getAPI().getItemFactory();

        MainMenuSettings cfg = guiConfig.getMainSettings();

        PaginatedWindow paginatedWindow = new PaginatedWindow(
                WindowSize.ROW_6,
                cfg.title(),
                cfg.layout(),
                guiConfig.getPaginationTheme()
        );

        paginatedWindow.setSpecialButton('O', new WindowButton(new ItemStack(Material.AIR)));

        List<WindowButton> buttons = new ArrayList<>();

        for (String id : registry.getAvailableBuildIds()) {

            ItemStack icon = guiConfig.getBuildButton(id, itemFactory, message);

            buttons.add(new WindowButton(icon).addAction((click, p) -> {
                AbstractConstructionMenu settingsMenu = menuRegistry.get(MenuType.SETTINGS);
                settingsMenu.open(MenuContext.create(p, id, benchLocation));
            }));

        }
        paginatedWindow.setItems(buttons);
        paginatedWindow.viewPaginated(player);
    }

}