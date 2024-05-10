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
  
  
  
  public static class Rest {
    
    public final static String API = "/api/";
    
    public final static String WITNESS = "witness";
    public final static String WITNESS_URI = API + WITNESS;
    public final static String UPDATE = "update";
    public final static String UPDATE_URI = API + UPDATE;
    public final static String STATE = "state";
    public final static String STATE_URI = API + STATE;
    
    public final static String QS_HASH = "hash";
    public final static String QS_UTC = "utc";
    public final static String QS_BLOCK = "block";
    public final static String QS_ENCODING = "enc";
    public final static String B64 = "b64";
    public final static String HEX = "hex";
    
    private Rest() { }
  }
  
  
  
  private Constants() { }
  
}
