package me.byteful.plugin.nightmarket.util;

import org.bukkit.Bukkit;

public final class MinecraftVersion {
    private final int major;
    private final int minor;
    private final int patch;
    private final String original;
    private final boolean known;

    private MinecraftVersion(int major, int minor, int patch, String original, boolean known) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.original = original;
        this.known = known;
    }

    public static MinecraftVersion current() {
        return CurrentVersionHolder.CURRENT;
    }

    public static MinecraftVersion parse(String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isBlank()) {
            return unknown(bukkitVersion);
        }

        final String normalized = bukkitVersion.split("-", 2)[0].trim();
        if (normalized.isEmpty()) {
            return unknown(bukkitVersion);
        }

        final String[] parts = normalized.split("\\.");
        final int[] numbers = new int[]{0, 0, 0};
        final int limit = Math.min(parts.length, numbers.length);

        for (int index = 0; index < limit; index++) {
            final String part = parts[index].trim();
            if (part.isEmpty()) {
                return unknown(bukkitVersion);
            }

            try {
                numbers[index] = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return unknown(bukkitVersion);
            }

            if (numbers[index] < 0) {
                return unknown(bukkitVersion);
            }
        }

        return new MinecraftVersion(numbers[0], numbers[1], numbers[2], bukkitVersion, true);
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        if (!this.known) {
            return false;
        }

        if (this.major != major) {
            return this.major > major;
        }

        if (this.minor != minor) {
            return this.minor > minor;
        }

        return this.patch >= patch;
    }

    public boolean supportsCustomModelData() {
        return isAtLeast(1, 14, 0);
    }

    public boolean supportsSpigotLibraryLoading() {
        return isAtLeast(1, 17, 0);
    }

    public boolean isKnown() {
        return this.known;
    }

    public int major() {
        return this.major;
    }

    public int minor() {
        return this.minor;
    }

    public int patch() {
        return this.patch;
    }

    public String original() {
        return this.original;
    }

    public static boolean isCurrentAtLeast(int major, int minor, int patch) {
        return current().isAtLeast(major, minor, patch);
    }

    private static MinecraftVersion unknown(String original) {
        return new MinecraftVersion(0, 0, 0, original == null ? "" : original, false);
    }

    private static final class CurrentVersionHolder {
        private static final MinecraftVersion CURRENT = parse(Bukkit.getBukkitVersion());

        private CurrentVersionHolder() {
        }
    }

    @Override
    public String toString() {
        if (!this.known) {
            return "MinecraftVersion{unknown, original='" + this.original + "'}";
        }

        return "MinecraftVersion{major=" + this.major + ", minor=" + this.minor + ", patch=" + this.patch + ", original='" + this.original + "'}";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MinecraftVersion other)) {
            return false;
        }

        return this.major == other.major
            && this.minor == other.minor
            && this.patch == other.patch
            && this.known == other.known
            && this.original.equals(other.original);
    }

    @Override
    public int hashCode() {
        int result = this.major;
        result = 31 * result + this.minor;
        result = 31 * result + this.patch;
        result = 31 * result + this.original.hashCode();
        result = 31 * result + Boolean.hashCode(this.known);
        return result;
    }
}
