package net.kassin.flareconstructor;

import lombok.Getter;
import net.kassin.flareconstructor.initializer.ConstructorInitializer;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlareConstructorPlugin extends JavaPlugin {

    @Getter
    private static FlareConstructorPlugin instance;
    private ConstructorInitializer initializer;

    @Override
    public void onEnable() {
        reloadConfig();
        instance = this;
        this.initializer = new ConstructorInitializer(this);
    }

    @Override
    public void onDisable() {}

    public ConstructorInitializer getInitializer() {
        return initializer;
    }

}