package net.kassin.flareconstructor.menu.configuration.settings;

import net.kyori.adventure.text.Component;

public record SettingsMenuSettings(
        Component title,
        char[][] layout
) {}