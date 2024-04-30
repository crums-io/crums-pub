/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import io.crums.tc.ChainParams;

/**
 * Notary settings an end user might be interested in.
 * 
 * @see NotarySettings
 */
public class NotaryPolicy {

  
  public final static int MIN_BLOCKS_RETAINED = 3;
  
  
  public final static int MIN_BLOCK_COMMIT_LAG = 2;
  

  private final ChainParams params;
  
  private final int blocksRetained;
  
  private final int blockCommitLag;
  
  
  
  public NotaryPolicy(ChainParams params, int blocksRetained) {
    this(
        params, blocksRetained,
        Math.max((int) params.blockDuration() / 2, MIN_BLOCK_COMMIT_LAG));
  }
  

  public NotaryPolicy(
      ChainParams params, int blocksRetained, int blockCommitLag) {
    this.params = params; // (null checked below)
    this.blocksRetained = blocksRetained;
    this.blockCommitLag = blockCommitLag;
    
    if (blocksRetained < MIN_BLOCKS_RETAINED)
      throw new IllegalArgumentException(
          "blocksRetained (" + blocksRetained + ") < MIN_BLOCKS_RETAINED (" +
          MIN_BLOCKS_RETAINED + ")");
    if (blockCommitLag < MIN_BLOCK_COMMIT_LAG)
      throw new IllegalArgumentException(
          "blockCommitLag (" + blockCommitLag +
          ") <  MIN_BLOCK_COMMIT_LAG (" +
          MIN_BLOCK_COMMIT_LAG + ")");
    
    // (params null check too)
    if (blockCommitLag >=
        (blocksRetained / 3) * params.timeBinner().duration())
      
      throw new IllegalArgumentException(
          "blockCommitLag (" + blockCommitLag +
          ")  too large for given blocksRetained (" +
          blocksRetained + ") and binner " + params.timeBinner());
          
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
  
  
  
  
  /** Equality sans {@code Object.equals(..)} formalities. */
  public final boolean equalPolicy(NotaryPolicy other) {
    return
        params.equalParams(other.params) &&
        blockCommitLag == other.blockCommitLag &&
        blocksRetained == other.blocksRetained;
  }
  
  
}















