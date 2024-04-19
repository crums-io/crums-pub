/*
 * Copyright 2021 Babak Farhang
 */
package io.crums.model;


import java.nio.ByteBuffer;
import java.util.Objects;

import io.crums.io.buffer.BufferUtils;
import io.crums.util.IntegralStrings;


/**
 * A beacon hash paired with a UTC time.
 * 
 * @see #getRefUrl()
 * @see Beacon.Info
 */
public class Beacon extends ProtoBeacon {

  private final ByteBuffer hash;
  
  
  public Beacon(ByteBuffer hash, long utc) {
    super(utc);
    this.hash = BufferUtils.readOnlySlice(Objects.requireNonNull(hash, "null hash"));
    if (hash.remaining() != Constants.HASH_WIDTH)
      throw new IllegalArgumentException("illegal hash-width: " + hash);
  }
  
  /**
   * Returns a read-only view of the beacon hash.
   */
  public ByteBuffer hash() {
    return hash.asReadOnlyBuffer();
  }
  
  /**
   * Returns the hexadecimal representation of {@linkplain #hash()}.
   */
  public String hashHex() {
    return IntegralStrings.toHex(hash);
  }
  
  
  
  /**
   * A beacon at a reference URL.
   * 
   * @see #getRefUrl()
   */
  public static class Info extends ProtoBeacon {
    
    public Info(long utc) {
      super(utc);
    }
  }

}


/**
 * Base type. Created in Beacon.java in order to minimize number of source files
 * and cognitivie load.
 */
class ProtoBeacon {
  
  private final long utc;
  
  
  ProtoBeacon(long utc) {
    this.utc = utc;
    if (utc < HashUtc.INCEPTION_UTC)
      throw new IllegalArgumentException("utc: " + utc);
  }
  
  /**
   * Returns a reference URL for the beacon.
   * 
   * @return the reference URL for the beacon per the REST api
   */
  public String getRefUrl() {
    return
        "https://crums.io" + Constants.LIST_ROOTS_PATH + "?" +
        Constants.QS_UTC_NAME + "=" + utc +
        "&" + Constants.QS_COUNT_NAME + "=-1";
  }
  
  
  /**
   * Returns the advertised UTC.
   */
  public long utc() {
    return utc;
  }
  
}



