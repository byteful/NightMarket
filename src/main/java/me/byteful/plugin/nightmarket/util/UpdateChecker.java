package me.byteful.plugin.nightmarket.util;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import me.byteful.plugin.nightmarket.NightMarketPlugin;

public final class UpdateChecker {
    private final NightMarketPlugin plugin;
    private String lastCheckedVersion = "N/A";

    public UpdateChecker(NightMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public String getLastCheckedVersion() {
        return this.lastCheckedVersion;
    }

    public void check() {
        this.plugin.getLogger().info("Checking for updates...");
        final String resourceId = "%%__RESOURCE__%%";
        final String isBuiltByBit = "%%__BUILTBYBIT__%%";
        final String currentVersion = this.plugin.getDescription().getVersion();

        if (currentVersion.contains("BETA")) {
            this.plugin.getLogger().info("Update check was cancelled because you are running a beta build!");

            return;
        }

        if (resourceId.startsWith("%")) {
            this.plugin.getLogger().info("Update check was cancelled because you are not using a purchased plugin JAR!");

            return;
        }

        this.plugin.getScheduler().runAsync(() -> {
            try (final InputStream inputStream = new URL("https://api.byteful.me/nightmarket").openStream(); final Scanner scanner = new Scanner(inputStream)) {
                if (!scanner.hasNext()) {
                    return;
                }

                final String latestVersion = scanner.next();

                if (currentVersion.equals(latestVersion)) {
                    this.plugin.getLogger().info("No new updates found.");
                } else {
                    this.plugin.getLogger().info("A new update was found. You are on " + currentVersion + " while the latest version is " + latestVersion + ".");
                    final String downloadUrl = isBuiltByBit.equalsIgnoreCase(
                        "true") ? "https://builtbybit.com/resources/" + resourceId : "https://www.spigotmc.org/resources/" + resourceId;
                    this.plugin.getLogger().info("Please install this update from: " + downloadUrl);
                }

                this.lastCheckedVersion = latestVersion;
            } catch (IOException e) {
                this.plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
            }
        });
    }
}
