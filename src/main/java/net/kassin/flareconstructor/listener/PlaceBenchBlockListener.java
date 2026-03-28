package net.kassin.flareconstructor.listener;

import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.api.CustomModelProvider;
import net.kassin.flareconstructor.utils.EntityPlacementValidator;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PlaceBenchBlockListener implements Listener {

    @EventHandler
    public void onPlaceItem(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (clickedBlock == null) return;
        if (!isBenchItem(player.getInventory().getItemInMainHand())) return;

        Location spawnLocation = clickedBlock.getLocation().add(0.5, 1, 0.5);

        spawnLocation.setYaw(player.getLocation().getYaw() + 180);
        spawnLocation.setPitch(0f);

        if (!EntityPlacementValidator.isAreaClear(clickedBlock, player)) {
            player.sendMessage("§cSem espaço para colocar aqui.");
            return;
        }


        event.setCancelled(true);
        placeBench(spawnLocation);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!isBenchItem(player.getInventory().getItemInMainHand())) return;
        event.setCancelled(true);
    }

    private boolean isBenchItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return false;

        return itemStack.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey("flare", "constructor"), PersistentDataType.STRING);
    }

    private void placeBench(Location location) {
        CustomModelProvider customModelProvider = FlareCorePlugin.getAPI().getCustomModelProvider();

        Entity entity = location.getWorld().spawn(location, org.bukkit.entity.ItemDisplay.class, display -> {
            display.setInvisible(true);
            display.getPersistentDataContainer().set(new NamespacedKey("flare", "bench"),
                    PersistentDataType.STRING, "bench_entity");
        });

        customModelProvider.applyModel("flare_bench", entity);
        customModelProvider.runAnimation("initial", "flare_bench", entity);
    }

}
