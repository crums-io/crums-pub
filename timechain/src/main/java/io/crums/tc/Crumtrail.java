/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;

import io.crums.io.Serial;
import io.crums.io.SerialFormatException;
import io.crums.io.buffer.BufferUtils;
import io.crums.sldg.Path;
import io.crums.tc.except.BlockNotFoundException;
import io.crums.tc.except.CargoConflictException;
import io.crums.tc.except.TimeChainException;

/**
 * Proof that a crum's {@linkplain Crum#witnessHash() witness hash} is
 * linked to a time chain. There are 2 versions of proofs:
 * <ol>
 * <li>A Merkle proof asserting the crum's (witness) hash in a Merkle tree,
 * coupled with a proof asserting said merkle tree's root hash, as
 * the cargo hash of a block in a time chain.</li>
 * <li>A proof asserting the crum's (witness) hash is written directly
 * in as the cargo hash of a block in a time chain.</li>
 * </ol>
 * <h2>TODO: Design peeve</h2>
 * <p>
 * The {@linkplain CargoProof} class ought to implement the logic of
 * both a merkle tree and a crum's straight hash (what this class
 * does). Punting for now, in the interest of time.
 * </p>
 */
public abstract class Crumtrail implements Serial {
  
  
  /**
   * Constructs a proof for when a single crum is recorded in the block.
   * The crum's {@linkplain Crum#witnessHash() witness hash} is recorded
   * directly in the cargo hash of some block in the time chain's
   * block proof.
   */
  public static LoneTrail newLoneTrail(BlockProof blockProof, Crum crum)
      throws TimeChainException {
    return new LoneTrail(blockProof, crum);
  }
  
  /**
   * Constructs a proof for when multiple crums are recorded in the block.
   * The crum's {@linkplain Crum#witnessHash() witness hash} is recorded
   * in a Merkle proof linking it to the root of a Merkle tree. The root
   * hash of the Merkle tree, in turn, is the block's cargo hash.
   */
  public static MerkleTrail newMerkleTrail(
      BlockProof blockProof, CargoProof cargoProof)
      throws TimeChainException {
    
    return new MerkleTrail(blockProof, cargoProof);
  }
  
  
  
  
  
  protected final BlockProof blockProof;

  
  private Crumtrail(BlockProof blockProof) {
    this.blockProof = Objects.requireNonNull(blockProof);
  }


  
  
  
  public final BlockProof blockProof() {
    return blockProof;
  }
  
  
  public final ChainParams chainParams() {
    return blockProof.chainParams();
  }
  
  
  /**
   * Returns the crum.
   */
  public abstract Crum crum();
  
  
  /**
   * Returns the hash observed in the block. When the block
   * records multiple crums, it's a hash of the collection
   * (root of a Merkle tree); when it records a single
   * crum, this is just the hash of that one crum.
   * 
   * @see #crumsInBlock()
   */
  public abstract ByteBuffer cargoHash();
  
  /**
   * Returns the number of crums encoded in the cargo hash.
   * 
   * @return positive
   * @see #cargoHash()
   */
  public abstract int crumsInBlock();
  
  
  /**
   * Determines if this is a {@linkplain MerkleTrail} instance.
   * If {@code true} is returned, then {@linkplain #asMerkleTrail()}
   * can safely be invoked.
   */
  public final boolean isMerkled() {
    return crumsInBlock() > 1;
  }
  
  /**
   * Returns (casts) this instance as a {@linkplain MerkleTrail}.
   * 
   * @throws ClassCastException
   *         if the instance is not {@linkplain #isMerkled() merkled}
   * @see #isMerkled()
   */
  public final MerkleTrail asMerkleTrail() throws ClassCastException {
    return (MerkleTrail) this;
  }




  public final Crumtrail trimToTarget(BlockProof chain) {

    final Path trailPath = blockProof.chainState();
    final Path chainPath = chain.chainState();
    final long hcbn = trailPath.highestCommonNo(chainPath);
    if (hcbn == 0)
      return this;

    final long blockNo = blockNo();

    final Path trimPath;
    
    if (hcbn == blockNo || chain.chainState().hasRowCovered(blockNo)) {

      trimPath = trailPath.tailPath(blockNo).headPath(blockNo + 1);
    
    } else if (hcbn < blockNo) {

      long iHcbn = trailPath.headPath(blockNo + 1L).highestCommonNo(chainPath);
      if (iHcbn == 0)
        return this;
      trimPath = trailPath.tailPath(iHcbn);

    } else {  // hcbn > blockNo

      trimPath = trailPath.tailPath(blockNo).headPath(hcbn + 1);
    }


    var trimBlockProof = new BlockProof(
      blockProof.chainParams(), trimPath, chain.getChainId());

    return isMerkled() ?
        new MerkleTrail(trimBlockProof, asMerkleTrail().cargoProof()) :
        new LoneTrail(trimBlockProof, crum());
  }
  

  
  /**
   * Returns the block number the crum's witness hash was first
   * recorded in the chain.
   */
  public final long blockNo() {
    return blockProof.chainParams().blockNoForUtc(crum().utc());
  }
  
  
  
  protected final void verifyCargoHashInChain()
      throws BlockNotFoundException, CargoConflictException {
    Crum crum = crum();
    final long blockNo = blockNo();
    Path chainState = blockProof.chainState();
    if (!chainState.hasRow(blockNo)) {
      throw new BlockNotFoundException(
          "block " + blockNo + " not found in state path " +
          chainState.rowNumbers())
          .setBlockNo(blockNo);
    }
    var block = chainState.getRowByNumber(blockNo);
    if (!cargoHash().equals(block.inputHash()))
      throw new CargoConflictException(
          "cargo hash in block " + blockNo + " must match " +
          (crumsInBlock() == 1 ? " crum witness" : "merkle root") +
          " hash; crum=" + crum)
          .setBlockNo(blockNo);
  }
  
  
  
  
  
  
  /**
   * A crum trail using the root hash of a Merkle proof as the block's
   * cargo hash.
   */
  public final static class MerkleTrail extends Crumtrail {
    
    protected final CargoProof cargoProof;

    public MerkleTrail(BlockProof blockProof, CargoProof cargoProof) {
      super(blockProof);
      this.cargoProof = Objects.requireNonNull(cargoProof);
      verifyCargoHashInChain();
    }

    
    public CargoProof cargoProof() {
      return cargoProof;
    }

    @Override
    public final Crum crum() {
      return cargoProof.crum();
    }

    @Override
    public final ByteBuffer cargoHash() {
      return ByteBuffer.wrap(cargoProof.rootHash()).asReadOnlyBuffer();
    }

    @Override
    public final int crumsInBlock() {
      return cargoProof.leafCount();
    }

    @Override
    public int serialSize() {
      return blockProof.serialSize() + cargoProof.serialSize();
    }

    @Override
    public ByteBuffer writeTo(ByteBuffer out) throws BufferOverflowException {
      blockProof.writeTo(out);
      cargoProof.writeTo(out);
      return out;
    }
    
    
    
  }
  
  
  /**
   * Proof for a lone crum in a block. The crum's witness
   * hash <em>is</em> the block's cargo hash.
   */
  public final static class LoneTrail extends Crumtrail {
    
    private final Crum crum;

    public LoneTrail(BlockProof blockProof, Crum crum) {
      super(blockProof);
      this.crum = Objects.requireNonNull(crum);
      verifyCargoHashInChain();
    }

    @Override
    public Crum crum() {
      return crum;
    }

    @Override
    public ByteBuffer cargoHash() {
      return ByteBuffer.wrap(crum.witnessHash()).asReadOnlyBuffer();
    }

    @Override
    public int crumsInBlock() {
      return 1;
    }

    @Override
    public int serialSize() {
      return 4 + Crum.DATA_SIZE + blockProof.serialSize();
    }

    @Override
    public ByteBuffer writeTo(ByteBuffer out) throws BufferOverflowException {
      blockProof.writeTo(out);
      out.putInt(1);
      crum.writeTo(out);
      return out;
    }
    
    
  }
  
  
  public static Crumtrail load(ByteBuffer in) throws SerialFormatException {
    try {
      var blockProof = BlockProof.load(in);
      int crums = in.getInt();
      if (crums < 1)
        throw new SerialFormatException("crum count: " + crums);
      
      if (crums == 1) {
        var cbuf = BufferUtils.slice(in, Crum.DATA_SIZE);
        var crum = new Crum(cbuf);
        return new LoneTrail(blockProof, crum);
      
      } else {
        var cargoProof = CargoProof.load(in, crums);
        return new MerkleTrail(blockProof, cargoProof);
      }
    } catch (SerialFormatException sfx) {
      throw sfx;
    } catch (Exception x) {
      throw new SerialFormatException(x);
    }
  }
  

}
