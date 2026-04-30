package me.byteful.plugin.nightmarket.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
        } else {
            updateMessages(this.plugin, file);
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

    private static void updateMessages(JavaPlugin plugin, File file) {
        String existingContent;
        try {
            existingContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read messages.txt for auto-update: " + e.getMessage());
            return;
        }

        Map<String, String> defaultMessages = readDefaultMessages(plugin);
        if (defaultMessages.isEmpty()) {
            return;
        }

        Set<String> existingKeys = parseKeys(existingContent);
        Map<String, String> missingMessages = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : defaultMessages.entrySet()) {
            if (existingKeys.contains(entry.getKey())) {
                continue;
            }

            missingMessages.put(entry.getKey(), entry.getValue());
        }

        if (missingMessages.isEmpty()) {
            return;
        }

        String lineSeparator = existingContent.contains("\r\n") ? "\r\n" : "\n";
        StringBuilder updatedContent = new StringBuilder(existingContent);
        if (!updatedContent.isEmpty() && !endsWithLineSeparator(updatedContent)) {
            updatedContent.append(lineSeparator);
        }
        if (!updatedContent.isEmpty() && !endsWithBlankLine(updatedContent)) {
            updatedContent.append(lineSeparator);
        }

        for (Map.Entry<String, String> entry : missingMessages.entrySet()) {
            updatedContent.append(entry.getKey()).append(": ").append(entry.getValue()).append(lineSeparator);
        }

        try {
            Files.writeString(file.toPath(), updatedContent.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to auto-update messages.txt: " + e.getMessage());
        }
    }

    private static Map<String, String> readDefaultMessages(JavaPlugin plugin) {
        InputStream defaultStream = plugin.getResource("messages.txt");
        if (defaultStream == null) {
            return new LinkedHashMap<>();
        }

        try (InputStream stream = defaultStream) {
            String defaultContent = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> messages = new LinkedHashMap<>();
            for (String line : defaultContent.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int colonIndex = trimmed.indexOf(':');
                if (colonIndex == -1) {
                    continue;
                }

                String key = trimmed.substring(0, colonIndex).trim();
                if (messages.containsKey(key)) {
                    continue;
                }

                messages.put(key, trimmed.substring(colonIndex + 1).trim());
            }
            return messages;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read bundled messages.txt for auto-update: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private static Set<String> parseKeys(String content) {
        Set<String> keys = new HashSet<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex == -1) {
                continue;
            }

            keys.add(trimmed.substring(0, colonIndex).trim());
        }
        return keys;
    }

    private static boolean endsWithLineSeparator(StringBuilder content) {
        int length = content.length();
        return content.charAt(length - 1) == '\n' || content.charAt(length - 1) == '\r';
    }

    private static boolean endsWithBlankLine(StringBuilder content) {
        String value = content.toString();
        if (value.length() < 2) {
            return false;
        }

        return value.endsWith("\n\n") || value.endsWith("\r\n\r\n");
    }

    public String get(String key) {
        return this.messages.getOrDefault(key, key);
    }
}
