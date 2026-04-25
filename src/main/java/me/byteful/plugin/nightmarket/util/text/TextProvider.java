package me.byteful.plugin.nightmarket.util.text;

import java.util.List;
import java.util.stream.Collectors;
import me.byteful.plugin.nightmarket.parser.IconData;
import me.byteful.plugin.nightmarket.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface TextProvider {
    void sendMessage(CommandSender sender, String text);

    String toLegacy(String text);

    default ItemStack buildItem(IconData icon) {
        return this.buildItem(null, icon);
    }

    default ItemStack buildItem(Player player, IconData icon, String... replacements) {
        if (icon == null) {
            return null;
        }
        ItemStack item = icon.baseItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String name = icon.name();
        if (name != null && !name.isEmpty()) {
            this.setDisplayName(meta, Text.applyPAPIAndReplace(player, name, replacements));
        }

        List<String> lore = icon.lore();
        if (lore != null && !lore.isEmpty()) {
            this.setLore(meta, lore.stream()
                .map(line -> Text.applyPAPIAndReplace(player, line, replacements))
                .collect(Collectors.toList()));
        }

        item.setItemMeta(meta);
        return item;
    }

    void setDisplayName(ItemMeta meta, String text);

    void setLore(ItemMeta meta, List<String> lines);
}
