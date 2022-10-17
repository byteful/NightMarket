package me.byteful.plugin.nightmarket.util;


import me.byteful.plugin.nightmarket.NightMarketPlugin;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

// From: https://www.spigotmc.org/wiki/creating-an-update-checker-that-checks-for-updates
// Further modified by byteful to accompany Tradeconomy.
public final class UpdateChecker {
  private final NightMarketPlugin plugin;
  private String lastCheckedVersion = "N/A";

  public UpdateChecker(NightMarketPlugin plugin) {
    this.plugin = plugin;
  }

  public String getLastCheckedVersion() {
    return lastCheckedVersion;
  }

  public void check() {
    plugin.getLogger().info("Checking for updates...");
    final String resourceId = "%%__RESOURCE__%%";
    final String isBuiltByBit = "%%__BUILTBYBIT__%%";

    if (resourceId.startsWith("%")) {
      plugin.getLogger().info("Update check was cancelled because you are not using a purchased plugin JAR!");

      return;
    }

    final String currentVersion = plugin.getDescription().getVersion();
    if (currentVersion.contains("BETA")) {
      plugin.getLogger().info("Update check was cancelled because you are running a beta build!");

      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try (final InputStream inputStream = new URL("https://api.byteful.me/nightmarket").openStream(); final Scanner scanner = new Scanner(inputStream)) {
        if (!scanner.hasNext()) {
          return;
        }

        final String latestVersion = scanner.next();

        if (currentVersion.equals(latestVersion)) {
          plugin.getLogger().info("No new updates found.");
        } else {
          plugin.getLogger().info("A new update was found. You are on " + currentVersion + " while the latest version is " + latestVersion + ".");
          final String downloadUrl = isBuiltByBit.equalsIgnoreCase("true") ? "https://builtbybit.com/resources/" + resourceId : "https://www.spigotmc.org/resources/" + resourceId;
          plugin.getLogger().info("Please install this update from: " + downloadUrl);
        }

        lastCheckedVersion = latestVersion;
      } catch (IOException e) {
        plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
      }
    });
  }
}