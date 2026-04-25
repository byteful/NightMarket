package me.byteful.plugin.nightmarket.util.text;

import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;

public class PaperTextProvider implements TextProvider {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    @Override
    public void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(this.deserialize(text));
    }

    @Override
    public String toLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return LEGACY_SERIALIZER.serialize(this.deserialize(text));
    }

    @Override
    public void setDisplayName(ItemMeta meta, String text) {
        meta.displayName(this.deserializeItemText(text));
    }

    @Override
    public void setLore(ItemMeta meta, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            meta.lore(null);
            return;
        }
        meta.lore(lines.stream().map(this::deserializeItemText).collect(Collectors.toList()));
    }

    private Component deserialize(String text) {
        return MINI_MESSAGE.deserialize(TextFormatNormalizer.normalizeLegacyToMiniMessage(text));
    }

    private Component deserializeItemText(String text) {
        return this.deserialize(text)
                .applyFallbackStyle(Style.style(TextDecoration.ITALIC.withState(false)));
    }
}
