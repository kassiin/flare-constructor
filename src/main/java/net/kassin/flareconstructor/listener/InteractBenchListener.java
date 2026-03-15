package net.kassin.flareconstructor.listener;

import net.kassin.flareconstructor.menu.context.MenuContext;
import net.kassin.flareconstructor.menu.registry.MenuRegistry;
import net.kassin.flareconstructor.menu.type.MenuType;
import net.kassin.flareconstructor.menu.window.AbstractConstructionMenu;
import net.kassin.flareconstructor.menu.window.ConstructionMainMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public record InteractBenchListener(MenuRegistry menuRegistry) implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();

        if (isBenchEntity(targetEntity)) {
            AbstractConstructionMenu constructionMenu = menuRegistry.get(MenuType.MAIN_CONSTRUCTION);
            constructionMenu.open(MenuContext.create(player, targetEntity.getLocation()));
        }
    }

    private boolean isBenchEntity(Entity entity) {
        String value = entity.getPersistentDataContainer().get(new NamespacedKey("flare", "bench"), PersistentDataType.STRING);
        return value != null && value.equalsIgnoreCase("bench_entity");
    }

}