package me.byteful.plugin.nightmarket.util.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TextFormatNormalizer {
    private static final Pattern BUNGEE_HEX_PATTERN = Pattern.compile("[&\u00a7]x([&\u00a7][0-9a-fA-F]){6}");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("[&\u00a7]#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("[&\u00a7]([0-9a-fk-orA-FK-OR])");
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("<(?:color:)?#([0-9a-fA-F]{6})>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLOSING_TAG_PATTERN = Pattern.compile("</[a-z_:]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADVANCED_TAG_OPEN_PATTERN = Pattern.compile(
        "<(gradient|rainbow|hover|click|insertion|font|transition|selector|score|keybind|translatable)(:[^>]*)?>",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ADVANCED_TAG_CLOSE_PATTERN = Pattern.compile(
        "</(gradient|rainbow|hover|click|insertion|font|transition|selector|score|keybind|translatable)>",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNKNOWN_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private static final Map<Character, String> LEGACY_CODE_TO_MINI_TAG = new LinkedHashMap<>();
    private static final Map<String, String> MINI_TAG_TO_LEGACY_CODE = new LinkedHashMap<>();
    private static final Pattern MINI_TAG_PATTERN;

    static {
        register('0', "black");
        register('1', "dark_blue");
        register('2', "dark_green");
        register('3', "dark_aqua");
        register('4', "dark_red");
        register('5', "dark_purple");
        register('6', "gold");
        register('7', "gray");
        register('8', "dark_gray");
        register('9', "blue");
        register('a', "green");
        register('b', "aqua");
        register('c', "red");
        register('d', "light_purple");
        register('e', "yellow");
        register('f', "white");
        register('k', "obfuscated");
        register('l', "bold");
        register('m', "strikethrough");
        register('n', "underlined");
        register('o', "italic");
        register('r', "reset");

        MINI_TAG_PATTERN = Pattern.compile(
            "<(" + String.join("|", MINI_TAG_TO_LEGACY_CODE.keySet()) + ")>",
            Pattern.CASE_INSENSITIVE
        );
    }

    private TextFormatNormalizer() {
    }

    static String normalizeLegacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String normalized = replaceBungeeHexWithMiniMessage(input);
        normalized = replaceLegacyHexWithMiniMessage(normalized);
        return replaceLegacyCodesWithMiniMessage(normalized);
    }

    static String normalizeMiniMessageToLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String normalized = replaceLegacyHexWithLegacy(input);
        normalized = replaceHexTagsWithLegacy(normalized);
        normalized = replaceLegacyCodesWithLegacy(normalized);
        normalized = replaceMiniTagsWithLegacy(normalized);

        // Legacy has no close concept; keep existing behavior by dropping close/advanced tags.
        normalized = CLOSING_TAG_PATTERN.matcher(normalized).replaceAll("");
        normalized = ADVANCED_TAG_OPEN_PATTERN.matcher(normalized).replaceAll("");
        normalized = ADVANCED_TAG_CLOSE_PATTERN.matcher(normalized).replaceAll("");
        return UNKNOWN_TAG_PATTERN.matcher(normalized).replaceAll("");
    }

    private static void register(char code, String tagName) {
        LEGACY_CODE_TO_MINI_TAG.put(code, "<" + tagName + ">");
        MINI_TAG_TO_LEGACY_CODE.put(tagName, "\u00a7" + code);
    }

    private static String replaceBungeeHexWithMiniMessage(String input) {
        Matcher matcher = BUNGEE_HEX_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String group = matcher.group();
            StringBuilder hex = new StringBuilder(6);
            for (int index = 3; index < group.length(); index += 2) {
                hex.append(group.charAt(index));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement("<color:#" + hex + ">"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceLegacyCodesWithMiniMessage(String input) {
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            String tag = LEGACY_CODE_TO_MINI_TAG.get(code);
            if (tag != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(tag));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceLegacyCodesWithLegacy(String input) {
        Matcher matcher = LEGACY_CODE_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement("\u00a7" + code));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceLegacyHexWithMiniMessage(String input) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement("<color:#" + matcher.group(1) + ">"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceLegacyHexWithLegacy(String input) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(toLegacyHex(matcher.group(1))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceHexTagsWithLegacy(String input) {
        Matcher matcher = HEX_TAG_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(toLegacyHex(matcher.group(1))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String replaceMiniTagsWithLegacy(String input) {
        Matcher matcher = MINI_TAG_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String tagName = matcher.group(1).toLowerCase();
            String legacy = MINI_TAG_TO_LEGACY_CODE.get(tagName);
            if (legacy != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(legacy));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String toLegacyHex(String hex) {
        return "\u00a7x\u00a7" + hex.charAt(0)
            + "\u00a7" + hex.charAt(1)
            + "\u00a7" + hex.charAt(2)
            + "\u00a7" + hex.charAt(3)
            + "\u00a7" + hex.charAt(4)
            + "\u00a7" + hex.charAt(5);
    }
}
