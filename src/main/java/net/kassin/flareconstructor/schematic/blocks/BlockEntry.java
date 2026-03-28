package net.kassin.flareconstructor.schematic.blocks;

import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.World;

public record BlockEntry(World world, int x, int y, int z, BlockState state) {
}
