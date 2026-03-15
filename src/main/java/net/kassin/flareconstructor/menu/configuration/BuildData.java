package net.kassin.flareconstructor.menu.configuration;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Map;

public record BuildData(
        String id,
        int delayTicks,
        int blocksPerTick,
        int agents,
        Location benchLocation,
        Map<Material, Material> replacements // Add this field
) {}