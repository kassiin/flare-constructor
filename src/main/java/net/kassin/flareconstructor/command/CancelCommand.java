package net.kassin.flareconstructor.command;

import net.kassin.flareconstructor.schematic.section.BuildSession;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        BuildSession session = BuildSession.get(player.getUniqueId());

        if (session == null) {
            player.sendMessage("§cNenhuma construção em andamento.");
            return true;
        }

        session.cancel();
        BuildSession.remove(player.getUniqueId());

        if (args.length > 0 && args[0].equalsIgnoreCase("undo")) {
            session.undo();
            player.sendMessage("§e⟳ Construção cancelada e desfeita.");
        } else {
            player.sendMessage("§c✖ Construção cancelada. Use §f/buildcancel undo §cpara reverter os blocos.");
        }

        return true;
    }
}
