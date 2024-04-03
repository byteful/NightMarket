package me.byteful.plugin.nightmarket.util.dependency;

import java.net.URL;
import java.net.URLClassLoader;

// Influenced by lucko's LuckPerms IsolatedClassLoader.
public class IsolatedClassLoader extends URLClassLoader {
  static {
    ClassLoader.registerAsParallelCapable();
  }

  public IsolatedClassLoader(URL... urls) {
    super(urls, ClassLoader.getSystemClassLoader().getParent());
  }
}
