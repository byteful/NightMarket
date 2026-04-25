package me.byteful.plugin.nightmarket.parser;

import java.util.List;
import java.util.Objects;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record IconData(@NotNull ItemStack baseItem, @Nullable String name, @Nullable List<String> lore) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        IconData iconData = (IconData) o;
        return this.baseItem.equals(iconData.baseItem) && Objects.equals(this.name, iconData.name) && Objects.equals(this.lore, iconData.lore);
    }
}
