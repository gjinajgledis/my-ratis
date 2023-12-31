package org.example.myratis.common.util;


import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public interface NetUtils {
  abstract class StaticResolution {
    /** Host -> resolved name */
    private static final Map<String, String> HOST_TO_RESOLVED = new ConcurrentHashMap<>();

    /** Adds a static resolution for host. */
    public static void put(String host, String resolvedName) {
      HOST_TO_RESOLVED.put(host, resolvedName);
    }

    /** @return the resolved name, or null if the host is not found. */
    public static String get(String host) {
      return HOST_TO_RESOLVED.get(host);
    }
  }

  /** The same as createSocketAddr(target, -1). */
  static InetSocketAddress createSocketAddr(String target) {
    return createSocketAddr(target, -1);
  }

  /**
   * Create an InetSocketAddress from the given target and default port.
   * @param target a string of either "host", "host:port" or "scheme://host:port/path"
   * @param defaultPort the default port if <code>target</code> does not
   *                    include a port number
   */
  static InetSocketAddress createSocketAddr(String target, int defaultPort) {
    target = Objects.requireNonNull(target, "target == null").trim();
    boolean hasScheme = target.contains("://");
    if (!hasScheme && target.charAt(0) == '/') {
      target = target.substring(1);
    }
    final URI uri;
    try {
      uri = new URI(hasScheme? target: "dummy://"+target);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Failed to create URI from target " + target, e);
    }

    final String host = uri.getHost();
    int port = uri.getPort();
    if (port == -1) {
      port = defaultPort;
    }
    final String path = uri.getPath();

    if (host == null) {
      throw new IllegalArgumentException("Host is null in " + target);
    } else if (port < 0) {
      throw new IllegalArgumentException("Port = " + port + " < 0 in " + target);
    } else if (!hasScheme && path != null && !path.isEmpty()) {
      throw new IllegalArgumentException("Unexpected path in " + target);
    }
    return createSocketAddrForHost(host, port);
  }

  /**
   * Create a socket address with the given host and port.  The hostname
   * might be replaced with another host that was set via
   * {@link StaticResolution#put(String, String)}.
   * @param host the hostname or IP use to instantiate the object
   * @param port the port number
   * @return InetSocketAddress
   */
  static InetSocketAddress createSocketAddrForHost(String host, int port) {
    String staticHost = StaticResolution.get(host);
    String resolveHost = (staticHost != null) ? staticHost : host;

    InetSocketAddress addr;
    try {
      InetAddress iaddr = InetAddress.getByName(resolveHost);
      // if there is a static entry for the host, make the returned
      // address look like the original given host
      if (staticHost != null) {
        iaddr = InetAddress.getByAddress(host, iaddr.getAddress());
      }
      addr = new InetSocketAddress(iaddr, port);
    } catch (UnknownHostException e) {
      addr = InetSocketAddress.createUnresolved(host, port);
    }
    return addr;
  }

  /**
   * Creates {@code count} unique local addresses.  They may conflict with
   * addresses created later, but not with one another.  Addresses are
   * guaranteed to be bound to the loopback interface.
   * @param count number of unique local addresses to create
   * @return {@code count} number of unique local addresses
   */
  static List<InetSocketAddress> createLocalServerAddress(int count) {
    List<InetSocketAddress> list = new ArrayList<>(count);
    List<ServerSocket> sockets = new ArrayList<>(count);
    try {
      for (int i = 0; i < count; i++) {
        ServerSocket s = new ServerSocket();
        sockets.add(s);
        s.setReuseAddress(true);
        s.bind(new InetSocketAddress(InetAddress.getByName(null), 0), 1);
        list.add((InetSocketAddress) s.getLocalSocketAddress());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      IOUtils.cleanup(null, sockets.toArray(new Closeable[0]));
    }
    return list;
  }

  /**
   * Creates a unique local address.  Addresses are guaranteed to be bound to
   * the loopback interface.
   * @return unique local address
   */
  static InetSocketAddress createLocalServerAddress() {
    try(ServerSocket s = new ServerSocket()) {
      s.setReuseAddress(true);
      s.bind(new InetSocketAddress(InetAddress.getByName(null), 0), 1);
      return (InetSocketAddress) s.getLocalSocketAddress();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static String address2String(InetSocketAddress address) {
    if (address == null) {
      return null;
    }
    final StringBuilder b = new StringBuilder(address.getHostName());
    if (address.getAddress() instanceof Inet6Address) {
      b.insert(0, '[').append(']');
    }
    return b.append(':').append(address.getPort()).toString();
  }
}