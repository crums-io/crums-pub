/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model;

/**
 * 
 */
public class Constants {

  /**
   * The maximum number hashes allowed to stamp in one REST request.
   */
  public final static int MAX_STAMP_COUNT = 100;
  
  
  /**
   * The maxmimum number of root trails listed in one REST request.
   */
  public final static int MAX_LIST_ROOTS_COUNT = 100;
  

  
  /**
   * Name of the hashing (digest) algorithm.
   * @see #HASH_WIDTH
   */
  public final static String HASH_ALGO = "SHA-256";
  
  /**
   * Width of the hash, in bytes.
   */
  public final static int HASH_WIDTH = 32;
  
  
  public final static String API_PATH = "/api/";
  
  
  
  
  public final static String STAMP_VERB = "stamp";
  
  public final static String VERIFY_STAMP_VERB = "verify_stamp";
  
  public final static String LIST_ROOTS_VERB = "list_roots";
  
  public final static String ROOT_TRAIL_VERB = "root_trail";
  
  public final static String BEACON_VERB = "beacon";
  
  public final static String QR_BEACON_VERB = "qr_beacon";
  
  
  
  
  public final static String CRUM_PATH = API_PATH + "crum";
  
  
  public final static String STAMP_PATH = API_PATH + "stamp";
  
  
  public final static String VERIFY_STAMP_PATH = API_PATH + "verify_stamp";
  
  
  public final static String LIST_ROOTS_PATH = API_PATH + "list_roots";
  
  
  public final static String ROOT_TRAIL_PATH = API_PATH + "root_trail";
  
  
  public final static String QS_HASH_NAME = "hash";
  
  
  public final static String QS_CRUMTRAIL_NAME = "crumtrail";
  
  
  public final static String QS_UTC_NAME = "utc";
  
  
  public final static String QS_COUNT_NAME = "count";
  
  
  public final static String RSP_STATUS_NAME = "status";
  
  
  public final static String RSP_STATUS_VAL_WITNESSED = "witnessed";
  
  
  public final static String RSP_STATUS_VAL_COMPLETED = "complete";
  
  
  public final static String RSP_UTC_JSON = QS_UTC_NAME;
  
  
  public final static String RSP_HASH_JSON = QS_HASH_NAME;
  
  
  public final static String RSP_MIN_UTC = "min_utc";
  
  
  public final static String RSP_MAX_UTC = "max_utc";
  
  
  public final static String RSP_TREE_NUM = "tree_num";
  
  
  public final static String RSP_WITNESS_DATE_JSON = "witnessed";
  
  
  public final static String JSON_CRUM = "crum";
  
  
  public final static String JSON_PROOF = "proof";
  

  
  /**
   * So that these constants can be extended.
   * Define, but don't instantiate.
   */
  protected Constants() { }

}
