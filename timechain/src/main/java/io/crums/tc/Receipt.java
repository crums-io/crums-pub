/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import java.util.Objects;
import java.util.Optional;

/**
 * A receipt vended out by a {@linkplain NotaryService}.
 * There are 2 general types: <em>un</em>-trailed, and trailed. Receipts
 * typically start out untrailed when a hash is first {@linkplain
 * NotaryService#witness(java.nio.ByteBuffer) witness}ed, and then become
 * trailed following an {@linkplain NotaryService#update(Crum) update}.
 * 
 * <h2>Trailed Receipts</h2>
 * <p>
 * There are 2 representations (hash proofs) for trailed receipts: compressed,
 * and un-compressed. Compressed instances lose significant auxilliary info
 * about the intermediate time chain block hashes, however, in most usecases,
 * this is welcome. The only disadvantage is that compressed block proofs
 * sometimes cannot be stitched together (extend one to another) off-line,
 * where if they weren't compressed, they could have.
 * </p>
 * 
 */
public class Receipt {
  
  private final ChainParams params;
  private final Crum crum;
  private final Crumtrail trail;
  
  
  /** Creates an untrailed instance. */
  public Receipt(ChainParams params, Crum crum) {
    this.params = Objects.requireNonNull(params);
    this.crum = Objects.requireNonNull(crum);
    if (params.blockNoForUtcUnchecked(crum.utc()) <= 0)
      throw new IllegalArgumentException(
          "params/crum.utc() mismatch: " + params + "/" + crum);
    this.trail = null;
  }
  
  /** Creates a trailed instance. */
  public Receipt(Crumtrail trail) {
    this.params = null;
    this.crum = null;
    this.trail = Objects.requireNonNull(trail);
  }
  
  /** Copy constructor. */
  public Receipt(Receipt copy) {
    this.params = copy.params;
    this.crum = copy.crum;
    this.trail = copy.trail;
  }


  /** Compression constructor. {@code compress} param is ignored. */
  private Receipt(Receipt copy, boolean compress) {
    this.params = null;
    this.crum = null;

    this.trail = copy.trail.compress();
  }
  
  
  
  
  public final ChainParams chainParams() {
    return trail == null ? params : trail.chainParams();
  }
  

  public final Crum crum() {
    return trail == null ? crum : trail.crum();
  }
  
  /** Returns the block no. associated with the crum's utc. */
  public final long blockNo() {
    return chainParams().blockNoForUtc(crum().utc());
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


  /**
   * An instance is compressed if it either has no trail,
   * or its trail is compressed.
   * 
   * @return {@code !hasTrail() || trail().isCompressed()}
   */
  public final boolean isCompressed() {
    return trail == null || trail.isCompressed();
  }


  /** 
   * Returns this instance if already {@linkplain #isCompressed() compress}ed;
   * a compressed version of this instance, otherwise.
   */
  public final Receipt compress() {
    return
        isCompressed() ?
            this :
            new Receipt(this, true);
  }

}





