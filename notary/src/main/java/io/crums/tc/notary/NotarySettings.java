/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;

import io.crums.tc.ChainParams;

/**
 * Notary settings. The parts of the settings an end user might
 * be interested (and which therefore might be made public)
 * are gathered in the base class {@linkplain NotaryPolicy}.
 */
public final class NotarySettings extends NotaryPolicy {
  
  public final static int DEFAULT_MAX_CROSS_MACHINE_TIME_SKEW = 8;

  /**
   * Returns the maximum value {@link #maxConcurrentLag()} can
   * take. (Didn't want to name it {@code maxMaxConc..}). It's
   * also the value overloaded constructors default to.
   * 
   * @param chainParams  the bin duration determines return value
   * @return {@code chainParams.timeBinner().duration() / 2}
   */
  public static int maxConcurrentLag(ChainParams chainParams) {
    return chainParams.timeBinner().duration() / 2;
  }

  private final int maxConcurrentLag;
  private final int maxCrossMachineTimeSkew;
  
  
  public NotarySettings(NotaryPolicy policy) {
    super(policy);
    this.maxConcurrentLag = maxConcurrentLag(policy.chainParams());
    this.maxCrossMachineTimeSkew =
        DEFAULT_MAX_CROSS_MACHINE_TIME_SKEW;
  }
  
  /**
   * 
   * @param policy            base settings (public)
   * @param maxConcurrentLag  see {@link #maxConcurrentLag()}
   */
  public NotarySettings(
      NotaryPolicy policy,
      int maxConcurrentLag,
      int maxCrossMachineTimeSkew) {
    
    super(policy);
    this.maxConcurrentLag = maxConcurrentLag;
    this.maxCrossMachineTimeSkew = maxCrossMachineTimeSkew;
    
    if (maxConcurrentLag > maxConcurrentLag(chainParams()))
      throw new IllegalArgumentException(
          "maxConcurrentLag (" + maxConcurrentLag +
          ") must be at least 1/2 of block duration (" +
          chainParams().timeBinner().duration() +
          ")");
    
    if (maxCrossMachineTimeSkew >= maxConcurrentLag)
      throw new IllegalArgumentException(
          "maxCrossMachineTimeSkew (" + maxCrossMachineTimeSkew +
          ") must be lesss than maxConcurrentLag (" +
          maxConcurrentLag + ")");
          
  }
  
  

  /**
   * Maximum time (millis) allowed for a newly seen hash to cross certain
   * critical sections of code. For the most part, this amounts to defining
   * maximum duration a {@link FreshCrum} is considered valid on input.
   * <p>
   * The returned value is no greater than half [time] bin duration.
   * </p>
   * <h4>Warning</h4>
   * <p>
   * <em>Setting this to too low a value may cause the VM to exit on an
   * assertion error.</em> (But that should happen only under load, which
   * is the point of this.)
   * </p>
   */
  public int maxConcurrentLag() {
    return maxConcurrentLag;
  }

  /**
   * Maximum clock skew (millis) across machines. The returned value
   * is less than {@linkplain #maxConcurrentLag()}.
   */
  public long maxCrossMachineTimeSkew() {
    return maxCrossMachineTimeSkew;
  }
}
