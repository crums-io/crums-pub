/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import java.net.URI;
import java.net.URISyntaxException;
/**
 * Informational identifier for a time chain.
 */
public class ChainId {
  
  private final String hostUri;

  public ChainId(String hostUri) {
    this.hostUri = hostUri.toLowerCase();
    checkHostUri();
  }



  public final String hostUri() {
    return hostUri;
  }


  



  private void checkHostUri() {
    URI host;
    try {
      host = new URI(hostUri);
    } catch (URISyntaxException usx) {
      throw new IllegalArgumentException("illegal syntax for hostUri: " + hostUri);
    }
    if (!host.isAbsolute())
      throw new IllegalArgumentException(
        "host URI must be absolute: " + host);
    var scheme = host.getScheme().toLowerCase();
    if (!scheme.equals("http") && !scheme.equals("https"))
      throw new IllegalArgumentException(
        "illegal scheme (" + host.getScheme() + "): " + host);
    if (host.getHost() == null)
      throw new IllegalArgumentException(
        "host URI has no host: " + host);
    if (host.getRawPath() != null)
      throw new IllegalArgumentException(
        "host URI must not specify a path: " + host);
  }
}
