/*
 * Copyright 2024 Babak Farhang
 */
package io.crums.tc.notary.server;


/**
 * Server-specific constants.
 */
public class ErgdConstants {

  /**
   * Version string.
   * <p>
   * TODO: since this is duplicated data (it sorta follows the pom version),
   * we need to at least enumerate all places a version string is used
   * so that they may be updated en-bloc. Plus, an out-of-box unit test
   * checking for version string consistency (?).
   * </p>
   */
  public final static String VERSION = "0.1.0-ALPHA";

  /**
   * HTTP response "Server" header value.
   */
  public final static String SERVER = "ergd " + VERSION;

}
