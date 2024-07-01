/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.json;

/**
 * 
 */
public class JsonTags {

  public final static String UTC = "utc";
  /**
   * Witnessed (user-submitted) hash.
   */
  public final static String W_HASH = "w_hash";
  
  public final static String HASH = "hash";
  
  public final static String CRUM = "crum";
  
  public final static String  CHAIN_PARAMS = "chain_params";

  public final static String CN_BIN_EXP = "bin_exp";

  public final static String CN_INCEPT_UTC = "incept_utc";
  
  public final static String CARGO_PROOF = "cargo_proof";
  
  public final static String BLOCK_PROOF = "block_proof";

  public final static String INDEX = "index";
  public final static String CRUM_COUNT = "cc";
  public final static String MRKL_PROOF_HASHES = "mp_hashes";
  
  public final static String BP_STITCH_NOS = "bp_stitch_nos";
  public final static String BP_TYPE = "bp_type";
  public final static String BP_HASHES = "hashes";


  public final static String P_BLOCK_COMMIT_LAG = "block_commit_lag";
  public final static String P_BLOCKS_SEARCHED = "blocks_searched";
  public final static String P_BLOCKS_RETAINED = "blocks_retained";
  
  private JsonTags() {  }

}
