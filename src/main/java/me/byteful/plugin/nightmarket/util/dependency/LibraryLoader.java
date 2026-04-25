package me.byteful.plugin.nightmarket.util.dependency;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import redempt.redlib.RedLib;

// Much of this code was borrowed from lucko's helper library. Adapted by byteful to support isolated class loaders.
public final class LibraryLoader {
    private static final Supplier<URLClassLoaderAccess> URL_INJECTOR = Suppliers.memoize(
        () -> URLClassLoaderAccess.create((URLClassLoader) NightMarketPlugin.class.getClassLoader()));
    private static final List<String> USED_NAMES = new ArrayList<>();

    public static IsolatedClassLoader load(NightMarketPlugin plugin, String groupId, String artifactId, String version) {
        return load(plugin, groupId, artifactId, version, "https://repo1.maven.org/maven2");
    }

    public static IsolatedClassLoader load(NightMarketPlugin plugin, String groupId, String artifactId, String version, String repoUrl) {
        return load(plugin, new Dependency(groupId, artifactId, version, repoUrl));
    }

    public static IsolatedClassLoader load(NightMarketPlugin plugin, Dependency d) {
        if (RedLib.MID_VERSION >= 17) {
            return null; // We don't need to do this ourselves because Spigot has library loading support now!
        }
        final String name = d.artifactId() + "-" + d.version();
        final File saveLocation = new File(getLibFolder(plugin), name + ".jar");

        download(plugin, d, saveLocation, name);

        try {
            final IsolatedClassLoader loader = new IsolatedClassLoader(saveLocation.toURI().toURL());
            plugin.getLogger().info("Loaded dependency '" + name + "' successfully.");
            USED_NAMES.add(saveLocation.getName());

            return loader;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to load dependency: " + saveLocation, e);
        }
    }

    private static void download(NightMarketPlugin plugin, Dependency d, File saveLocation, String name) {
        plugin.getLogger().info(String.format("Loading dependency '%s:%s:%s'...", d.groupId(), d.artifactId(), d.version()));

        if (!saveLocation.exists()) {
            try {
                plugin.getLogger().info("Dependency '" + name + "' not found in libraries folder. Attempting to download...");
                final URL url = d.getUrl();

                try (final InputStream is = url.openStream()) {
                    Files.copy(is, saveLocation.toPath());
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to download dependency '" + name + "'.", e);
            }

            plugin.getLogger().info("Dependency '" + name + "' successfully downloaded.");
        }

        if (!saveLocation.exists()) {
            throw new RuntimeException("Unable to download dependency: " + d);
        }
    }

    private static File getLibFolder(NightMarketPlugin plugin) {
        final File folder = new File(plugin.getDataFolder(), "libraries");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public static void loadWithInject(NightMarketPlugin plugin, String groupId, String artifactId, String version) {
        loadWithInject(plugin, groupId, artifactId, version, "https://repo1.maven.org/maven2");
    }

    public static void loadWithInject(NightMarketPlugin plugin, String groupId, String artifactId, String version, String repoUrl) {
        loadWithInject(plugin, new Dependency(groupId, artifactId, version, repoUrl));
    }

    public static void loadWithInject(NightMarketPlugin plugin, Dependency d) {
        if (RedLib.MID_VERSION >= 17) {
            return; // We don't need to do this ourselves because Spigot has library loading support now!
        }

        final String name = d.artifactId() + "-" + d.version();
        final File saveLocation = new File(getLibFolder(plugin), name + ".jar");

        download(plugin, d, saveLocation, name);

        try {
            URL_INJECTOR.get().addURL(saveLocation.toURI().toURL());
            plugin.getLogger().info("Loaded dependency '" + name + "' successfully.");

            USED_NAMES.add(saveLocation.getName());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load dependency: " + saveLocation, e);
        }
    }

    public static void clearUnusedJars(NightMarketPlugin plugin) {
        final File folder = getLibFolder(plugin);

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (!file.getName().endsWith(".jar") || USED_NAMES.contains(file.getName())) {
                continue;
            }

            file.delete();
        }
    }

    public record Dependency(String groupId, String artifactId, String version, String repoUrl) {
            public Dependency(String groupId, String artifactId, String version, String repoUrl) {
                this.groupId = Objects.requireNonNull(groupId, "groupId");
                this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
                this.version = Objects.requireNonNull(version, "version");
                this.repoUrl = Objects.requireNonNull(repoUrl, "repoUrl");
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                result = result * PRIME + this.groupId().hashCode();
                result = result * PRIME + this.artifactId().hashCode();
                result = result * PRIME + this.version().hashCode();
                result = result * PRIME + this.repoUrl().hashCode();
                return result;
            }

            @Override
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (!(o instanceof Dependency other)) {
                    return false;
                }
                return this.groupId().equals(other.groupId()) && this.artifactId().equals(other.artifactId()) && this.version()
                    .equals(other.version()) && this.repoUrl().equals(other.repoUrl());
            }

            @Override
            public String toString() {
                return "LibraryLoader.Dependency(" + "groupId=" + this.groupId() + ", " + "artifactId=" + this.artifactId() + ", " + "version=" + this.version() + ", " + "repoUrl=" + this.repoUrl() + ")";
            }

            public URL getUrl() throws MalformedURLException {
                String repo = this.repoUrl;
                if (!repo.endsWith("/")) {
                    repo += "/";
                }
                repo += "%s/%s/%s/%s-%s.jar";

                String url = String.format(repo, this.groupId.replace(".", "/"), this.artifactId, this.version, this.artifactId, this.version);
                return new URL(url);
            }
        }
}
