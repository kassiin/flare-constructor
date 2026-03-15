package net.kassin.flareconstructor.menu.registry;

import net.kassin.flareconstructor.menu.window.AbstractConstructionMenu;
import net.kassin.flareconstructor.menu.type.MenuType;

import java.util.EnumMap;
import java.util.Map;

public class MenuRegistry {
    private final Map<MenuType, AbstractConstructionMenu> menus = new EnumMap<>(MenuType.class);

    public void register(MenuType type, AbstractConstructionMenu menu) {
        menus.put(type, menu);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractConstructionMenu> T get(MenuType type) {
        AbstractConstructionMenu menu = menus.get(type);
        if (menu == null) {
            throw new IllegalStateException("O menu " + type + " não foi registrado no Initializer!");
        }
        return (T) menu;
    }

}