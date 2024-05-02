/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;


import java.nio.ByteBuffer;

import io.crums.tc.Constants;
import io.crums.util.IntegralStrings;

/**
 * Cargo block hash / crums-in-block tuple.
 * <em>Do not access the {@linkplain #hash} member directly since
 * its positional state is mutable!</em> Instead, use the
 * {@linkplain #hash()} method which returns a thread-safe,
 * cleared duplicate.
 * 
 * @param hash  the hash of the cargo block (which ultimately becomes
 *              the cargo hash of the time chain block). Exactly
 *              32-bytes remaining. Sliced, but
 *              <em>do not modify contents!</em>
 * @param crums the number of crums recorded in the cargo block (&ge; 0)
 */
public record CargoHash(ByteBuffer hash, int crums) {
  
  private final static ByteBuffer SN = Constants.DIGEST.sentinelHash();
  
  public final static CargoHash EMPTY = new CargoHash(SN, 0);
  
  public CargoHash {
    var in = hash;
    // duplicate it, so that positionally independent
    hash = hash.asReadOnlyBuffer();
    if (hash.capacity() != hash.remaining())
      hash = hash.slice();
    if (hash.remaining() != Constants.HASH_WIDTH)
      throw new IllegalArgumentException(
          "expected " + Constants.HASH_WIDTH +
          " remaining bytes; actual " + hash.remaining() + ": " + in);
    
    if (crums < 1) {
      if (crums == 0) {
        if (!SN.equals(hash))
          throw new IllegalArgumentException(
              "expected sentinel hash for crums=0; actual given was " +
              IntegralStrings.toHex(hash.limit(4)) + "..");
      } else
        throw new IllegalArgumentException("negative crums: " + crums);
    }
  }
  
  
  public CargoHash(byte[] hash, int crums) {
    this(ByteBuffer.wrap(hash), crums);
  }
  
  
  /**
   * Returns a read-only duplicate of the hash.
   * <em>Do not access the {@linkplain #hash} member directly since
   * its positional state is mutable.</em>
   * 
   * @return
   */
  public ByteBuffer hash() {
    return hash.duplicate().clear();
  }

}
