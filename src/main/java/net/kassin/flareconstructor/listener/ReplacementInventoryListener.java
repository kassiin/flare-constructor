package net.kassin.flareconstructor.listener;

import net.flareplugins.core.utils.window.PaginatedWindow;
import net.kassin.flareconstructor.menu.registry.MenuRegistry;
import net.kassin.flareconstructor.menu.type.MenuType;
import net.kassin.flareconstructor.menu.window.ConstructionReplacementMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public record ReplacementInventoryListener(MenuRegistry menuRegistry) implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof PaginatedWindow)) {
            return;
        }

        ConstructionReplacementMenu menu = menuRegistry.get(MenuType.REPLACEMENT);
        if (menu == null || !menu.isSelecting(player)) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir() || !clickedItem.getType().isBlock()) {
                return;
            }

            menu.applyReplacement(player, clickedItem.getType());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        ConstructionReplacementMenu menu = menuRegistry.get(MenuType.REPLACEMENT);

        if (menu != null && menu.isSelecting(player) && !menu.isRefreshing(player)) {
            menu.cancelSelection(player);
        }
    }

}