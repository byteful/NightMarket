package me.byteful.plugin.nightmarket.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigUpdater {
    private static final Set<String> IGNORED_SECTIONS = new HashSet<>(Arrays.asList(
        "items",
        "gui.extra_icons",
        "access_schedule.dates",
        "access_schedule.times",
        "rotate_schedule.dates",
        "rotate_schedule.times"
    ));

    private ConfigUpdater() {
    }

    public static void update(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }

        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) {
            return;
        }

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        boolean changed = false;

        for (String key : defaultConfig.getKeys(true)) {
            Object defaultValue = defaultConfig.get(key);
            if (defaultValue instanceof ConfigurationSection) {
                continue;
            }
            if (isIgnored(key)) {
                continue;
            }

            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultValue);
                changed = true;
            }
        }

        if (changed) {
            try {
                userConfig.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to auto-update config.yml: " + e.getMessage());
            }
        }
    }

    private static boolean isIgnored(String key) {
        for (String ignored : IGNORED_SECTIONS) {
            if (key.equals(ignored) || key.startsWith(ignored + ".")) {
                return true;
            }
        }
        return false;
    }
}
