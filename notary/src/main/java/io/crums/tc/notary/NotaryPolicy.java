/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;

import java.util.Objects;

import io.crums.tc.ChainParams;

/**
 * Notary settings an end user might be interested in.
 * 
 * @see NotarySettings
 */
public class NotaryPolicy {
  
  
  public final static int MIN_BLOCK_COMMIT_LAG = 128;
  

  private final ChainParams params;
  
  private final int blocksRetained;
  
  private final int blockCommitLag;

  public NotaryPolicy(
      ChainParams params, int blocksRetained, int blockCommitLag) {
    this.params = Objects.requireNonNull(params);
    this.blocksRetained = blocksRetained;
    this.blockCommitLag = blockCommitLag;
    
    if (blocksRetained < 1)
      throw new IllegalArgumentException(
          "blockRetained: " + blocksRetained);
    if (blockCommitLag < MIN_BLOCK_COMMIT_LAG)
      throw new IllegalArgumentException(
          "blockCommitLag (" + blockCommitLag +
          ") <  MIN_BLOCK_COMMIT_LAG (" +
          MIN_BLOCK_COMMIT_LAG + ")");
  }
  
  
  /** Copy / promotion constructor. */
  protected NotaryPolicy(NotaryPolicy policy) {
    this.params = policy.params;
    this.blocksRetained = policy.blocksRetained;
    this.blockCommitLag = policy.blockCommitLag;
  }

  

  public final ChainParams chainParams() {
    return params;
  }
  
  
  
  public final int blocksRetained() {
    return blocksRetained;
  }

  
  /**
   * Number of milliseconds after which a cargo block is considered
   * committed. This, in turn, establishes the earliest time a
   * cargo block can be built.
   * 
   * @return &ge; {@link #MIN_BLOCK_COMMIT_LAG}
   */
  public final int blockCommitLag() {
    return blockCommitLag;
  }
  
}
