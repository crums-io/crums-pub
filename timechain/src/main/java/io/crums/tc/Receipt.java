/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import java.util.Objects;
import java.util.Optional;

/**
 * 
 */
public class Receipt {
  
  private final ChainParams params;
  private final Crum crum;
  private final Crumtrail trail;
  
  
  public Receipt(ChainParams params, Crum crum) {
    this.params = Objects.requireNonNull(params);
    this.crum = Objects.requireNonNull(crum);
    if (params.blockNoForUtcUnchecked(crum.utc()) <= 0)
      throw new IllegalArgumentException(
          "params/crum.utc() mismatch: " + params + "/" + crum);
    this.trail = null;
  }
  
  public Receipt(Crumtrail trail) {
    this.params = null;
    this.crum = null;
    this.trail = Objects.requireNonNull(trail);
  }
  
  public Receipt(Receipt copy) {
    this.params = copy.params;
    this.crum = copy.crum;
    this.trail = copy.trail;
  }
  
  
  
  
  public final ChainParams chainParams() {
    return trail == null ? params : trail.chainParams();
  }
  
  public final Crum crum() {
    return trail == null ? crum : trail.crum();
  }
  
  public final long blockNo() {
    return params.blockNoForUtc(crum.utc());
  }
  
  public final Crumtrail trail() {
    return trail;
  }
  
  public final boolean hasTrail() {
    return trail != null;
  }
  
  public final Optional<Crumtrail> trailOpt() {
    return Optional.ofNullable(trail);
  }
}





