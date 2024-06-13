package me.byteful.plugin.nightmarket.util.dependency;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Provides access to {@link URLClassLoader}#addURL.
 */
// Brought from lucko's helper library. Only works on older Java versions!
public abstract class URLClassLoaderAccess {

  private final URLClassLoader classLoader;

  protected URLClassLoaderAccess(URLClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Creates a {@link URLClassLoaderAccess} for the given class loader.
   *
   * @param classLoader the class loader
   * @return the access object
   */
  static URLClassLoaderAccess create(URLClassLoader classLoader) {
    if (Reflection.isSupported()) {
      return new Reflection(classLoader);
    } else {
      return Noop.INSTANCE;
    }
  }

  /**
   * Adds the given URL to the class loader.
   *
   * @param url the URL to add
   */
  public abstract void addURL(URL url);

  /**
   * Accesses using reflection, not supported on Java 9+.
   */
  private static class Reflection extends URLClassLoaderAccess {
    private static final Method ADD_URL_METHOD;

    static {
      Method addUrlMethod;
      try {
        addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrlMethod.setAccessible(true);
      } catch (Exception e) {
        addUrlMethod = null;
      }
      ADD_URL_METHOD = addUrlMethod;
    }

    Reflection(URLClassLoader classLoader) {
      super(classLoader);
    }

    private static boolean isSupported() {
      return ADD_URL_METHOD != null;
    }

    @Override
    public void addURL(URL url) {
      try {
        ADD_URL_METHOD.invoke(super.classLoader, url);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class Noop extends URLClassLoaderAccess {
    private static final Noop INSTANCE = new Noop();

    private Noop() {
      super(null);
    }

    @Override
    public void addURL(URL url) {
      throw new UnsupportedOperationException("NightMarket is unable to inject classes properly into your Java runtime! Please contact byteful on discord about this!");
    }
  }

}
