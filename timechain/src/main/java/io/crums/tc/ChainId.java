/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Informational identifier for a time chain.
 */
public record ChainId(
    String name,
    Optional<URI> origin) {

  public ChainId {
    Objects.requireNonNull(name, "null name");
    Objects.requireNonNull(origin);
  }
  
  public ChainId(String name) {
    this(name, Optional.empty());
  }

}
