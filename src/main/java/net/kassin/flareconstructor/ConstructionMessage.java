package net.kassin.flareconstructor;


import net.flareplugins.core.utils.message.CoreMessenger;
import net.flareplugins.core.utils.message.MCUtilsFontConverter;

public class ConstructionMessage extends CoreMessenger {

    public ConstructionMessage(FlareConstructorPlugin plugin) {
        super(
                plugin,
                MCUtilsFontConverter::toMCUtilsFont,
                () -> plugin.getConfig().getBoolean("custom-font", false)
        );
    }
}