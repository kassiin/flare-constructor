package net.kassin.flareconstructor.schematic.blocks;

import com.sk89q.worldedit.world.block.BlockState;

public record BlockEntry(int x, int y, int z, BlockState state) {}
