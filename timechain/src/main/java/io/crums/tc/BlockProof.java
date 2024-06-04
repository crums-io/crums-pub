/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import io.crums.io.Serial;
import io.crums.sldg.HashConflictException;
import io.crums.sldg.MemoPathPack;
import io.crums.sldg.Path;
import io.crums.sldg.PathPack;
import io.crums.sldg.Row;
import io.crums.sldg.SkipLedger;

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
  
  
  /** Creates an instance with no ID. */
  public BlockProof(ChainParams params, Path chainState) {
    this(params, chainState, Optional.empty());
  }

  /**
   * Full constructor.
   * 
   * @param chainId TBD, empty by default
   */
  public BlockProof(
      ChainParams params, Path chainState, Optional<ChainId> chainId) {
    this.params = Objects.requireNonNull(params);
    this.chainState = Objects.requireNonNull(chainState);
    this.chainId = Objects.requireNonNull(chainId);
    
  }



  /**
   * Extends the block proof by stitching a [hash proof] path forward
   * from the given target block no to the highest block in the {@code other}
   * proof. If the other proof does not intersect with this one, or if
   * it does not extend this proof (to a higher block no.), then this
   * instance is returned.
   * <p>
   * On success, the returned proof contains the "head" blocks in this proof,
   * up to the block numbered {@code targetBlockNo}, followed by a <em>skip
   * path</em> from {@code targetBlockNo} to the last block in the other
   * proof. This way, by repeated invoking this method, one can build proofs
   * that contain a set of target block no.s on the left side (head), and a
   * [hash] skip path to the timechain's latest block on the right side (tail).
   * </p>
   * 
   * @throws HashConflictException
   *         if the given block proof is provably not from the same chain
   *         (i.e. block hashes conflict at one or more block no.s)
   */
  public final BlockProof extendTarget(long targetBlockNo, BlockProof other)
      throws HashConflictException {

    if (targetBlockNo <= 0)
      throw new IllegalArgumentException("targetBlockNo: " + targetBlockNo);
    
    if (!chainState.hasRow(targetBlockNo))
      throw new IllegalArgumentException(
          "targetBlockNo (" + targetBlockNo + ") not in: " +
          chainState.rowNumbers());


    // verify the *known* common blocks are in agreement
    {
      long hbn = highestCommonBlockNo(other);
      if (hbn < targetBlockNo || hbn == other.blockNo())
        return this;
    }
    
    final int tbnIndex =
        Collections.binarySearch(chainState.rowNumbers(), targetBlockNo);

    assert tbnIndex >= 0;

    var blocks = new ArrayList<Row>();
    blocks.addAll(chainState.rows().subList(0, tbnIndex + 1));
    
    var skipNos = SkipLedger.skipPathNumbers(targetBlockNo, other.blockNo());
    for (long bn : skipNos.subList(1, skipNos.size()))
      blocks.add(other.chainState.getRowByNumber(bn));

    Path statePath = new Path(blocks).pack().path();
    
    return new BlockProof(params, statePath);
  }



  /**
   * Returns the highest block no. whose hash is known by both this and the
   * {@code other} block proof.
   * 
   * @param other   block proof from the same chain this instance is from
   * 
   * @return  the highest block no, or zero, if the 2 instances do not
   *          intersect
   * 
   * @throws HashConflictException if the hashes at the highest known block no.s
   *                               for the 2 instances conflict
   * @see #extendTarget(long, BlockProof)
   * @see #highestCommonBlockNo(Path)
   */
  public final long highestCommonBlockNo(BlockProof other)
      throws HashConflictException {

    return highestCommonBlockNo(other.chainState);
  }


  /**
   * Returns the highest block no. whose hash is known by both this and the
   * {@code other} hash path proof. The hashes of the blocks at that block no.
   * are checked for consistency. (Since {@code Path} instances are validated
   * on construction, we needn't validate the hash of the lower numbered blocks
   * in the proof.)
   * 
   * <h4>Meaning of Return Value</h4>
   * <p>
   * The return value means we <em>proved</em> the 2 block proofs are in
   * agreement from the genesis block [1] to the block numbered the return
   * value.
   * </p><p>
   * <em>This property is symmetric</em>. I.e.
   * {@code a.highestCommonBlockNo(b) == b.highestCommonBlockNo(a)} is always
   * {@code true}.
   * </p>
   * 
   * @param other   block path proof from the same chain this instance is from
   * 
   * @return  the highest block no, or zero, if the 2 instances do not
   *          intersect
   * 
   * @throws HashConflictException if the hashes at the highest known block no.s
   *                               for the 2 instances conflict
   * @see #extendTarget(long, BlockProof)
   */
  public final long highestCommonBlockNo(Path other)
      throws HashConflictException {

    return chainState.highestCommonNo(other);
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


