package me.byteful.plugin.nightmarket.parser;

import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Preconditions;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import redempt.redlib.RedLib;

import java.util.List;
import java.util.Objects;

import static me.byteful.plugin.nightmarket.util.Text.color;

public class IconParser {
  public static ItemStack parse(ConfigurationSection config) {
    Preconditions.checkNotNull(config, "Config for icon needs to not be null!");

    final String name = config.getString("name");
    final List<String> lore = config.getStringList("lore");
    final String mat = config.getString("material");
    final String head = config.getString("player_head");
    final Integer customModelData = config.contains("custom_model_data") ? config.getInt("custom_model_data") : null;

    if (mat == null && head != null) {
      return parseHead(name, lore, head, customModelData);
    } else if (mat != null) {
      final ItemStack material = XMaterial.matchXMaterial(mat).orElseThrow(() -> new RuntimeException("Failed to parse material: " + mat)).parseItem();
      Preconditions.checkNotNull(material, "Failed to parse material: " + mat);
      Preconditions.checkArgument(material.getType() != Material.AIR, "Material cannot be AIR!");
      return parseMaterial(name, lore, material, customModelData);
    } else {
      throw new RuntimeException("Failed to find either material or player head for: " + config.getName());
    }
  }

  private static ItemStack parseMaterial(String name, List<String> lore, ItemStack item, Integer customModelData) {
    final ItemMeta meta = item.getItemMeta();
    Preconditions.checkNotNull(meta, "Failed to load item's meta.");
    applyMeta(name, lore, customModelData, meta);
    item.setItemMeta(meta);

    return item;
  }

  private static ItemStack parseHead(String name, List<String> lore, String texture, Integer customModelData) {
    final ItemStack item = Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem());
    SkullMeta meta = (SkullMeta) item.getItemMeta();
    Preconditions.checkNotNull(meta, "Failed to load item's meta.");
    meta = SkullUtils.applySkin(meta, texture);
    applyMeta(name, lore, customModelData, meta);
    item.setItemMeta(meta);

    return item;
  }

  private static void applyMeta(String name, List<String> lore, Integer customModelData, ItemMeta meta) {
    meta.setDisplayName(color(name));
    meta.setLore(lore == null || lore.isEmpty() ? null : color(lore));
    if (customModelData != null && RedLib.MID_VERSION >= 14) {
      meta.setCustomModelData(customModelData);
    }
  }
}
