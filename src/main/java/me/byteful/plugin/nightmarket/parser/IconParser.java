package me.byteful.plugin.nightmarket.parser;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import redempt.redlib.RedLib;

public class IconParser {
    public static IconData parse(ConfigurationSection config) {
        Preconditions.checkNotNull(config, "Config for icon needs to not be null!");

        final String name = config.getString("name");
        final List<String> rawLore = config.getStringList("lore");
        final List<String> lore = rawLore.isEmpty() ? null : rawLore;
        final int amount = config.getInt("amount", 1);
        final String mat = config.getString("material");
        final String head = config.getString("player_head");
        final Integer customModelData = config.contains("custom_model_data") ? config.getInt("custom_model_data") : null;

        ItemStack baseItem;
        if (head != null && !head.isEmpty()) {
            baseItem = parseHead(amount, head, customModelData);
        } else if (mat != null) {
            baseItem = parseMaterial(amount, mat, customModelData);
        } else {
            throw new RuntimeException("Failed to find either material or player head for: " + config.getName());
        }

        return new IconData(baseItem, name, lore);
    }

    private static ItemStack parseMaterial(int amount, String mat, Integer customModelData) {
        final ItemStack item = XMaterial.matchXMaterial(mat).orElseThrow(() -> new RuntimeException("Failed to parse material: " + mat)).parseItem();
        Preconditions.checkNotNull(item, "Failed to parse material: " + mat);
        Preconditions.checkArgument(item.getType() != Material.AIR, "Material cannot be AIR!");
        applyBaseMeta(customModelData, item);
        item.setAmount(amount);

        return item;
    }

    private static ItemStack parseHead(int amount, String texture, Integer customModelData) {
        final ItemStack item = Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem());
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        Preconditions.checkNotNull(meta, "Failed to load item's meta.");
        meta = (SkullMeta) XSkull.of(meta).profile(Profileable.detect(texture)).apply();
        item.setItemMeta(meta);
        applyBaseMeta(customModelData, item);
        item.setAmount(amount);

        return item;
    }

    private static void applyBaseMeta(Integer customModelData, ItemStack item) {
        if (customModelData != null && RedLib.MID_VERSION >= 14) {
            final ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }
    }
}
