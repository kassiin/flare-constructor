package net.kassin.flareconstructor.command;

import com.cryptomorin.xseries.XMaterial;
import net.flareplugins.core.FlareCorePlugin;
import net.flareplugins.core.utils.items.ItemBuilder;
import net.kassin.flareconstructor.ConstructionMessage;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.menu.configuration.ConstructionRegistry;
import net.kassin.flareconstructor.schematic.SchematicCreator;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildCommand implements TabExecutor {

    private final FlareConstructorPlugin plugin;
    private final ConstructionRegistry registry;
    private final ConstructionMessage message;

    public BuildCommand(FlareConstructorPlugin plugin, ConstructionRegistry registry, ConstructionMessage message) {
        this.plugin = plugin;
        this.registry = registry;
        this.message = message;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length < 1) {
            ItemStack itemFromData = FlareCorePlugin.getAPI().getItemFactory().createItemFromData("flare_constructor:flare_bench_item", XMaterial.SCULK_SENSOR, null, null);

            ItemStack bench = ItemBuilder.builder(itemFromData)
                    .name("<gradient:#86efac:#4ade80>Flare Bench")
                    .withContext(new NamespacedKey("flare", "constructor"), PersistentDataType.STRING, "bench")
                    .build();

            player.getInventory().addItem(bench);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                if (player.hasPermission("flareconstructor.admin")) {
                    plugin.getInitializer().reload();
                }
                return true;
            }
            case "create" -> {
                if (!player.hasPermission("flareconstructor.admin") || args.length < 2) {
                    return true;
                }
                String id = args[1].toLowerCase();
                try {
                    SchematicCreator.createAndRegister(plugin, player, id);
                } catch (IllegalArgumentException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;

            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            if ("reload".startsWith(partial)) completions.add("reload");
            if ("gui".startsWith(partial)) completions.add("gui");
            if ("create".startsWith(partial)) completions.add("create");

            for (String id : registry.getAvailableBuildIds()) {
                if (id.startsWith(partial)) {
                    completions.add(id);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            completions.add("<id_da_construcao>");
        }

        return completions;
    }

    private int parseInt(String s, int def) {
        try {
            return Math.max(1, Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return def;
        }
    }

}