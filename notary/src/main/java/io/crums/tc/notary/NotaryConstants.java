/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary;

/**
 * 
 */
public class NotaryConstants {

  /** Default log name. */
  public final static String NOTARY_LOG = "notary";
  /** Notary properties filename. */
  public final static String NOTARY_PROPS = "notary.properties";
  
  /** Name of staging subdirectory. */
  public final static String STAGING_DIR = "STAGING";
  /** Crum Merkle tree filename. */
  public final static String MRKL = "MRKL";
  /** Crum witness hash filename. */
  public final static String WHASH = "WHASH";
  

  public final static String TIME_CHAIN_EXT = ".ergo";
  public final static String CHAIN = "CHAIN" + TIME_CHAIN_EXT;
  /** Cargo directory  */
  public final static String CARGO_DIR = "CARGO";
//  public final static String CARGO_CHAIN_EXT = ".ccc";
  public final static String CARGO_BLOCK_EXT = ".cb";
  /** Crum filename extension. Includes dot. */
  public final static String CRUM_EXT = ".crum";
  
  public final static String BAD_FILE_EXT = ".BAD";
  
  private NotaryConstants() {  }

}
