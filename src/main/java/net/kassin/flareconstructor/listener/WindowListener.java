package net.kassin.flareconstructor.listener;

import net.flareplugins.core.utils.window.PaginatedWindow;
import net.flareplugins.core.utils.window.WindowButton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public record WindowListener() implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PaginatedWindow window)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (e.getClickedInventory() == null) return;

        if (!e.getClickedInventory().equals(e.getInventory())) return;

        int slot = e.getSlot();

        WindowButton button = window.getButton(slot);

        if (button != null && button.getAction() != null) {
            button.getAction().accept(e.getClick(), player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PaginatedWindow) {
            e.setCancelled(true);
        }
    }

}