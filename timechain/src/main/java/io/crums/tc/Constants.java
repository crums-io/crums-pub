/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc;

import io.crums.util.hash.Digest;
import io.crums.util.hash.Digests;

/**
 * 
 */
public class Constants {

  
  public final static Digest DIGEST = Digests.SHA_256; 

  /**
   * Name of the hashing (digest) algorithm.
   * @see #HASH_WIDTH
   */
  public final static String HASH_ALGO = DIGEST.hashAlgo();
  
  /**
   * Width of the hash, in bytes.
   */
  public final static int HASH_WIDTH = DIGEST.hashWidth();
  
  
  
  
  private Constants() { }
  
}
