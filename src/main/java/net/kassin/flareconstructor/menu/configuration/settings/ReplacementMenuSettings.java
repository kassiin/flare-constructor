package net.kassin.flareconstructor.menu.configuration.settings;

import net.kyori.adventure.text.Component;
import java.util.List;

public record ReplacementMenuSettings(
        Component title,
        char[][] layout,
        Component backName,
        String nameSelected,
        String nameUnselected,
        List<String> loreBase,
        List<String> loreSelected,
        List<String> loreUnselected
) {}