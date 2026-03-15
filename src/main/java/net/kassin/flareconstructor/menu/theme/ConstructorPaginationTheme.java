package net.kassin.flareconstructor.menu.theme;

import com.cryptomorin.xseries.XMaterial;
import net.flareplugins.core.utils.MiniMessageProvider;
import net.flareplugins.core.utils.items.ItemBuilder;
import net.flareplugins.core.utils.window.PaginationTheme;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ConstructorPaginationTheme implements PaginationTheme {

    private final MiniMessage mm = MiniMessageProvider.get();

    @Override
    public ItemStack buildNextItem(int currentPage, int maxPage) {
        return ItemBuilder.builder(XMaterial.ARROW)
                .nameComponent(mm.deserialize("<green>Próxima Página <gray>(" + currentPage + "/" + maxPage + ")"))
                .loreComponent(java.util.List.of(
                        mm.deserialize("<gray>Clique para avançar.")
                ))
                .build();
    }

    @Override
    public ItemStack buildPreviousItem(int currentPage, int maxPage) {
        return ItemBuilder.builder(XMaterial.ARROW)
                .nameComponent(mm.deserialize("<red>Página Anterior <gray>(" + currentPage + "/" + maxPage + ")"))
                .loreComponent(java.util.List.of(
                        mm.deserialize("<gray>Clique para voltar.")
                ))
                .build();
    }

}