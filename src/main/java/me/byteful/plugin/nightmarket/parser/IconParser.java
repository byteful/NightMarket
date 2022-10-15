package me.byteful.plugin.nightmarket.parser;

import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

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

    if (mat == null && head != null) {
      return parseHead(name, lore, head);
    } else if (mat != null) {
      return parseMaterial(name, lore, Objects.requireNonNull(XMaterial.matchXMaterial(mat).orElseThrow(RuntimeException::new).parseItem()));
    } else {
      throw new RuntimeException("Failed to find either material or player head for: " + config.getName());
    }
  }

  private static ItemStack parseMaterial(String name, List<String> lore, ItemStack item) {
    final ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(color(name));
    meta.setLore(lore == null || lore.isEmpty() ? null : color(lore));
    item.setItemMeta(meta);

    return item;
  }

  private static ItemStack parseHead(String name, List<String> lore, String texture) {
    final ItemStack item = Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem());
    SkullMeta meta = (SkullMeta) item.getItemMeta();
    meta = SkullUtils.applySkin(meta, texture);
    meta.setDisplayName(color(name));
    meta.setLore(lore == null || lore.isEmpty() ? null : color(lore));
    item.setItemMeta(meta);

    return item;
  }
}