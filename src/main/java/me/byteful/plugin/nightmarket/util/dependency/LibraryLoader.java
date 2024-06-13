package me.byteful.plugin.nightmarket.util.dependency;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import me.byteful.plugin.nightmarket.NightMarketPlugin;
import redempt.redlib.RedLib;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Much of this code was borrowed from lucko's helper library. Adapted by byteful to support isolated class loaders.
public final class LibraryLoader {
  private static final Supplier<URLClassLoaderAccess> URL_INJECTOR = Suppliers.memoize(() -> URLClassLoaderAccess.create((URLClassLoader) NightMarketPlugin.class.getClassLoader()));
  private static final List<String> USED_NAMES = new ArrayList<>();

  public static IsolatedClassLoader load(NightMarketPlugin plugin, String groupId, String artifactId, String version) {
    return load(plugin, groupId, artifactId, version, "https://repo1.maven.org/maven2");
  }

  public static IsolatedClassLoader load(NightMarketPlugin plugin, String groupId, String artifactId, String version, String repoUrl) {
    return load(plugin, new Dependency(groupId, artifactId, version, repoUrl));
  }

  public static IsolatedClassLoader load(NightMarketPlugin plugin, Dependency d) {
    if (RedLib.MID_VERSION >= 17)
      return null; // We don't need to do this ourselves because Spigot has library loading support now!
    final String name = d.getArtifactId() + "-" + d.getVersion();
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

  public static void loadWithInject(NightMarketPlugin plugin, String groupId, String artifactId, String version) {
    loadWithInject(plugin, groupId, artifactId, version, "https://repo1.maven.org/maven2");
  }

  public static void loadWithInject(NightMarketPlugin plugin, String groupId, String artifactId, String version, String repoUrl) {
    loadWithInject(plugin, new Dependency(groupId, artifactId, version, repoUrl));
  }

  public static void loadWithInject(NightMarketPlugin plugin, Dependency d) {
    if (RedLib.MID_VERSION >= 17)
      return; // We don't need to do this ourselves because Spigot has library loading support now!

    final String name = d.getArtifactId() + "-" + d.getVersion();
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

  private static void download(NightMarketPlugin plugin, Dependency d, File saveLocation, String name) {
    plugin.getLogger().info(String.format("Loading dependency '%s:%s:%s'...", d.getGroupId(), d.getArtifactId(), d.getVersion()));

    if (!saveLocation.exists()) {
      try {
        plugin.getLogger().info("Dependency '" + name + "' not found in libraries folder. Attempting to download...");
        final URL url = d.getUrl();

        try (final InputStream is = url.openStream()) {
          Files.copy(is, saveLocation.toPath());
        }

      } catch (Exception e) {
        e.printStackTrace();
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

  public static void clearUnusedJars(NightMarketPlugin plugin) {
    final File folder = getLibFolder(plugin);

    for (File file : Objects.requireNonNull(folder.listFiles())) {
      if (!file.getName().endsWith(".jar") || USED_NAMES.contains(file.getName())) continue;

      file.delete();
    }
  }

  public static final class Dependency {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String repoUrl;

    public Dependency(String groupId, String artifactId, String version, String repoUrl) {
      this.groupId = Objects.requireNonNull(groupId, "groupId");
      this.artifactId = Objects.requireNonNull(artifactId, "artifactId");
      this.version = Objects.requireNonNull(version, "version");
      this.repoUrl = Objects.requireNonNull(repoUrl, "repoUrl");
    }

    public String getGroupId() {
      return this.groupId;
    }

    public String getArtifactId() {
      return this.artifactId;
    }

    public String getVersion() {
      return this.version;
    }

    public String getRepoUrl() {
      return this.repoUrl;
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

    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof Dependency)) return false;
      final Dependency other = (Dependency) o;
      return this.getGroupId().equals(other.getGroupId()) && this.getArtifactId().equals(other.getArtifactId()) && this.getVersion().equals(other.getVersion()) && this.getRepoUrl().equals(other.getRepoUrl());
    }

    @Override
    public int hashCode() {
      final int PRIME = 59;
      int result = 1;
      result = result * PRIME + this.getGroupId().hashCode();
      result = result * PRIME + this.getArtifactId().hashCode();
      result = result * PRIME + this.getVersion().hashCode();
      result = result * PRIME + this.getRepoUrl().hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "LibraryLoader.Dependency(" + "groupId=" + this.getGroupId() + ", " + "artifactId=" + this.getArtifactId() + ", " + "version=" + this.getVersion() + ", " + "repoUrl=" + this.getRepoUrl() + ")";
    }
  }
}
