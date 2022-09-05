/*
 * Copyright 2020 Babak Farhang
 */
package io.crums.model;


import static io.crums.model.Constants.*;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

import io.crums.io.Serial;
import io.crums.io.channels.ChannelUtils;
import io.crums.model.hashing.ExpressionSymbols;
import io.crums.model.hashing.Statement;
import io.crums.util.IntegralStrings;
import io.crums.util.mrkl.Proof;
import io.crums.util.mrkl.hashing.ProofStatement;
import io.crums.util.mrkl.Tree;

/**
 * A  Merkle proof of a crum. Since the root hash is timestamped at multiple public
 * locations, this object proves a hash was witnessed within some error bars of
 * that recording. Note this has nothing to do with the crum's utc value.
 * 
 * @see #crum()
 * @see Proof
 */
public class CrumTrail extends Proof implements Serial {
  

  /**
   * Loads and returns an instance from the given stream. Does not read beyond
   * the crumtrail's data (which is of variable length).
   * 
   * @param in the steam to load from
   * 
   * @see #writeTo(ByteBuffer)
   */
  public static CrumTrail load(ReadableByteChannel in) throws IOException {
    return load(in, ByteBuffer.allocate(8));
  }

  /**
   * Loads and returns an instance from the given stream. Does not read beyond
   * the crumtrail's data (which is of variable length).
   * 
   * @param in the steam to load from
   * @param work work buffer with at least 8 bytes capacity (2k recommended)
   * 
   * @see #writeTo(ByteBuffer)
   */
  public static CrumTrail load(ReadableByteChannel in, ByteBuffer work) throws IOException {
    if (Objects.requireNonNull(work, "null work").capacity() < 8)
      throw new IllegalArgumentException("capacity < 8: " + work);
    
    work.clear().limit(8);
    ChannelUtils.readRemaining(in, work).flip();
    final int leafCount = work.getInt();
    final int leafIndex = work.getInt();
    
    final int chainLength = chainLength(leafCount, leafIndex);
    
    // prepare buffer to read chain
    {
      int capRequired = chainLength * HASH_WIDTH + Crum.DATA_SIZE;
      if (work.capacity() < capRequired)
        work = ByteBuffer.allocate(capRequired);
      else
        work.clear();
      
      work.limit(capRequired);
    }
    
    
    ChannelUtils.readRemaining(in, work).flip();
    
    byte[][] chain = new byte[chainLength][];
    for (int index = 0; index < chainLength; ++index) {
      byte[] hash = new byte[HASH_WIDTH];
      work.get(hash);
      chain[index] = hash;
    }
    
    ByteBuffer cbuf = ByteBuffer.allocate(Crum.DATA_SIZE);
    cbuf.put(work);
    // below unnecessary because the code after it validates
//    assert !cbuf.hasRemaining(); 
    
    Crum crum = new Crum(cbuf.flip());
    
    return new CrumTrail(leafCount, leafIndex, chain, crum);
  }
  
  
  public static CrumTrail load(ByteBuffer in) throws BufferUnderflowException {

    final int leafCount = in.getInt();
    final int leafIndex = in.getInt();

    final int chainLength = chainLength(leafCount, leafIndex);

    byte[][] chain = new byte[chainLength][];
    for (int index = 0; index < chainLength; ++index) {
      byte[] hash = new byte[HASH_WIDTH];
      in.get(hash);
      chain[index] = hash;
    }
    
    
    ByteBuffer cbuf = ByteBuffer.allocate(Crum.DATA_SIZE);

    if (in.remaining() > Crum.DATA_SIZE) {
      int savedLimit = in.limit();
      in.limit(in.position() + Crum.DATA_SIZE);
      
      cbuf.put(in);
      int pos = in.limit();
      in.limit(savedLimit).position(pos);
      
    } else
      cbuf.put(in);
    
    // below unnecessary because the code after it validates
//    assert !cbuf.hasRemaining(); 
    
    Crum crum = new Crum(cbuf.flip());
    
    return new CrumTrail(leafCount, leafIndex, chain, crum);
  }
  
  
  
  
  private final Crum crum;
  
  
  public CrumTrail(Tree tree, int leafIndex, Crum crum) {
    super(tree, leafIndex);
    this.crum = Objects.requireNonNull(crum, "null crum");
    assert verify();
  }

  public CrumTrail(int leafCount, int leafIndex, byte[][] chain, Crum crum) {
    super(Constants.HASH_ALGO, leafCount, leafIndex, chain);
    this.crum = Objects.requireNonNull(crum, "null crum");
    assert verify();
  }

  public CrumTrail(Proof proof, Crum crum) {
    super(proof);
    this.crum = Objects.requireNonNull(crum, "null crum");
    assert verify();
  }
  
  
  
  
  public final boolean verify() {
    try {
      return verifyTrail(MessageDigest.getInstance(HASH_ALGO));
    } catch (NoSuchAlgorithmException nsax) {
      throw new RuntimeException(nsax);
    }
  }

  

  /**
   * Returns the crum in this proof. The first element of the {@linkplain #hashChain()} is the
   * <em>hash</em> of the returned crum. 
   * 
   * @return the crum this proof is about
   */
  public final Crum crum() {
    return crum;
  }
  
  
  /**
   * Returns the SHA-256 hash of the {@linkplain #crum() crum}. This is just a
   * synonym for {@linkplain #item()}.
   */
  public final ByteBuffer hashedCrum() {
    return ByteBuffer.wrap(item());
  }
  
  /**
   * Verifies this crum trail. This involves both verifying the hash pointers that
   * make up the Merkle proof and also checking that the leaf hash indeed matches the hash of the crum. 
   */
  public final boolean verifyTrail(MessageDigest digest) {
    if (!verify(digest))
      return false;
    digest.reset();
    digest.update(crum.serialForm());
    byte[] crumHash = digest.digest();
    return Arrays.equals(crumHash, item());
  }
  
  
  
  @Override
  public ByteBuffer writeTo(ByteBuffer buffer) throws BufferOverflowException {
    buffer.putInt(leafCount()).putInt(leafIndex());
    for (byte[] link : hashChain())
      buffer.put(link);
    
    return buffer.put(crum.serialForm());
  }
  

  @Override
  public int serialSize() {
    return Crum.DATA_SIZE + 8 + HASH_WIDTH * chainLength(leafCount(), leafIndex());
  }
  
  
  /**
   * Returns this Merkle proof as a hash statement. On inspecting its LHS, the first
   * literals a human user sees are the hex of the {@linkplain #crum() crum}'s hash
   * and the hex of the crum's UTC expressed as an 8-byte sequence (size of long),
   * seperated by a space character. On the RHS, the user finds the value of the
   * root of the Merkle tree. This serves as a "full" hash pointer: it can be used to
   * construct a URL to reference the tree's permanent record at <code>https://crums.io</code>.
   * 
   * @return a statement that {@linkplain Statement#eval() eval}uates to <code>true</code>
   */
  public Statement toStatement() {
    ByteBuffer utcBuffer = ByteBuffer.wrap(new byte[8]);
    utcBuffer.putLong(crum.utc()).flip();
    StringBuilder merkleItem = new StringBuilder(HASH_WIDTH * 2 + 24)
        .append(ExpressionSymbols.LFT_PRNS)
        .append(crum.hashHex()).append(' ').append(IntegralStrings.toHex(utcBuffer))
        .append(ExpressionSymbols.RGT_PRNS);
    return ProofStatement.createItemStatement(this, merkleItem);
  }
  
  

  private final static int REF_UTC_BUFFER = 14 * 15_000;
  private final static int REF_URL_COUNT = 4;
  
  
  
  public String getRefUrl() {
    return
        "https://crums.io" + Constants.LIST_ROOTS_PATH + "?" + QS_UTC_NAME + "=" +
        (crum.utc() + REF_UTC_BUFFER) + "&" + QS_COUNT_NAME + "=-" + REF_URL_COUNT;
        
  }
  
}















