package net.kassin.flareconstructor.command;


import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import net.kassin.flareconstructor.schematic.session.ProjectRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelCommand implements CommandExecutor {

    private final ProjectRegistry projectRegistry;

    public CancelCommand(ProjectRegistry projectRegistry) {
        this.projectRegistry = projectRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        ConstructionProject project = projectRegistry.getProject(player.getUniqueId());

        if (project == null) {
            player.sendMessage("§cNenhuma construção em andamento.");
            return true;
        }

        project.cancel();
        projectRegistry.removeProject(player.getUniqueId());

        if (args.length > 0 && args[0].equalsIgnoreCase("undo")) {
            player.sendMessage("§e⟳ Construção cancelada e desfeita.");
        } else {
            player.sendMessage("§c✖ Construção cancelada. Use §f/buildcancel undo §cpara reverter os blocos.");
        }

        return true;
    }
}
