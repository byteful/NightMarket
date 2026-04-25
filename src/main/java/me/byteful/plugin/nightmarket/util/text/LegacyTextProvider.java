package me.byteful.plugin.nightmarket.util.text;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;

public class LegacyTextProvider implements TextProvider {
    @Override
    public void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(this.toLegacy(text));
    }

    @Override
    public String toLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return TextFormatNormalizer.normalizeMiniMessageToLegacy(text);
    }

    @Override
    public void setDisplayName(ItemMeta meta, String text) {
        meta.setDisplayName(this.toLegacy(text));
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            meta.setLore(null);
            return;
        }
        meta.setLore(lines.stream().map(this::toLegacy).collect(Collectors.toList()));
    }
}
