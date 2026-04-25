package me.byteful.plugin.nightmarket.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageManager {
    private final Map<String, String> messages = new HashMap<>();
    private final JavaPlugin plugin;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.load();
    }

    private void load() {
        this.messages.clear();

        File file = new File(this.plugin.getDataFolder(), "messages.txt");
        if (!file.exists()) {
            this.plugin.saveResource("messages.txt", false);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) {
                    continue;
                }

                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                this.messages.put(key, value);
            }
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to load messages.txt: " + e.getMessage());
        }
    }

    public String get(String key) {
        return this.messages.getOrDefault(key, key);
    }
}
