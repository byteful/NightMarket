package me.byteful.plugin.nightmarket.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigUpdater {
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

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

        YamlConfiguration defaultConfig;
        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream == null) {
                return;
            }

            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read bundled config.yml for auto-update: " + e.getMessage());
            return;
        }

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

        changed |= addMissingItemDefaults(userConfig);

        if (changed) {
            try {
                createBackup(configFile);
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

    private static boolean addMissingItemDefaults(YamlConfiguration userConfig) {
        ConfigurationSection itemsSection = userConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            return false;
        }

        boolean changed = false;
        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) {
                continue;
            }

            changed |= addPath(userConfig, "items." + itemId + ".permission", "");
            changed |= addPath(userConfig, "items." + itemId + ".confirm_purchase", "DEFAULT");
        }
        return changed;
    }

    private static boolean addPath(YamlConfiguration config, String path, Object value) {
        if (config.contains(path)) {
            return false;
        }

        config.set(path, value);
        return true;
    }

    private static void createBackup(File configFile) throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
        Path source = configFile.toPath();
        Path backup = source.resolveSibling(configFile.getName() + "." + timestamp + ".bak");
        int duplicate = 1;
        while (Files.exists(backup)) {
            backup = source.resolveSibling(configFile.getName() + "." + timestamp + "-" + duplicate + ".bak");
            duplicate++;
        }

        Files.copy(source, backup);
    }
}
