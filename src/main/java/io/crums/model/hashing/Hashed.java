/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model.hashing;


import static io.crums.model.hashing.ExpressionSymbols.*;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import io.crums.util.hash.Digest;
import io.crums.util.hash.Digests;

/**
 * The hash of an entity's {@linkplain Entity#bytes() bytes}.
 */
final class Hashed extends ArgEntity implements Digest {
  
  /**
   * The default digest is SHA-256.
   * 
   * @see #digest()
   */
  public final static Digest DIGEST = Digests.SHA_256;
  
  
  public Hashed() {  }

  /**
   * 
   */
  public Hashed(Entity entity) {
    super(entity);
  }

  @Override
  public ByteBuffer bytes() {
    MessageDigest digest = newDigest();
    digest.update(arg().bytes());
    return ByteBuffer.wrap(digest.digest()).asReadOnlyBuffer();
  }
  
  
//  public boolean equals(Entity entity) {
//    return
//        entity instanceof Hashed ?
//            equals((Hashed) entity) :
//              super.equals(entity);
//  }
//  
//
//  /**
//   * A more efficient implementation of {@linkplain #equals(Entity)}.
//   * 
//   * @param other non-null
//   */
//  public boolean equals(Hashed other) {
//    return other == this || other.arg().equals(entity);
//  }
  


  @Override
  char openChar() {
    return LFT_PRNS;
  }

  @Override
  char closeChar() {
    return RGT_PRNS;
  }
  
  
  
  
  
  
  // -- DIGEST METHODS --

  

  @Override
  public int hashWidth() {
    return digest().hashWidth();
  }

  @Override
  public String hashAlgo() {
    return digest().hashAlgo();
  }

  @Override
  public MessageDigest newDigest() {
    return digest().newDigest();
  }

  @Override
  public ByteBuffer sentinelHash() {
    return digest().sentinelHash();
  }
  
  /**
   * Override hook to change the hash algo.
   */
  protected Digest digest() {
    return DIGEST;
  }

}











