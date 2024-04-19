/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import io.crums.io.Serial;
import io.crums.sldg.MemoPathPack;
import io.crums.sldg.Path;
import io.crums.sldg.PathPack;

/**
 * Proof that a block belongs in a chain. This uses the skip ledger
 * data structure / commitment scheme.
 */
public class BlockProof implements Serial {
  
  private final ChainParams params;
  private final Path chainState;
  
  // TBD field.. the plan is to tombstone chain state hashes in 
  // other chains: together they weave a network of causal structures.
  // A chain then will be able to package proofs about its validity
  // relative to other chains. 
  private final Optional<ChainId> chainId;
  
  
  public BlockProof(ChainParams params, Path chainState) {
    this(params, chainState, Optional.empty());
  }

  /**
   * 
   * @param params
   * @param chainState
   * @param chainId
   */
  public BlockProof(ChainParams params, Path chainState, Optional<ChainId> chainId) {
    this.params = Objects.requireNonNull(params);
    this.chainState = chainState;
    this.chainId = Objects.requireNonNull(chainId);
    
    if (chainState.loRowNumber() != 1)
      throw new IllegalArgumentException(
          "block no. 1 missing in chain state path");
    
  }
  
  
  /**
   * Returns the last block no.
   */
  public final long blockNo() {
    return chainState.hiRowNumber();
  }
  
  /**
   * Returns the UTC for the start of the last block
   */
  public final long blockUtc() {
    return params.utcForBlockNo(blockNo());
  }
  
  
  /**
   * Returns the chain parameters. This determines the time
   * boundaries each block represents.
   */
  public final ChainParams chainParams() {
    return params;
  }
  
  
  /**
   * A skip ledger path from a lower block no. (usually the first)
   * to a higher block no. (typically the last) in the time chain.
   * The maximum length of this path [of hash proofs] is of
   * <strong>O(</strong>{@code log(blockCount())}<strong>)</strong>.
   */
  public final Path chainState() {
    return chainState;
  }
  
  
  public Optional<ChainId> getChainId() {
    return chainId;
  }


  public final TimeBinner timeBinner() {
    return params.timeBinner();
  }

  @Override
  public int serialSize() {
    return params.serialSize() + chainState.pack().serialSize();
  }
  
  
  /**
   * 
   */
  @Override
  public final boolean equals(Object o) {
    return o == this ||
        o instanceof BlockProof bproof &&
        chainState.last().equals(bproof.chainState().last()) &&
        chainState.rowNumbers().equals(bproof.chainState.rowNumbers());
  }
  
  
  /**
   * Consistent with {@linkplain #equals(Object)}, but expensive. Avoid.
   */
  @Override
  public final int hashCode() {
    return
        chainState.rowNumbers().hashCode() ^
        chainState.last().hash().hashCode();
  }
  
  
  

  /**
   * @see #load(ByteBuffer)
   */
  @Override
  public ByteBuffer writeTo(ByteBuffer out) throws BufferOverflowException {
    params.writeTo(out);
    chainState.pack().writeTo(out);
    return out;
  }
  
  
  public static BlockProof load(ByteBuffer in) {
    return load(in, true);
  }
  
  
  /**
   * Loads and returns an instance, reading the given buffer.
   * 
   * @param precompute  if {@code true} the returned instance's
   *                    hash computations are memo-ised (faster to use)
   * @return            deserialized instance
   */
  public static BlockProof load(ByteBuffer in, boolean precompute) {
    var params = ChainParams.load(in);
    Path chainState;
    {
      var pack = PathPack.load(in);
      if (precompute)
        pack = new MemoPathPack(pack);
      chainState = pack.path();
    }
    return new BlockProof(params, chainState);
  }

}


