package net.kassin.flareconstructor.menu.configuration.settings;

import net.kyori.adventure.text.Component;

public record MainMenuSettings(
        Component title,
        char[][] layout
) {}